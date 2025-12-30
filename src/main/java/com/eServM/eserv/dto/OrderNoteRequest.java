package com.eServM.eserv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderNoteRequest(
        @NotBlank(message = "订单UID不能为空") String orderUid,
        @NotBlank(message = "备注内容不能为空") @Size(max = 1024, message = "备注长度不应超过1024字符") String message) {
}
