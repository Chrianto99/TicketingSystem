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
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> unregister(userId, emitter));
        emitter.onTimeout(() -> unregister(userId, emitter));
        emitter.onError(ex -> unregister(userId, emitter));
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
            } catch (IOException e) {
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
