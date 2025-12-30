package com.eServM.eserv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "商品名称不能为空") @Size(max = 128) String name,
        @Size(max = 2048, message = "描述不应超过2048字符") String description,
        @NotNull(message = "单价不能为空") @DecimalMin(value = "0.00", message = "单价不能为负数") @Digits(integer = 13, fraction = 2) BigDecimal unitPrice,
        Boolean active) {
}
