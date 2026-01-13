package com.lumina.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RelayService {
    /**
     * 中转 Anthropic 对话格式
     * @param type
     * @param params
     * @return
     */
    Flux<String> relay(String type, ObjectNode params,Boolean beta);
}
