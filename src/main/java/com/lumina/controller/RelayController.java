package com.lumina.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumina.service.RelayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
    public Object createMessage(@RequestBody ObjectNode params, Boolean beta) {
        Object anthropicMessages = relayService.relay("anthropic_messages", params, beta);
        return anthropicMessages;
    }

    /**
     * 中转 OpenAI 格式（Chat Completions）
     * @param params
     * @return
     */
    @PostMapping(
            value = "/v1/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Object createChatCompletions(@RequestBody ObjectNode params,Boolean beta) {
        Object anthropicMessages = relayService.relay("openai_chat_completions", params, beta);
        return anthropicMessages;
    }

}