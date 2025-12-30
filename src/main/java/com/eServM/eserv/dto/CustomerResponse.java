package com.eServM.eserv.dto;

import java.time.OffsetDateTime;

public record CustomerResponse(
        String uid,
        String name,
        String contactMethod,
        OffsetDateTime createdAt) {
}
