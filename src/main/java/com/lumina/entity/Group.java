package com.lumina.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("model_groups")
public class Group {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer balanceMode;
    private String matchRegex;
    private Integer firstTokenTimeout;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<GroupItem> groupItems;
}
