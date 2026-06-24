package com.jujin.claudeview.model;

import java.util.List;

/**
 * Response wrapper for paginated conversation lines.
 */
public record ConversationPage(
    String sessionId,
    int totalLines,
    int offset,
    int limit,
    List<ConversationLine> lines
) {}
