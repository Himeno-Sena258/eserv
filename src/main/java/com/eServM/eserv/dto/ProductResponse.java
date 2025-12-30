package com.eServM.eserv.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductResponse(
        String uid,
        String name,
        String description,
        BigDecimal unitPrice,
        boolean active,
        OffsetDateTime createdAt) {
}
