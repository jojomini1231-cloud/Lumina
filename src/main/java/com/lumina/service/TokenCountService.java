package com.lumina.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenCountService {

    private final EncodingRegistry registry;
    private final ConcurrentHashMap<String, Encoding> encodingCache;

    public TokenCountService() {
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.encodingCache = new ConcurrentHashMap<>();
    }

    public int countTokens(String modelName, JsonNode params) {
        if (modelName == null) {
            modelName = "gpt-4o";
        }
        Encoding encoding = getEncodingForModel(modelName);
        
        int tokenCount = 0;
        
        // Handle input text if present (for simple count_tokens endpoint)
        if (params.has("text")) {
            tokenCount += encoding.countTokens(params.get("text").asText());
        }

        // Handle standard chat completions messages array
        if (params.has("messages") && params.get("messages").isArray()) {
            for (JsonNode message : params.get("messages")) {
                tokenCount += 3; // Every message follows <im_start>{role/name}\n{content}<im_end>\n
                if (message.has("content")) {
                    JsonNode contentNode = message.get("content");
                    if (contentNode.isTextual()) {
                        tokenCount += encoding.countTokens(contentNode.asText());
                    } else if (contentNode.isArray()) {
                        // Handle array content (like vision models with text parts)
                        for (JsonNode part : contentNode) {
                            if (part.has("type") && "text".equals(part.get("type").asText()) && part.has("text")) {
                                tokenCount += encoding.countTokens(part.get("text").asText());
                            }
                            // Note: image token calculation is complex and often model-specific, 
                            // simplified here to just count text parts
                        }
                    }
                }
                if (message.has("role")) {
                    tokenCount += encoding.countTokens(message.get("role").asText());
                }
                if (message.has("name")) {
                    tokenCount += encoding.countTokens(message.get("name").asText());
                    tokenCount += 1; // if there's a name, the role is omitted
                }
            }
            tokenCount += 3; // every reply is primed with <im_start>assistant
        }

        // Return at least 0
        return Math.max(0, tokenCount);
    }

    private Encoding getEncodingForModel(String modelName) {
        return encodingCache.computeIfAbsent(modelName, this::resolveEncodingForModel);
    }

    private Encoding resolveEncodingForModel(String modelName) {
        // Find the right encoding based on model name
        Optional<Encoding> encoding = registry.getEncodingForModel(modelName);
        
        if (encoding.isPresent()) {
            return encoding.get();
        }

        // Fallback heuristics for common model families if exact match fails
        if (modelName.contains("gpt-4o") || modelName.contains("gpt-4")) {
            return registry.getEncodingForModel(ModelType.GPT_4O);
        } else if (modelName.contains("claude") || modelName.contains("gemini")) {
            // Approximation
            return registry.getEncodingForModel(ModelType.GPT_4O);
        }

        // Default fallback
        return registry.getEncodingForModel(ModelType.GPT_4O);
    }
}
