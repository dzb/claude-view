package com.jujin.claudeview.model;

import java.util.List;

/**
 * Response wrapper for history entries.
 */
public record HistoryPage(
    int total,
    int offset,
    int limit,
    List<HistoryEntry> entries
) {}
