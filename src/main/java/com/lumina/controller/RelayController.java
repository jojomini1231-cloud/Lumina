package com.lumina.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumina.service.RelayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
public class RelayController {

    @Autowired
    private RelayService relayService;

    /**
     * 中转 Anthropic 对话格式
     * @param params
     * @return
     */
    @PostMapping("/v1/messages")
    public Flux<String> createMessage(@RequestBody ObjectNode params,Boolean beta) {
        Flux<String> anthropicMessages = relayService.relay("anthropic_messages", params, beta);
        return anthropicMessages;
    }

}