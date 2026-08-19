package com.Chrianto.TicketingSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attachment")
@Getter @Setter @NoArgsConstructor
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident; // nullable, new

    @ManyToOne
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath; // or URL, depending on storage strategy (local/S3/etc.)

    private String contentType;

    private Long fileSize; // bytes

    private LocalDateTime uploadedAt;

    @PrePersist
    @PreUpdate
    private void validateParent() {
        boolean hasTicket = ticket != null;
        boolean hasIncident = incident != null;

        if (hasTicket == hasIncident) {
            // both null, or both set — either way, invalid
            throw new IllegalStateException(
                    "Attachment must belong to exactly one of: Ticket, Incident"
            );
        }
    }
}