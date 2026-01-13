package com.lumina.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModelGroupConfig {

    private String name;

    private Integer balanceMode;

    private List<ModelGroupConfigItem> items;
}
