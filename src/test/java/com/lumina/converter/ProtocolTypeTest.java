package com.lumina.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolTypeTest {

    @Test
    void mapsOpenAiImagesGenerationsRequestType() {
        ProtocolType type = ProtocolType.fromRequestType("openai_images_generations");

        assertEquals(ProtocolType.OPENAI_IMAGES, type);
        assertEquals(4, type.getCode());
        assertEquals("openai_images_generations", type.toRequestType());
    }

    @Test
    void registryCoversAllTextProtocolConversions() {
        ProtocolConverterRegistry registry = new ProtocolConverterRegistry(List.of(
                new AnthropicToOpenAiChatConverter(),
                new OpenAiChatToAnthropicConverter(),
                new OpenAiChatToResponsesConverter(),
                new ResponsesToOpenAiChatConverter(),
                new AnthropicToResponsesConverter(new AnthropicToOpenAiChatConverter(), new OpenAiChatToResponsesConverter()),
                new ResponsesToAnthropicConverter(new ResponsesToOpenAiChatConverter(), new OpenAiChatToAnthropicConverter())
        ));

        ProtocolType[] textProtocols = {
                ProtocolType.OPENAI_CHAT,
                ProtocolType.OPENAI_RESPONSES,
                ProtocolType.ANTHROPIC
        };

        for (ProtocolType source : textProtocols) {
            for (ProtocolType target : textProtocols) {
                if (source == target) continue;
                assertTrue(registry.needsConversion(source, target), source + "→" + target + " converter missing");
            }
        }
    }
}
