package com.jujin.claudeview.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jujin.claudeview.model.ConversationLine;
import com.jujin.claudeview.model.ConversationPage;
import com.jujin.claudeview.model.SessionInfo;
import com.jujin.freeway.commons.json.JsonUtils;

public class ConversationServiceImpl implements ConversationService {

    private final ClaudeDataService dataService;
    private final SessionService sessionService;

    public ConversationServiceImpl(ClaudeDataService dataService, SessionService sessionService) {
        this.dataService = dataService;
        this.sessionService = sessionService;
    }

    @Override
    public SessionInfo getSessionInfo(String projectDirName, String sessionId) {
        Path file = dataService.conversationPath(projectDirName, sessionId);
        if (!Files.exists(file)) throw new RuntimeException("Session not found: " + sessionId);

        try {
            List<String> allLines = Files.readAllLines(file);
            int lineCount = allLines.size();
            long fileSize = Files.size(file);

            String title = sessionId.substring(0, 8);
            String model = "unknown";
            String createdAt = null;
            String updatedAt = null;
            long firstTs = 0, lastTs = 0;

            for (String line : allLines) {
                var obj = JsonUtils.parseObject(line);
                String lineType = obj.getString("type");

                // Extract title from first user message
                if (title.equals(sessionId.substring(0, 8)) && "user".equals(lineType)) {
                    var message = obj.getObject("message");
                    if (message != null) {
                        Object c = message.get("content");
                        if (c instanceof String text && !text.startsWith("<") && !text.isBlank()) {
                            title = text.length() > 80 ? text.substring(0, 80) + "..." : text;
                        }
                    }
                }

                // Extract model from assistant lines
                if ("unknown".equals(model) && "assistant".equals(lineType)) {
                    var message = obj.getObject("message");
                    if (message != null) {
                        String m = message.getString("model");
                        if (m != null && !m.isBlank()) model = m;
                    }
                }

                // Track timestamps
                String ts = obj.getString("timestamp");
                if (ts != null) {
                    if (createdAt == null) createdAt = ts;
                    updatedAt = ts;
                }
            }

            if (createdAt != null && updatedAt != null) {
                try {
                    firstTs = java.time.Instant.parse(createdAt).toEpochMilli();
                    lastTs = java.time.Instant.parse(updatedAt).toEpochMilli();
                } catch (Exception ignored) {}
            }

            boolean isActive = sessionService.findActive(projectDirName, sessionId);

            return new SessionInfo(
                sessionId, projectDirName, lineCount, fileSize,
                title, model, createdAt, updatedAt,
                lastTs - firstTs, isActive
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to read session: " + sessionId, e);
        }
    }

    @Override
    public ConversationPage readConversation(String projectDirName, String sessionId, int offset, int limit) {
        Path file = dataService.conversationPath(projectDirName, sessionId);
        if (!Files.exists(file)) throw new RuntimeException("Session not found: " + sessionId);

        try {
            List<String> allLines = Files.readAllLines(file);
            int totalLines = allLines.size();
            List<ConversationLine> result = new ArrayList<>();

            int end = Math.min(offset + limit, totalLines);
            for (int i = offset; i < end; i++) {
                result.add(parseLine(allLines.get(i), i + 1));
            }

            return new ConversationPage(sessionId, totalLines, offset, limit, result);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read conversation", e);
        }
    }

    @Override
    public List<ConversationLine> searchInSession(String projectDirName, String sessionId, String keyword) {
        Path file = dataService.conversationPath(projectDirName, sessionId);
        if (!Files.exists(file)) return List.of();

        List<ConversationLine> matches = new ArrayList<>();
        try (var reader = Files.newBufferedReader(file)) {
            String line;
            int lineNum = 0;
            String lowerKeyword = keyword.toLowerCase();
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.toLowerCase().contains(lowerKeyword)) {
                    matches.add(parseLine(line, lineNum));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
        return matches;
    }

    private ConversationLine parseLine(String jsonLine, int lineNumber) {
        var obj = JsonUtils.parseObject(jsonLine);

        String type = obj.getString("type");
        String uuid = obj.getString("uuid");
        String timestamp = obj.getString("timestamp");
        Boolean isSidechain = obj.getBoolean("isSidechain");
        String agentId = obj.getString("agentId");
        String attrAgent = obj.getString("attributionAgent");

        String role = null, content = null, thinking = null, thinkingSig = null;
        String model = null, toolUseName = null;
        Map<String, Object> usage = null;
        Map<String, Object> toolUseInput = null;

        var message = obj.getObject("message");
        if (message != null) {
            role = message.getString("role");
            model = message.getString("model");

            // Content: could be String (user messages) or Array of content blocks (assistant)
            Object rawContent = message.get("content");
            if (rawContent instanceof String s) {
                content = s;
            } else if (rawContent instanceof com.jujin.freeway.commons.json.JsonArray arr) {
                StringBuilder textBuf = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    var block = arr.getObject(i);
                    if (block == null) continue;
                    String blockType = block.getString("type");
                    if ("text".equals(blockType)) {
                        String t = block.getString("text");
                        if (t != null) textBuf.append(t);
                    } else if ("thinking".equals(blockType)) {
                        thinking = block.getString("thinking");
                        thinkingSig = block.getString("signature");
                    } else if ("tool_use".equals(blockType)) {
                        toolUseName = block.getString("name");
                        var inputObj = block.getObject("input");
                        if (inputObj != null) {
                            toolUseInput = new HashMap<>(inputObj.toMap());
                        }
                    } else if ("tool_result".equals(blockType)) {
                        // Tool results are skipped — they add noise to conversation view
                    }
                }
                content = !textBuf.isEmpty() ? textBuf.toString() : null;
            }

            // Usage
            var usageObj = message.getObject("usage");
            if (usageObj != null) {
                usage = new HashMap<>();
                Long it = usageObj.getLong("input_tokens");
                Long ot = usageObj.getLong("output_tokens");
                Long cr = usageObj.getLong("cache_read_input_tokens");
                Long cc = usageObj.getLong("cache_creation_input_tokens");
                if (it != null) usage.put("input_tokens", it);
                if (ot != null) usage.put("output_tokens", ot);
                if (cr != null) usage.put("cache_read_input_tokens", cr);
                if (cc != null) usage.put("cache_creation_input_tokens", cc);
            }
        }


        return new ConversationLine(
            uuid, type, role, timestamp, model,
            content, thinking, thinkingSig,
            usage, toolUseName, toolUseInput,
            isSidechain != null && isSidechain, agentId, attrAgent, lineNumber
        );
    }
}
