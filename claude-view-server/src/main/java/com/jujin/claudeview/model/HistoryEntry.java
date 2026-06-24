package com.jujin.claudeview.model;

/**
 * A prompt history entry from ~/.claude/history.jsonl.
 */
public record HistoryEntry(
    String display,
    long timestamp,
    String project,
    String sessionId
) {}
