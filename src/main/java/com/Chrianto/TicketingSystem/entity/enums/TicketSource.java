package com.Chrianto.TicketingSystem.entity.enums;


public enum TicketSource {
    MANUAL("Χειροκίνητο"),        // created directly via POST /api/tickets
    INCIDENT("Από Συμβάν"),       // spawned from an Incident
    CALLBACK("Επιστροφή Κλήσης"); // lightweight "call this person back" ticket, no resolution — completing it deletes it

    private final String displayName;

    TicketSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
