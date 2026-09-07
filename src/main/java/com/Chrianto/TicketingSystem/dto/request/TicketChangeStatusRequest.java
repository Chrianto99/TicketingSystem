package com.Chrianto.TicketingSystem.dto.request;

import lombok.*;

// Shared by resolve/cancel/reopen/comment — whether commentText is required
// differs per action (mandatory for resolve/comment, optional for cancel/
// reopen), so that's enforced in TicketService rather than here.
@Getter @Setter
public class TicketChangeStatusRequest {

    private String commentText;

    private Long subcategoryId;

}
