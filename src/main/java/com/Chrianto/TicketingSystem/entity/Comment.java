package com.Chrianto.TicketingSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comment")
@Getter @Setter @NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket ticket; // nullable now

//    @ManyToOne
//    @JoinColumn(name = "incident_report_id")
//    private IncidentReport incidentReport; // nullable, new

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User user;

    @Column(length = 3000)
    private String text;

    private LocalDateTime timestamp;
//
//    @PrePersist
//    @PreUpdate
//    private void validateParent() {
//        boolean hasTicket = ticket != null;
//        boolean hasIncidentReport = incidentReport != null;
//
//        if (hasTicket == hasIncidentReport) {
//            // both null, or both set — either way, invalid
//            throw new IllegalStateException(
//                    "Comment must belong to exactly one of: Ticket, IncidentReport"
//            );
//        }
//    }
}
