package com.eServM.eserv.dto;

import java.time.OffsetDateTime;

public record OrderNoteResponse(
        String uid,
        String orderUid,
        String orderSummary,
        String message,
        OffsetDateTime createdAt) {
}
