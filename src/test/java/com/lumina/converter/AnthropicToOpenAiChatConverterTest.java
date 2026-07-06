package com.lumina.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicToOpenAiChatConverterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AnthropicToOpenAiChatConverter converter = new AnthropicToOpenAiChatConverter();

    @Test
    void convertRequestMapsAnthropicImageBlockToOpenAiImageUrlPart() throws Exception {
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "model": "gpt-4.1",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {"type": "text", "text": "describe"},
                        {
                          "type": "image",
                          "source": {
                            "type": "base64",
                            "media_type": "image/png",
                            "data": "AAA="
                          }
                        }
                      ]
                    }
                  ]
                }
                """);

        ObjectNode converted = converter.convertRequest(request);
        JsonNode content = converted.get("messages").get(0).get("content");

        assertEquals("text", content.get(0).get("type").asText());
        assertEquals("image_url", content.get(1).get("type").asText());
        assertEquals("data:image/png;base64,AAA=", content.get(1).get("image_url").get("url").asText());
    }

    @Test
    void convertRequestKeepsToolResultsBeforeFollowingUserText() throws Exception {
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "model": "gpt-4.1",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {"type": "tool_result", "tool_use_id": "tool_1", "content": "ok"},
                        {"type": "text", "text": "continue"}
                      ]
                    }
                  ]
                }
                """);

        ObjectNode converted = converter.convertRequest(request);

        assertEquals("tool", converted.get("messages").get(0).get("role").asText());
        assertEquals("tool_1", converted.get("messages").get(0).get("tool_call_id").asText());
        assertEquals("user", converted.get("messages").get(1).get("role").asText());
        assertEquals("continue", converted.get("messages").get(1).get("content").asText());
    }

    @Test
    void streamResponseDoesNotStartEmptyTextBlockBeforeToolCall() throws Exception {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"id\":\"chatcmpl_1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-v4-pro\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"},\"finish_reason\":null}]}"),
                sse("{\"id\":\"chatcmpl_1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-v4-pro\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"TaskCreate\",\"arguments\":\"\"}}]},\"finish_reason\":null}]}"),
                sse("{\"id\":\"chatcmpl_1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-v4-pro\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"description\\\":\"}}]},\"finish_reason\":null}]}"),
                sse("{\"id\":\"chatcmpl_1\",\"object\":\"chat.completion.chunk\",\"model\":\"deepseek-v4-pro\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"\\\"继续搜索\\\"}\"}}]},\"finish_reason\":null}]}"),
                sse("[DONE]")
        )).collectList().block();

        List<JsonNode> blockStarts = events.stream()
                .filter(event -> "content_block_start".equals(event.event()))
                .map(event -> {
                    try {
                        return mapper.readTree(event.data());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        assertEquals(1, blockStarts.size());
        assertEquals(0, blockStarts.get(0).get("index").asInt());
        assertEquals("tool_use", blockStarts.get(0).get("content_block").get("type").asText());
        assertFalse(events.stream().anyMatch(event -> event.data() != null && event.data().contains("text_delta")));
    }

    @Test
    void streamResponseCompletesGlmToolCallWithUsageAndToolStop() throws Exception {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"id\":\"chatcmpl_glm\",\"object\":\"chat.completion.chunk\",\"model\":\"glm-5.2\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\"},\"finish_reason\":null}]}"),
                sse("{\"id\":\"chatcmpl_glm\",\"object\":\"chat.completion.chunk\",\"model\":\"glm-5.2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"Bash\",\"arguments\":\"{\\\"command\\\":\"}}]},\"finish_reason\":null}]}"),
                sse("{\"id\":\"chatcmpl_glm\",\"object\":\"chat.completion.chunk\",\"model\":\"glm-5.2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"\\\"pwd\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":4,\"prompt_tokens_details\":{\"cached_tokens\":3}}}"),
                sse("[DONE]")
        )).collectList().block();

        assertEquals(1, countEvents(events, "content_block_start"));
        assertEquals(1, countEvents(events, "content_block_stop"));
        JsonNode toolStart = firstEventData(events, "content_block_start");
        assertEquals("tool_use", toolStart.get("content_block").get("type").asText());
        assertEquals("call_1", toolStart.get("content_block").get("id").asText());
        assertEquals("Bash", toolStart.get("content_block").get("name").asText());

        String joined = events.stream().map(ServerSentEvent::data).filter(data -> data != null).reduce("", String::concat);
        assertTrue(joined.contains("\"partial_json\":\"{\\\"command\\\":\""));
        assertTrue(joined.contains("\"partial_json\":\"\\\"pwd\\\"}\""));

        JsonNode messageDelta = firstEventData(events, "message_delta");
        assertEquals("tool_use", messageDelta.get("delta").get("stop_reason").asText());
        assertEquals(9, messageDelta.get("usage").get("input_tokens").asInt());
        assertEquals(3, messageDelta.get("usage").get("cache_read_input_tokens").asInt());
        assertEquals(4, messageDelta.get("usage").get("output_tokens").asInt());
        assertEquals("message_stop", events.get(events.size() - 1).event());
    }

    @Test
    void streamResponseBuffersToolArgumentsUntilIdAndNameArrive() throws Exception {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"id\":\"chatcmpl_late\",\"model\":\"glm-5.2\",\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"path\\\":\"}}]}}]}"),
                sse("{\"id\":\"chatcmpl_late\",\"model\":\"glm-5.2\",\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_late\",\"type\":\"function\",\"function\":{\"name\":\"Read\",\"arguments\":\"\\\"README.md\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}]}"),
                sse("[DONE]")
        )).collectList().block();

        JsonNode toolStart = firstEventData(events, "content_block_start");
        assertEquals("call_late", toolStart.get("content_block").get("id").asText());
        assertEquals("Read", toolStart.get("content_block").get("name").asText());
        String joined = events.stream().map(ServerSentEvent::data).filter(data -> data != null).reduce("", String::concat);
        assertTrue(joined.contains("\"partial_json\":\"{\\\"path\\\":\""));
        assertTrue(joined.contains("\"partial_json\":\"\\\"README.md\\\"}\""));
    }

    @Test
    void streamResponseFinalizesWhenUpstreamCompletesWithoutDone() throws Exception {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"id\":\"chatcmpl_no_done\",\"model\":\"glm-5.2\",\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"Bash\",\"arguments\":\"{}\"}}]},\"finish_reason\":\"tool_calls\"}]}")
        )).collectList().block();

        assertEquals(1, countEvents(events, "message_delta"));
        assertEquals(1, countEvents(events, "message_stop"));
        assertEquals("message_stop", events.get(events.size() - 1).event());
    }

    @Test
    void streamResponseStopsToolBlockOnFinishReasonBeforeDone() {
        List<ServerSentEvent<String>> events = converter.convertStreamResponse(Flux.just(
                sse("{\"id\":\"chatcmpl_finish\",\"model\":\"glm-5.2\",\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"Bash\",\"arguments\":\"{}\"}}]},\"finish_reason\":\"tool_calls\"}]}"),
                sse("{\"id\":\"chatcmpl_finish\",\"model\":\"glm-5.2\",\"choices\":[],\"usage\":{\"prompt_tokens\":7,\"completion_tokens\":2}}"),
                sse("[DONE]")
        )).collectList().block();

        int blockStopIndex = eventIndex(events, "content_block_stop");
        int messageDeltaIndex = eventIndex(events, "message_delta");
        assertTrue(blockStopIndex > -1);
        assertTrue(messageDeltaIndex > blockStopIndex);
        JsonNode messageDelta = firstEventData(events, "message_delta");
        assertEquals(7, messageDelta.get("usage").get("input_tokens").asInt());
        assertEquals(2, messageDelta.get("usage").get("output_tokens").asInt());
    }

    private ServerSentEvent<String> sse(String data) {
        return ServerSentEvent.<String>builder().data(data).build();
    }

    private long countEvents(List<ServerSentEvent<String>> events, String eventType) {
        return events.stream().filter(event -> eventType.equals(event.event())).count();
    }

    private JsonNode firstEventData(List<ServerSentEvent<String>> events, String eventType) {
        return events.stream()
                .filter(event -> eventType.equals(event.event()))
                .findFirst()
                .map(event -> {
                    try {
                        return mapper.readTree(event.data());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow();
    }

    private int eventIndex(List<ServerSentEvent<String>> events, String eventType) {
        for (int i = 0; i < events.size(); i++) {
            if (eventType.equals(events.get(i).event())) {
                return i;
            }
        }
        return -1;
    }
}
