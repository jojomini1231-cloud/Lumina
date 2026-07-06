package com.lumina.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiChatToResponsesConverterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenAiChatToResponsesConverter converter = new OpenAiChatToResponsesConverter();

    @Test
    void convertRequestPreservesAssistantReasoningBeforeToolCalls() throws Exception {
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "model": "deepseek-v4-flash",
                  "messages": [
                    {
                      "role": "assistant",
                      "content": null,
                      "reasoning_content": "Need to inspect files.",
                      "tool_calls": [
                        {
                          "id": "call_1",
                          "type": "function",
                          "function": {
                            "name": "exec_command",
                            "arguments": "{\\"cmd\\":\\"pwd\\"}"
                          }
                        }
                      ]
                    }
                  ]
                }
                """);

        ObjectNode converted = converter.convertRequest(request);
        JsonNode input = converted.get("input");

        assertEquals("reasoning", input.get(0).get("type").asText());
        assertTrue(input.get(0).get("encrypted_content").asText().startsWith("deepseek-reasoning:"));
        assertEquals("function_call", input.get(1).get("type").asText());
        assertEquals("call_1", input.get(1).get("call_id").asText());
    }

    @Test
    void convertRequestPreservesAssistantReasoningBeforeTextMessage() throws Exception {
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "model": "deepseek-v4-flash",
                  "messages": [
                    {
                      "role": "assistant",
                      "content": "Let me inspect the workspace.",
                      "reasoning_content": "Need to inspect first."
                    }
                  ]
                }
                """);

        ObjectNode converted = converter.convertRequest(request);
        JsonNode input = converted.get("input");

        assertEquals("reasoning", input.get(0).get("type").asText());
        assertTrue(input.get(0).get("encrypted_content").asText().startsWith("deepseek-reasoning:"));
        assertEquals("message", input.get(1).get("type").asText());
        assertEquals("assistant", input.get(1).get("role").asText());
        assertEquals("Let me inspect the workspace.", input.get(1).get("content").get(0).get("text").asText());
    }

    @Test
    void convertResponseRestoresOwnEncodedReasoningForChatToolCalls() throws Exception {
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "model": "deepseek-v4-flash",
                  "messages": [
                    {
                      "role": "assistant",
                      "content": null,
                      "reasoning_content": "Need to inspect files.",
                      "tool_calls": [
                        {
                          "id": "call_1",
                          "type": "function",
                          "function": {
                            "name": "exec_command",
                            "arguments": "{\\"cmd\\":\\"pwd\\"}"
                          }
                        }
                      ]
                    }
                  ]
                }
                """);
        ObjectNode convertedRequest = converter.convertRequest(request);
        ArrayNode input = (ArrayNode) convertedRequest.get("input");

        ObjectNode response = mapper.createObjectNode();
        response.put("id", "resp_1");
        response.put("model", "deepseek-v4-flash");
        ArrayNode output = mapper.createArrayNode();
        output.add(input.get(0).deepCopy());
        output.add(input.get(1).deepCopy());
        response.set("output", output);

        ObjectNode convertedResponse = converter.convertResponse(response);
        JsonNode message = convertedResponse.get("choices").get(0).get("message");

        assertEquals("Need to inspect files.", message.get("reasoning_content").asText());
        assertEquals("call_1", message.get("tool_calls").get(0).get("id").asText());
    }

    @Test
    void convertResponseRestoresOwnEncodedReasoningForChatText() throws Exception {
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "model": "deepseek-v4-flash",
                  "messages": [
                    {
                      "role": "assistant",
                      "content": "Let me inspect the workspace.",
                      "reasoning_content": "Need to inspect first."
                    }
                  ]
                }
                """);
        ObjectNode convertedRequest = converter.convertRequest(request);
        ArrayNode input = (ArrayNode) convertedRequest.get("input");

        ObjectNode response = mapper.createObjectNode();
        response.put("id", "resp_1");
        response.put("model", "deepseek-v4-flash");
        ArrayNode output = mapper.createArrayNode();
        output.add(input.get(0).deepCopy());
        output.add(input.get(1).deepCopy());
        response.set("output", output);

        ObjectNode convertedResponse = converter.convertResponse(response);
        JsonNode message = convertedResponse.get("choices").get(0).get("message");

        assertEquals("Need to inspect first.", message.get("reasoning_content").asText());
        assertEquals("Let me inspect the workspace.", message.get("content").asText());
    }

    @Test
    void convertRequestMapsOpenAiImageContentToResponsesInputImage() throws Exception {
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "model": "gpt-4.1",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {"type": "text", "text": "describe"},
                        {"type": "image_url", "image_url": {"url": "data:image/png;base64,AAA="}}
                      ]
                    }
                  ]
                }
                """);

        ObjectNode converted = converter.convertRequest(request);
        JsonNode content = converted.get("input").get(0).get("content");

        assertEquals("input_text", content.get(0).get("type").asText());
        assertEquals("input_image", content.get(1).get("type").asText());
        assertEquals("data:image/png;base64,AAA=", content.get(1).get("image_url").asText());
    }

    @Test
    void streamResponseBuffersFunctionArgumentsUntilOutputItemAdded() throws Exception {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"type\":\"response.function_call_arguments.delta\",\"item_id\":\"fc_1\",\"output_index\":0,\"delta\":\"{\\\"cmd\"}"),
                sse("{\"type\":\"response.output_item.added\",\"output_index\":0,\"item\":{\"id\":\"fc_1\",\"type\":\"function_call\",\"call_id\":\"call_1\",\"name\":\"exec_command\",\"arguments\":\"\"}}"),
                sse("{\"type\":\"response.function_call_arguments.delta\",\"item_id\":\"fc_1\",\"output_index\":0,\"delta\":\"\\\":\\\"pwd\\\"}\"}"),
                sse("{\"type\":\"response.completed\",\"response\":{\"usage\":{\"input_tokens\":3,\"output_tokens\":2,\"total_tokens\":5}}}")
        )).collectList().block();

        assertNotNull(events);
        JsonNode start = nthToolCall(events, 0);
        JsonNode firstArgs = nthToolCall(events, 1);
        JsonNode secondArgs = nthToolCall(events, 2);
        JsonNode usage = findUsageChunk(events);

        assertEquals("call_1", start.get("id").asText());
        assertEquals("exec_command", start.get("function").get("name").asText());
        assertEquals("{\"cmd", firstArgs.get("function").get("arguments").asText());
        assertEquals("\":\"pwd\"}", secondArgs.get("function").get("arguments").asText());
        assertEquals(3, usage.get("prompt_tokens").asInt());
        assertEquals(2, usage.get("completion_tokens").asInt());
        assertEquals(5, usage.get("total_tokens").asInt());
    }

    @Test
    void streamResponseConvertsFunctionCallDoneWhenAddedWasMissing() throws Exception {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"type\":\"response.output_item.done\",\"output_index\":0,\"item\":{\"id\":\"fc_1\",\"type\":\"function_call\",\"call_id\":\"call_1\",\"name\":\"exec_command\",\"arguments\":\"{\\\"cmd\\\":\\\"pwd\\\"}\"}}"),
                sse("{\"type\":\"response.completed\",\"response\":{}}")
        )).collectList().block();

        assertNotNull(events);
        JsonNode start = nthToolCall(events, 0);
        JsonNode args = nthToolCall(events, 1);
        JsonNode finish = findFinishChunk(events);

        assertEquals("call_1", start.get("id").asText());
        assertEquals("exec_command", start.get("function").get("name").asText());
        assertEquals("{\"cmd\":\"pwd\"}", args.get("function").get("arguments").asText());
        assertEquals("tool_calls", finish.get("choices").get(0).get("finish_reason").asText());
    }

    @Test
    void streamResponseUsesFunctionCallDoneArgumentsWhenDeltasWereMissing() throws Exception {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"type\":\"response.output_item.added\",\"output_index\":0,\"item\":{\"id\":\"fc_1\",\"type\":\"function_call\",\"call_id\":\"call_1\",\"name\":\"exec_command\",\"arguments\":\"\"}}"),
                sse("{\"type\":\"response.output_item.done\",\"output_index\":0,\"item\":{\"id\":\"fc_1\",\"type\":\"function_call\",\"call_id\":\"call_1\",\"name\":\"exec_command\",\"arguments\":\"{\\\"cmd\\\":\\\"pwd\\\"}\"}}"),
                sse("{\"type\":\"response.completed\",\"response\":{}}")
        )).collectList().block();

        assertNotNull(events);
        JsonNode start = nthToolCall(events, 0);
        JsonNode args = nthToolCall(events, 1);

        assertEquals("call_1", start.get("id").asText());
        assertEquals("{\"cmd\":\"pwd\"}", args.get("function").get("arguments").asText());
    }

    private ServerSentEvent<String> sse(String data) {
        return ServerSentEvent.<String>builder().data(data).build();
    }

    private JsonNode nthToolCall(List<ServerSentEvent<String>> events, int ordinal) throws Exception {
        int seen = 0;
        for (ServerSentEvent<String> event : events) {
            if (event.data() == null || "[DONE]".equals(event.data())) continue;
            JsonNode node = mapper.readTree(event.data());
            JsonNode toolCalls = node.path("choices").path(0).path("delta").path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                if (seen == ordinal) {
                    return toolCalls.get(0);
                }
                seen++;
            }
        }
        throw new AssertionError("Missing tool call #" + ordinal);
    }

    private JsonNode findUsageChunk(List<ServerSentEvent<String>> events) throws Exception {
        for (ServerSentEvent<String> event : events) {
            if (event.data() == null || "[DONE]".equals(event.data())) continue;
            JsonNode node = mapper.readTree(event.data());
            if (node.has("usage")) {
                return node.get("usage");
            }
        }
        throw new AssertionError("Missing usage chunk");
    }

    private JsonNode findFinishChunk(List<ServerSentEvent<String>> events) throws Exception {
        for (ServerSentEvent<String> event : events) {
            if (event.data() == null || "[DONE]".equals(event.data())) continue;
            JsonNode node = mapper.readTree(event.data());
            JsonNode choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0 && !choices.get(0).path("finish_reason").isNull()) {
                return node;
            }
        }
        throw new AssertionError("Missing finish chunk");
    }
}
