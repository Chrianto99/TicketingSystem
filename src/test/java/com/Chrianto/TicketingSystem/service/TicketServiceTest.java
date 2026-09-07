package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Covers the multi-candidate offer/claim flow specifically — it's the one part
// of the feature with no controller-level test coverage (claiming needs a real
// authenticated session to exercise end-to-end), and the atomic-claim path has
// no margin for the native UPDATE/DELETE ordering to be wrong.
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubcategoryRepository subcategoryRepository;
    @Mock private TicketHistoryService ticketHistoryService;
    @Mock private AttachmentService attachmentService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private TicketService ticketService;

    private User performer;

    @BeforeEach
    void setUp() {
        performer = new User();
        performer.setId(1L);
        performer.setUsername("alice");
    }

    private User activeUser(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setActive(true);
        return u;
    }

    private Ticket openTicket() {
        Ticket t = new Ticket();
        t.setId(5L);
        t.setStatus(TicketStatus.OPEN);
        t.setSummary("Κάτι χάλασε");
        return t;
    }

    @Test
    void claimTicket_whenAtomicUpdateWins_clearsCandidatesAndLogsClaim() {
        Ticket ticket = openTicket();
        ticket.setAssignedUser(performer);
        when(ticketRepository.claimIfCandidate(5L, 2L)).thenReturn(1);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        User claimant = activeUser(2L, "bob");
        TicketResponse response = ticketService.claimTicket(5L, claimant);

        assertThat(response.getId()).isEqualTo(5L);
        InOrder order = inOrder(ticketRepository);
        order.verify(ticketRepository).claimIfCandidate(5L, 2L);
        order.verify(ticketRepository).clearCandidates(5L);
        order.verify(ticketRepository).findById(5L);
        verify(ticketHistoryService).logHistory(eq(ticket), eq(claimant), eq(TicketAction.CLAIMED), isNull(), isNull(), isNull());
    }

    @Test
    void claimTicket_whenAtomicUpdateLoses_throwsAndNeverTouchesCandidatesOrHistory() {
        when(ticketRepository.claimIfCandidate(5L, 2L)).thenReturn(0);

        User claimant = activeUser(2L, "bob");
        assertThatThrownBy(() -> ticketService.claimTicket(5L, claimant))
                .isInstanceOf(IllegalStateException.class);

        verify(ticketRepository, never()).clearCandidates(anyLong());
        verify(ticketRepository, never()).findById(anyLong());
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void offerTicketToCandidates_rejectsDeactivatedCandidate() {
        Ticket ticket = openTicket();
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        User activeCandidate = activeUser(2L, "bob");
        User deactivatedCandidate = activeUser(3L, "carol");
        deactivatedCandidate.setActive(false);
        when(userRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(activeCandidate, deactivatedCandidate));

        assertThatThrownBy(() -> ticketService.offerTicketToCandidates(5L, List.of(2L, 3L), performer))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void offerTicketToCandidates_happyPath_clearsAssigneeAndNotifiesEveryoneButPerformer() {
        Ticket ticket = openTicket();
        ticket.setAssignedUser(activeUser(9L, "previousAssignee"));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        User bob = activeUser(2L, "bob");
        User alice = performer; // performer offers to themselves too, among others
        when(userRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(bob, alice));

        TicketResponse response = ticketService.offerTicketToCandidates(5L, List.of(2L, 1L), performer);

        assertThat(ticket.getAssignedUser()).isNull();
        assertThat(response.getAssignedUserId()).isNull();
        assertThat(response.getCandidateUserIds()).containsExactlyInAnyOrder(2L, 1L);
        verify(notificationService).notifyTicketAssigned(2L);
        verify(notificationService, never()).notifyTicketAssigned(1L);
        verify(ticketHistoryService).logHistory(eq(ticket), eq(performer), eq(TicketAction.OFFERED), isNull(), isNull(), any(String.class));
    }
}
