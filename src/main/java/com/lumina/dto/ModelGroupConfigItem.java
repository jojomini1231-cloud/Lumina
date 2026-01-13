package com.lumina.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModelGroupConfigItem {

    private String modelName;

    private Integer weight;

    private String apiKey;

    private String baseUrl;
}
