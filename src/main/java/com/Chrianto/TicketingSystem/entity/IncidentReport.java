//package com.Chrianto.TicketingSystem.entity;
//
//import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
//import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//
//@Entity
//@Table(name = "IncidentReport")
//@Getter @Setter @NoArgsConstructor
//public class IncidentReport {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne @JoinColumn(name = "creator_id")
//    private User creator;
//
//    @Column(length = 2000)
//    private String description;
//
//    @Enumerated(EnumType.STRING)
//    private IncidentStatus status;
//
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//
//
//}
