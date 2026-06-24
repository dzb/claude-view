package com.jujin.claudeview.model;

import java.util.Map;

/**
 * A single line from a conversation JSONL file.
 */
public record ConversationLine(
    String uuid,
    String type,            // "user", "assistant", "mode", "attachment", "ai-title", etc.
    String role,            // "user", "assistant", "system"
    String timestamp,       // ISO 8601
    String model,           // present on assistant lines
    String content,         // text content (user message, or assistant text)
    String thinking,        // present on assistant lines with thinking
    String thinkingSignature,
    Map<String, Object> usage,  // token usage {input_tokens, output_tokens, ...}
    String toolUseName,     // if this line contains a tool_use
    Map<String, Object> toolUseInput,  // tool_use input params
    boolean isSidechain,    // sub-agent conversation
    String agentId,
    String attributionAgent,
    int lineNumber          // position in the file
) {}
