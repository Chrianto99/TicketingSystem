package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// In-memory SSE registry, single-instance app — no need for a message broker.
// The DB flag is the source of truth (survives reloads/reconnects); the emitters
// just push a live nudge to whoever happens to have /tickets open right now.
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> list = emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());

        // Every page load opens a fresh EventSource (this is a multi-page app,
        // not an SPA — there's no single persistent connection to reuse across
        // navigations), and the browser's "abort the old one on unload" isn't
        // instant. Rapid tab/filter switching was piling up emitters faster
        // than they were being torn down, eventually exhausting the server's
        // connection capacity. A user only ever needs one live stream, so
        // evict whatever was there before adding the new one — caps this at
        // one per user no matter how fast someone clicks.
        list.forEach(SseEmitter::complete);
        list.clear();
        list.add(emitter);

        emitter.onCompletion(() -> unregister(userId, emitter));
        emitter.onTimeout(() -> unregister(userId, emitter));
        emitter.onError(ex -> unregister(userId, emitter));

        // Without writing anything, the response (headers included) can sit
        // buffered server-side indefinitely — the browser's EventSource never
        // reports the connection as open, and there's no client-visible signal
        // that a subscribe actually succeeded versus silently failing. A
        // comment line is invisible to EventSource's event parsing but forces
        // the flush.
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            unregister(userId, emitter);
        }
        return emitter;
    }

    private void unregister(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    @Transactional
    public void notifyTicketAssigned(Long assigneeId) {
        userRepository.markUnseenAssignedTickets(assigneeId);
        push(assigneeId);
    }

    private void push(Long userId) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("ticket-assigned").data("assigned"));
            } catch (IOException | IllegalStateException e) {
                // IllegalStateException: the emitter completed (e.g. evicted by a
                // newer subscribe() for the same user) between the list snapshot
                // above and this send() call.
                unregister(userId, emitter);
            }
        }
    }

    @Transactional
    public void markSeen(Long userId) {
        userRepository.clearUnseenAssignedTickets(userId);
    }

    public boolean hasUnseenAssignedTickets(Long userId) {
        return userRepository.existsByIdAndUnseenAssignedTicketsTrue(userId);
    }
}
