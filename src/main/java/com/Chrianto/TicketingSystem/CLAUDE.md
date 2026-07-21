# TicketingSystem

Spring Boot 4.1.0 ticketing system. Java, Maven, PostgreSQL, Lombok.

## Conventions
- DTOs: XCreateRequest / XUpdateRequest / XResponse, in dto/request/ and dto/response/
- Repositories extend JpaRepository; use derived query methods where possible
- Services: one per entity domain (TicketService, UserService, DepartmentService)
- Controllers: thin, delegate to services, use ResponseEntity ,(Comments Controller and Ticket History reside in Tickets Controller )
- Never expose entities directly from controllers — always map to Response DTOs
- Auth: session-based (Spring Security form login, HttpSession), @PreAuthorize for role checks (USER / ADMIN)
- View layer: Thymeleaf, controllers in controller/view/ (return view names), REST controllers in controller/ (JSON, unchanged)
- Known version gotchas: springdoc-openapi 3.0.3 (not 2.x — Boot 4 incompatible),
  DaoAuthenticationProvider now requires constructor injection