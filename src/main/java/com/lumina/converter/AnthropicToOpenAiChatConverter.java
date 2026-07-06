package com.lumina.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Anthropic Messages → OpenAI Chat Completions 协议转换器
 *
 * 请求方向: 客户端发 Anthropic 格式，转为 OpenAI Chat 格式发给上游
 * 响应方向: 上游返回 OpenAI Chat 格式，转为 Anthropic 格式返回给客户端
 */
@Slf4j
@Component
public class AnthropicToOpenAiChatConverter implements ProtocolConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ProtocolType sourceType() {
        return ProtocolType.ANTHROPIC;
    }

    @Override
    public ProtocolType targetType() {
        return ProtocolType.OPENAI_CHAT;
    }

    // ==================== 请求转换: Anthropic → OpenAI Chat ====================

    @Override
    public ObjectNode convertRequest(ObjectNode request) {
        ObjectNode result = mapper.createObjectNode();

        if (request.has("model")) result.set("model", request.get("model"));

        ArrayNode messages = mapper.createArrayNode();

        // system → system message
        if (request.has("system")) {
            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            JsonNode system = request.get("system");
            if (system.isTextual()) {
                systemMsg.put("content", system.asText());
            } else if (system.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode block : system) {
                    if (block.has("text")) sb.append(block.get("text").asText());
                }
                systemMsg.put("content", sb.toString());
            }
            messages.add(systemMsg);
        }

        // 转换 messages
        if (request.has("messages")) {
            for (JsonNode msg : request.get("messages")) {
                String role = msg.has("role") ? msg.get("role").asText() : "user";
                if ("assistant".equals(role)) {
                    convertAnthropicAssistantMessage(msg, messages);
                } else if ("user".equals(role)) {
                    convertAnthropicUserMessage(msg, messages);
                } else {
                    // 其他角色直接透传
                    ObjectNode openAiMsg = mapper.createObjectNode();
                    openAiMsg.put("role", role);
                    openAiMsg.put("content", getContentText(msg));
                    messages.add(openAiMsg);
                }
            }
        }
        result.set("messages", messages);

        if (request.has("max_tokens")) result.set("max_tokens", request.get("max_tokens"));
        if (request.has("temperature")) result.set("temperature", request.get("temperature"));
        if (request.has("top_p")) result.set("top_p", request.get("top_p"));
        if (request.has("stream")) result.set("stream", request.get("stream"));
        if (request.has("stop_sequences")) result.set("stop", request.get("stop_sequences"));

        // tools 转换: Anthropic → OpenAI
        if (request.has("tools") && request.get("tools").isArray()) {
            ArrayNode openAiTools = mapper.createArrayNode();
            for (JsonNode tool : request.get("tools")) {
                ObjectNode openAiTool = mapper.createObjectNode();
                openAiTool.put("type", "function");
                ObjectNode func = mapper.createObjectNode();
                func.put("name", tool.has("name") ? tool.get("name").asText() : "");
                if (tool.has("description")) {
                    func.put("description", tool.get("description").asText());
                }
                if (tool.has("input_schema")) {
                    func.set("parameters", tool.get("input_schema"));
                }
                openAiTool.set("function", func);
                openAiTools.add(openAiTool);
            }
            if (openAiTools.size() > 0) {
                result.set("tools", openAiTools);
            }
        }

        // tool_choice 转换
        if (request.has("tool_choice")) {
            JsonNode tc = request.get("tool_choice");
            if (tc.isObject() && tc.has("type")) {
                String tcType = tc.get("type").asText();
                switch (tcType) {
                    case "auto" -> result.put("tool_choice", "auto");
                    case "any" -> result.put("tool_choice", "auto"); // "required" 部分供应商不支持，降级为 auto
                    case "tool" -> {
                        ObjectNode openAiChoice = mapper.createObjectNode();
                        openAiChoice.put("type", "function");
                        ObjectNode funcChoice = mapper.createObjectNode();
                        funcChoice.put("name", tc.has("name") ? tc.get("name").asText() : "");
                        openAiChoice.set("function", funcChoice);
                        result.set("tool_choice", openAiChoice);
                    }
                }
            }
        }

        log.debug("Anthropic→Chat converted request: {}", result);
        return result;
    }

    /**
     * 转换 Anthropic assistant 消息，处理 tool_use content blocks
     */
    private void convertAnthropicAssistantMessage(JsonNode msg, ArrayNode messages) {
        ObjectNode openAiMsg = mapper.createObjectNode();
        openAiMsg.put("role", "assistant");

        JsonNode content = msg.get("content");
        if (content == null || content.isNull()) {
            openAiMsg.putNull("content");
            messages.add(openAiMsg);
            return;
        }

        if (content.isTextual()) {
            openAiMsg.put("content", content.asText());
            messages.add(openAiMsg);
            return;
        }

        // content 是数组，可能包含 text 和 tool_use blocks
        if (content.isArray()) {
            StringBuilder textContent = new StringBuilder();
            ArrayNode toolCalls = mapper.createArrayNode();
            int toolCallIndex = 0;

            for (JsonNode block : content) {
                String blockType = block.has("type") ? block.get("type").asText() : "";
                if ("text".equals(blockType) && block.has("text")) {
                    textContent.append(block.get("text").asText());
                } else if ("tool_use".equals(blockType)) {
                    ObjectNode tc = mapper.createObjectNode();
                    tc.put("id", block.has("id") ? block.get("id").asText() : UUID.randomUUID().toString());
                    tc.put("type", "function");
                    ObjectNode func = mapper.createObjectNode();
                    func.put("name", block.has("name") ? block.get("name").asText() : "");
                    if (block.has("input")) {
                        func.put("arguments", block.get("input").toString());
                    } else {
                        func.put("arguments", "{}");
                    }
                    tc.set("function", func);
                    tc.put("index", toolCallIndex++);
                    toolCalls.add(tc);
                }
            }

            if (textContent.length() > 0) {
                openAiMsg.put("content", textContent.toString());
            } else {
                openAiMsg.putNull("content");
            }

            if (toolCalls.size() > 0) {
                openAiMsg.set("tool_calls", toolCalls);
            }
        }

        messages.add(openAiMsg);
    }

    /**
     * 转换 Anthropic user 消息，处理 tool_result content blocks
     */
    private void convertAnthropicUserMessage(JsonNode msg, ArrayNode messages) {
        JsonNode content = msg.get("content");
        if (content == null || content.isNull()) {
            ObjectNode openAiMsg = mapper.createObjectNode();
            openAiMsg.put("role", "user");
            openAiMsg.put("content", "");
            messages.add(openAiMsg);
            return;
        }

        if (content.isTextual()) {
            ObjectNode openAiMsg = mapper.createObjectNode();
            openAiMsg.put("role", "user");
            openAiMsg.put("content", content.asText());
            messages.add(openAiMsg);
            return;
        }

        if (content.isArray()) {
            // 分离 tool_result 和普通内容
            List<JsonNode> toolResults = new ArrayList<>();
            ArrayNode userContent = mapper.createArrayNode();

            for (JsonNode block : content) {
                String blockType = block.has("type") ? block.get("type").asText() : "";
                if ("tool_result".equals(blockType)) {
                    toolResults.add(block);
                } else if ("text".equals(blockType) && block.has("text")) {
                    addOpenAiTextPart(userContent, block.get("text").asText());
                } else if ("image".equals(blockType)) {
                    ObjectNode imagePart = convertAnthropicImageBlock(block);
                    if (imagePart != null) userContent.add(imagePart);
                }
            }

            // tool_result → tool role messages
            for (JsonNode tr : toolResults) {
                ObjectNode toolMsg = mapper.createObjectNode();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", tr.has("tool_use_id") ? tr.get("tool_use_id").asText() : "");
                // tool_result 的 content 可以是字符串或数组
                if (tr.has("content")) {
                    JsonNode trContent = tr.get("content");
                    if (trContent.isTextual()) {
                        toolMsg.put("content", trContent.asText());
                    } else if (trContent.isArray()) {
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode part : trContent) {
                            if (part.has("text")) sb.append(part.get("text").asText());
                        }
                        toolMsg.put("content", sb.toString());
                    } else {
                        toolMsg.put("content", trContent.toString());
                    }
                } else {
                    toolMsg.put("content", "");
                }
                messages.add(toolMsg);
            }

            // 普通文本内容 → user message
            if (userContent.size() > 0) {
                ObjectNode userMsg = mapper.createObjectNode();
                userMsg.put("role", "user");
                if (hasOnlyTextParts(userContent)) {
                    userMsg.put("content", concatenateTextParts(userContent));
                } else {
                    userMsg.set("content", userContent);
                }
                messages.add(userMsg);
            }
        }
    }

    // ==================== 响应转换: OpenAI Chat → Anthropic ====================

    @Override
    public ObjectNode convertResponse(ObjectNode response) {
        return convertOpenAiChatResponseToAnthropic(response);
    }

    @Override
    public Flux<ServerSentEvent<String>> convertStreamResponse(Flux<ServerSentEvent<String>> upstream) {
        return convertOpenAiChatStreamToAnthropic(upstream);
    }

    private ObjectNode convertOpenAiChatResponseToAnthropic(ObjectNode response) {
        ObjectNode result = mapper.createObjectNode();
        String id = response.has("id") ? response.get("id").asText() : "msg_" + UUID.randomUUID();
        result.put("id", id);
        result.put("type", "message");
        result.put("role", "assistant");
        if (response.has("model")) result.set("model", response.get("model"));

        ArrayNode content = mapper.createArrayNode();
        String finishReason = "end_turn";

        if (response.has("choices") && response.get("choices").isArray() && response.get("choices").size() > 0) {
            JsonNode firstChoice = response.get("choices").get(0);
            if (firstChoice.has("finish_reason") && !firstChoice.get("finish_reason").isNull()) {
                finishReason = mapFinishReason(firstChoice.get("finish_reason").asText());
            }

            if (firstChoice.has("message")) {
                JsonNode message = firstChoice.get("message");

                // 文本内容
                if (message.has("content") && !message.get("content").isNull()) {
                    ObjectNode textBlock = mapper.createObjectNode();
                    textBlock.put("type", "text");
                    textBlock.put("text", message.get("content").asText());
                    content.add(textBlock);
                }

                // tool_calls → tool_use blocks
                if (message.has("tool_calls") && message.get("tool_calls").isArray()) {
                    for (JsonNode tc : message.get("tool_calls")) {
                        ObjectNode toolUse = mapper.createObjectNode();
                        toolUse.put("type", "tool_use");
                        toolUse.put("id", tc.has("id") ? tc.get("id").asText() : UUID.randomUUID().toString());
                        if (tc.has("function")) {
                            JsonNode func = tc.get("function");
                            toolUse.put("name", func.has("name") ? func.get("name").asText() : "");
                            if (func.has("arguments")) {
                                try {
                                    JsonNode argsObj = mapper.readTree(func.get("arguments").asText());
                                    toolUse.set("input", argsObj);
                                } catch (Exception e) {
                                    toolUse.set("input", mapper.createObjectNode());
                                }
                            } else {
                                toolUse.set("input", mapper.createObjectNode());
                            }
                        }
                        content.add(toolUse);
                    }
                }
            }
        }
        result.set("content", content);
        result.put("stop_reason", finishReason);

        if (response.has("usage")) {
            JsonNode usage = response.get("usage");
            ObjectNode anthropicUsage = mapper.createObjectNode();
            anthropicUsage.put("input_tokens", usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0);
            anthropicUsage.put("output_tokens", usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0);
            result.set("usage", anthropicUsage);
        }

        return result;
    }

    // ==================== 流式转换: OpenAI Chat Stream → Anthropic Stream ====================

    private Flux<ServerSentEvent<String>> convertOpenAiChatStreamToAnthropic(Flux<ServerSentEvent<String>> upstream) {
        return Flux.defer(() -> {
            OpenAiChatToAnthropicStreamState state = new OpenAiChatToAnthropicStreamState();
            return upstream
                    .flatMapIterable(state::handle)
                    .concatWith(Flux.defer(() -> Flux.fromIterable(state.finish())));
        });
    }

    private static final class OpenAiChatToAnthropicStreamState {
        private final String fallbackMessageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        private final Map<Integer, ToolCallState> tools = new TreeMap<>();
        private boolean messageStarted;
        private boolean finished;
        private boolean seenUpstreamData;
        private int nextBlockIndex;
        private Integer openTextBlockIndex;
        private Integer openThinkingBlockIndex;
        private String messageId;
        private String model;
        private String stopReason;
        private ObjectNode latestUsage;

        List<ServerSentEvent<String>> handle(ServerSentEvent<String> event) {
            if (finished) {
                return Collections.emptyList();
            }
            String data = event.data();
            if (data == null || data.isBlank()) {
                return Collections.emptyList();
            }
            seenUpstreamData = true;
            if ("[DONE]".equals(data)) {
                return finish();
            }

            try {
                JsonNode node = mapper.readTree(data);
                List<ServerSentEvent<String>> events = new ArrayList<>();
                captureEnvelope(node);
                events.addAll(ensureMessageStart());
                captureUsage(node);

                JsonNode choices = node.get("choices");
                if (choices != null && choices.isArray()) {
                    for (JsonNode choice : choices) {
                        JsonNode delta = choice.get("delta");
                        if (delta != null && delta.isObject()) {
                            appendReasoningDelta(events, delta);
                            appendTextDelta(events, delta);
                            appendToolCallDeltas(events, delta);
                        }
                        if (captureFinishReason(choice)) {
                            closeNonToolBlocks(events);
                            startDanglingTools(events);
                            closeOpenTools(events);
                        }
                    }
                }
                return events;
            } catch (Exception e) {
                log.debug("Failed to parse OpenAI stream event: {}", data);
                return Collections.emptyList();
            }
        }

        List<ServerSentEvent<String>> finish() {
            if (finished) {
                return Collections.emptyList();
            }
            if (!seenUpstreamData && !messageStarted) {
                finished = true;
                return Collections.emptyList();
            }
            finished = true;

            List<ServerSentEvent<String>> events = new ArrayList<>();
            events.addAll(ensureMessageStart());
            closeNonToolBlocks(events);
            startDanglingTools(events);
            closeOpenTools(events);

            ObjectNode messageDelta = mapper.createObjectNode();
            messageDelta.put("type", "message_delta");
            ObjectNode delta = mapper.createObjectNode();
            delta.put("stop_reason", stopReason != null ? stopReason : (hasStartedTool() ? "tool_use" : "end_turn"));
            delta.putNull("stop_sequence");
            messageDelta.set("delta", delta);
            messageDelta.set("usage", latestUsage != null ? latestUsage : emptyAnthropicUsage());
            events.add(sse("message_delta", messageDelta.toString()));

            ObjectNode messageStop = mapper.createObjectNode();
            messageStop.put("type", "message_stop");
            events.add(sse("message_stop", messageStop.toString()));
            return events;
        }

        private void captureEnvelope(JsonNode node) {
            if (messageId == null && node.hasNonNull("id")) {
                messageId = node.get("id").asText();
            }
            if (model == null && node.hasNonNull("model")) {
                model = node.get("model").asText();
            }
        }

        private List<ServerSentEvent<String>> ensureMessageStart() {
            if (messageStarted) {
                return Collections.emptyList();
            }
            messageStarted = true;

            ObjectNode messageStart = mapper.createObjectNode();
            messageStart.put("type", "message_start");
            ObjectNode message = mapper.createObjectNode();
            message.put("id", messageId != null && !messageId.isBlank() ? messageId : fallbackMessageId);
            message.put("type", "message");
            message.put("role", "assistant");
            message.put("model", model != null ? model : "");
            message.set("content", mapper.createArrayNode());
            message.putNull("stop_reason");
            message.set("usage", emptyAnthropicUsage());
            messageStart.set("message", message);
            return List.of(sse("message_start", messageStart.toString()));
        }

        private void captureUsage(JsonNode node) {
            JsonNode usage = node.get("usage");
            if (usage == null || usage.isNull() || !usage.isObject()) {
                return;
            }
            ObjectNode anthropicUsage = emptyAnthropicUsage();
            int cacheRead = usage.path("cache_read_input_tokens").asInt(0);
            int cacheCreation = usage.path("cache_creation_input_tokens").asInt(0);
            JsonNode promptDetails = usage.get("prompt_tokens_details");
            if (cacheRead == 0 && promptDetails != null && promptDetails.has("cached_tokens")) {
                cacheRead = promptDetails.path("cached_tokens").asInt(0);
            }
            int inputTokens = usage.path("input_tokens").asInt(
                    Math.max(0, usage.path("prompt_tokens").asInt(0) - cacheRead - cacheCreation));
            int outputTokens = usage.path("output_tokens").asInt(usage.path("completion_tokens").asInt(0));
            anthropicUsage.put("input_tokens", inputTokens);
            anthropicUsage.put("output_tokens", outputTokens);
            if (cacheRead > 0) {
                anthropicUsage.put("cache_read_input_tokens", cacheRead);
            }
            if (cacheCreation > 0) {
                anthropicUsage.put("cache_creation_input_tokens", cacheCreation);
            }
            latestUsage = anthropicUsage;
        }

        private boolean captureFinishReason(JsonNode choice) {
            JsonNode finishReason = choice.get("finish_reason");
            if (finishReason == null || finishReason.isNull()) {
                return false;
            }
            stopReason = mapFinishReasonValue(finishReason.asText());
            return true;
        }

        private void appendReasoningDelta(List<ServerSentEvent<String>> events, JsonNode delta) {
            String reasoning = textFromFirst(delta, "reasoning_content", "reasoning");
            if (reasoning == null || reasoning.isEmpty()) {
                return;
            }
            closeTextBlock(events);
            if (openThinkingBlockIndex == null) {
                int index = nextBlockIndex++;
                openThinkingBlockIndex = index;
                ObjectNode blockStart = mapper.createObjectNode();
                blockStart.put("type", "content_block_start");
                blockStart.put("index", index);
                ObjectNode contentBlock = mapper.createObjectNode();
                contentBlock.put("type", "thinking");
                contentBlock.put("thinking", "");
                blockStart.set("content_block", contentBlock);
                events.add(sse("content_block_start", blockStart.toString()));
            }

            ObjectNode deltaEvent = mapper.createObjectNode();
            deltaEvent.put("type", "content_block_delta");
            deltaEvent.put("index", openThinkingBlockIndex);
            ObjectNode deltaObj = mapper.createObjectNode();
            deltaObj.put("type", "thinking_delta");
            deltaObj.put("thinking", reasoning);
            deltaEvent.set("delta", deltaObj);
            events.add(sse("content_block_delta", deltaEvent.toString()));
        }

        private void appendTextDelta(List<ServerSentEvent<String>> events, JsonNode delta) {
            JsonNode content = delta.get("content");
            if (content == null || content.isNull()) {
                return;
            }
            String text = content.asText();
            if (text.isEmpty()) {
                return;
            }
            closeThinkingBlock(events);
            if (openTextBlockIndex == null) {
                int index = nextBlockIndex++;
                openTextBlockIndex = index;
                ObjectNode blockStart = mapper.createObjectNode();
                blockStart.put("type", "content_block_start");
                blockStart.put("index", index);
                ObjectNode contentBlock = mapper.createObjectNode();
                contentBlock.put("type", "text");
                contentBlock.put("text", "");
                blockStart.set("content_block", contentBlock);
                events.add(sse("content_block_start", blockStart.toString()));
            }

            ObjectNode deltaEvent = mapper.createObjectNode();
            deltaEvent.put("type", "content_block_delta");
            deltaEvent.put("index", openTextBlockIndex);
            ObjectNode deltaObj = mapper.createObjectNode();
            deltaObj.put("type", "text_delta");
            deltaObj.put("text", text);
            deltaEvent.set("delta", deltaObj);
            events.add(sse("content_block_delta", deltaEvent.toString()));
        }

        private void appendToolCallDeltas(List<ServerSentEvent<String>> events, JsonNode delta) {
            JsonNode toolCalls = delta.get("tool_calls");
            if (toolCalls == null || !toolCalls.isArray() || toolCalls.isEmpty()) {
                return;
            }
            closeNonToolBlocks(events);

            for (JsonNode toolCall : toolCalls) {
                int toolIndex = toolCall.path("index").asInt(0);
                ToolCallState tool = tools.computeIfAbsent(toolIndex, ignored -> new ToolCallState(nextBlockIndex++));
                if (toolCall.hasNonNull("id")) {
                    tool.id = toolCall.get("id").asText();
                }
                JsonNode function = toolCall.get("function");
                String argumentsDelta = null;
                if (function != null && function.isObject()) {
                    if (function.hasNonNull("name")) {
                        tool.name = function.get("name").asText();
                    }
                    if (function.has("arguments") && !function.get("arguments").isNull()) {
                        argumentsDelta = function.get("arguments").asText();
                    }
                }

                if (!tool.started && tool.canStart()) {
                    startTool(events, tool);
                }
                if (argumentsDelta != null && !argumentsDelta.isEmpty()) {
                    if (tool.started) {
                        emitToolArgumentsDelta(events, tool.blockIndex, argumentsDelta);
                    } else {
                        tool.pendingArguments.append(argumentsDelta);
                    }
                }
            }
        }

        private void startDanglingTools(List<ServerSentEvent<String>> events) {
            tools.forEach((toolIndex, tool) -> {
                if (tool.started || !tool.hasPayload()) {
                    return;
                }
                if (tool.id == null || tool.id.isBlank()) {
                    tool.id = "tool_call_" + toolIndex;
                }
                if (tool.name == null || tool.name.isBlank()) {
                    tool.name = "unknown_tool";
                }
                startTool(events, tool);
            });
        }

        private void startTool(List<ServerSentEvent<String>> events, ToolCallState tool) {
            tool.started = true;
            ObjectNode blockStart = mapper.createObjectNode();
            blockStart.put("type", "content_block_start");
            blockStart.put("index", tool.blockIndex);
            ObjectNode contentBlock = mapper.createObjectNode();
            contentBlock.put("type", "tool_use");
            contentBlock.put("id", tool.id);
            contentBlock.put("name", tool.name);
            contentBlock.set("input", mapper.createObjectNode());
            blockStart.set("content_block", contentBlock);
            events.add(sse("content_block_start", blockStart.toString()));
            if (!tool.pendingArguments.isEmpty()) {
                emitToolArgumentsDelta(events, tool.blockIndex, tool.pendingArguments.toString());
                tool.pendingArguments.setLength(0);
            }
        }

        private void emitToolArgumentsDelta(List<ServerSentEvent<String>> events, int blockIndex, String argumentsDelta) {
            ObjectNode deltaEvent = mapper.createObjectNode();
            deltaEvent.put("type", "content_block_delta");
            deltaEvent.put("index", blockIndex);
            ObjectNode deltaObj = mapper.createObjectNode();
            deltaObj.put("type", "input_json_delta");
            deltaObj.put("partial_json", argumentsDelta);
            deltaEvent.set("delta", deltaObj);
            events.add(sse("content_block_delta", deltaEvent.toString()));
        }

        private void closeNonToolBlocks(List<ServerSentEvent<String>> events) {
            closeThinkingBlock(events);
            closeTextBlock(events);
        }

        private void closeTextBlock(List<ServerSentEvent<String>> events) {
            if (openTextBlockIndex == null) {
                return;
            }
            events.add(contentBlockStop(openTextBlockIndex));
            openTextBlockIndex = null;
        }

        private void closeThinkingBlock(List<ServerSentEvent<String>> events) {
            if (openThinkingBlockIndex == null) {
                return;
            }
            events.add(contentBlockStop(openThinkingBlockIndex));
            openThinkingBlockIndex = null;
        }

        private void closeOpenTools(List<ServerSentEvent<String>> events) {
            tools.values().stream()
                    .filter(tool -> tool.started && !tool.closed)
                    .sorted(Comparator.comparingInt(tool -> tool.blockIndex))
                    .forEach(tool -> {
                        events.add(contentBlockStop(tool.blockIndex));
                        tool.closed = true;
                    });
        }

        private boolean hasStartedTool() {
            return tools.values().stream().anyMatch(tool -> tool.started);
        }

        private static String textFromFirst(JsonNode node, String... fields) {
            for (String field : fields) {
                JsonNode value = node.get(field);
                if (value != null && !value.isNull()) {
                    return value.asText();
                }
            }
            return null;
        }

        private static ServerSentEvent<String> contentBlockStop(int index) {
            ObjectNode blockStop = mapper.createObjectNode();
            blockStop.put("type", "content_block_stop");
            blockStop.put("index", index);
            return sse("content_block_stop", blockStop.toString());
        }

        private static ObjectNode emptyAnthropicUsage() {
            ObjectNode usage = mapper.createObjectNode();
            usage.put("input_tokens", 0);
            usage.put("output_tokens", 0);
            return usage;
        }
    }

    private static final class ToolCallState {
        private final int blockIndex;
        private final StringBuilder pendingArguments = new StringBuilder();
        private String id;
        private String name;
        private boolean started;
        private boolean closed;

        private ToolCallState(int blockIndex) {
            this.blockIndex = blockIndex;
        }

        private boolean canStart() {
            return id != null && !id.isBlank() && name != null && !name.isBlank();
        }

        private boolean hasPayload() {
            return canStart() || !pendingArguments.isEmpty();
        }
    }

    private static ServerSentEvent<String> sse(String eventType, String data) {
        return ServerSentEvent.<String>builder().event(eventType).data(data).build();
    }

    private String getContentText(JsonNode msg) {
        if (!msg.has("content")) return "";
        JsonNode content = msg.get("content");
        if (content.isTextual()) return content.asText();
        if (content.isNull()) return "";
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : content) {
                if (part.has("text")) sb.append(part.get("text").asText());
                else if (part.has("input_text")) sb.append(part.get("input_text").asText());
                else if (part.has("output_text")) sb.append(part.get("output_text").asText());
            }
            return sb.toString();
        }
        return content.asText();
    }

    private void addOpenAiTextPart(ArrayNode content, String text) {
        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", text);
        content.add(textPart);
    }

    private boolean hasOnlyTextParts(ArrayNode content) {
        if (content.size() == 0) return false;
        for (JsonNode part : content) {
            if (!"text".equals(part.path("type").asText())) return false;
        }
        return true;
    }

    private String concatenateTextParts(ArrayNode content) {
        StringBuilder text = new StringBuilder();
        for (JsonNode part : content) {
            text.append(part.path("text").asText());
        }
        return text.toString();
    }

    private ObjectNode convertAnthropicImageBlock(JsonNode block) {
        JsonNode source = block.get("source");
        if (source == null || source.isNull()) return null;

        String url = "";
        String sourceType = source.path("type").asText();
        if ("base64".equals(sourceType)) {
            String mediaType = source.path("media_type").asText("image/png");
            String data = source.path("data").asText("");
            if (data.isBlank()) return null;
            url = "data:" + mediaType + ";base64," + data;
        } else if ("url".equals(sourceType)) {
            url = source.path("url").asText("");
        }
        if (url.isBlank()) return null;

        ObjectNode imagePart = mapper.createObjectNode();
        imagePart.put("type", "image_url");
        ObjectNode imageUrl = mapper.createObjectNode();
        imageUrl.put("url", url);
        imagePart.set("image_url", imageUrl);
        return imagePart;
    }

    private String mapFinishReason(String openAiReason) {
        return mapFinishReasonValue(openAiReason);
    }

    private static String mapFinishReasonValue(String openAiReason) {
        if (openAiReason == null) return "end_turn";
        return switch (openAiReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "content_filter" -> "end_turn";
            case "tool_calls" -> "tool_use";
            default -> "end_turn";
        };
    }
}
