package com.eServM.eserv.dto;

import java.time.OffsetDateTime;

public record OrderResponse(
        String uid,
        String summary,
        String productName,
        String customerUid,
        String customerName,
        OffsetDateTime orderTime) {
}
