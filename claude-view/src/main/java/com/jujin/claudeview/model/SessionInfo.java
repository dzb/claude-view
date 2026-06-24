package com.jujin.claudeview.model;

/**
 * Summary info about a conversation session (a .jsonl file).
 */
public record SessionInfo(
    String sessionId,       // UUID
    String projectDirName,  // parent directory name
    int lineCount,
    long fileSize,
    String title,           // extracted from first user message or ai-title
    String model,           // e.g. "deepseek-v4-flash"
    String createdAt,       // ISO timestamp from first line
    String updatedAt,       // ISO timestamp from last line
    long durationMs,        // time between first and last message
    boolean isActive        // whether a running session exists in ~/.claude/sessions/
) {}
