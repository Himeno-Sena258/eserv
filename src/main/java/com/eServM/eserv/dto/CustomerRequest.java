package com.eServM.eserv.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank(message = "客户名称不能为空") String name,
        String contactMethod) {
}
