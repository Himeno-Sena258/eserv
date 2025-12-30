package com.eServM.eserv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record OrderRequest(
        @NotBlank(message = "订单简介不能为空") String summary,
        @NotBlank(message = "商品名称不能为空") String productName,
        @NotBlank(message = "客户UID不能为空") String customerUid,
        OffsetDateTime orderTime) {
}
