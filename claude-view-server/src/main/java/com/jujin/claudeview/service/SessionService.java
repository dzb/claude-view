package com.jujin.claudeview.service;

import java.util.List;

public interface SessionService {

    /** Find if a specific session is currently active. */
    boolean findActive(String projectDirName, String sessionId);

    /** List all active sessions from ~/.claude/sessions/. */
    List<ActiveSession> listActive();

    record ActiveSession(
        int pid,
        String sessionId,
        String cwd,
        String status,
        String version,
        long startedAt,
        long updatedAt
    ) {}
}
