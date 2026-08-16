package com.Chrianto.TicketingSystem.entity.enums;


public enum TicketSource {
    MANUAL,       // created directly via POST /api/tickets
    INCIDENT      // spawned from an IncidentReport
}

