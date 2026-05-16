package com.lumina.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("llm_models")
public class LlmModel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelName;
    private String provider;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Integer contextLimit;
    private Integer outputLimit;
    private BigDecimal cacheReadPrice;
    private BigDecimal cacheWritePrice;
    private Boolean isReasoning;
    private Boolean isToolCall;
    private Boolean isAttachment;
    private Boolean isStructuredOutput;
    private Boolean isTemperature;
    private Boolean isOpenWeights;
    private String inputType;
    private String outputType;
    private String displayName;
    private String family;
    private String knowledgeCutoff;
    private String releaseDate;
    private Integer inputLimit;
    private Boolean isActive;
    private String lastUpdatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
