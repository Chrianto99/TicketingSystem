package com.Chrianto.TicketingSystem.entity;

import com.Chrianto.TicketingSystem.entity.enums.IncidentAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_history")
@Getter @Setter @NoArgsConstructor
public class TicketHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String description;

    @ManyToOne @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne @JoinColumn(name = "incident_id")
    private Incident incident;

    @NonNull @ManyToOne @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_action")
    private TicketAction ticketAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_action")
    private IncidentAction incidentAction;

    private LocalDateTime timestamp;

    @PrePersist
    @PreUpdate
    private void validateParent() {
        boolean hasTicket = ticket != null;
        boolean hasIncident = incident != null;

        if (hasTicket == hasIncident) {
            // both null, or both set — either way, invalid
            throw new IllegalStateException(
                    "Ticket History must belong to exactly one of: Ticket, Incident"
            );
        }
    }
}
