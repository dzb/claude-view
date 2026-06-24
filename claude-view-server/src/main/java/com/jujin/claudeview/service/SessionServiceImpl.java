package com.jujin.claudeview.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.jujin.freeway.commons.json.JsonUtils;

public class SessionServiceImpl implements SessionService {

    private final ClaudeDataService dataService;

    public SessionServiceImpl(ClaudeDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public boolean findActive(String projectDirName, String sessionId) {
        return listActive().stream()
            .anyMatch(s -> s.sessionId().equals(sessionId));
    }

    @Override
    public List<ActiveSession> listActive() {
        List<ActiveSession> sessions = new ArrayList<>();
        Path sessionsDir = dataService.sessionsPath();
        if (!Files.isDirectory(sessionsDir)) return sessions;

        try (var files = Files.list(sessionsDir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(p -> {
                    try {
                        String json = Files.readString(p);
                        var obj = JsonUtils.parseObject(json);
                        sessions.add(new ActiveSession(
                            obj.getLong("pid") != null ? obj.getLong("pid").intValue() : 0,
                            obj.getString("sessionId"),
                            obj.getString("cwd"),
                            obj.getString("status"),
                            obj.getString("version"),
                            obj.getLong("startedAt") != null ? obj.getLong("startedAt") : 0L,
                            obj.getLong("updatedAt") != null ? obj.getLong("updatedAt") : 0L
                        ));
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}

        return sessions;
    }
}
