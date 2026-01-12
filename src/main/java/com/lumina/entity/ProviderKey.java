package com.lumina.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("provider_keys")
public class ProviderKey {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long providerId;
    private Boolean isEnabled;
    private String apiKey;
    private Integer statusCode;
    private Long lastUsedAt;
    private BigDecimal totalCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
