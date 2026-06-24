package com.jujin.claudeview.model;

import java.util.List;

/**
 * Represents a Claude Code project (maps to a directory under ~/.claude/projects/).
 */
public record Project(
    String dirName,       // e.g. "-Users-apple-Projects-claude-view"
    String displayPath,   // e.g. "/Users/apple/Projects/claude-view"
    List<String> sessionIds,
    int sessionCount,
    boolean hasMemory,
    long lastActivityAt    // epoch millis of most recent session
) {}
