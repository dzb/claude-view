package com.jujin.claudeview;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jujin.claudeview.model.MemoryFile;
import com.jujin.claudeview.service.ClaudeDataService;
import com.jujin.claudeview.service.ClaudeDataServiceImpl;
import com.jujin.claudeview.service.ConversationService;
import com.jujin.claudeview.service.ConversationServiceImpl;
import com.jujin.claudeview.service.MemoryService;
import com.jujin.claudeview.service.MemoryServiceImpl;
import com.jujin.claudeview.service.SessionService;
import com.jujin.claudeview.service.SessionServiceImpl;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.commons.json.JsonUtils;
import com.jujin.freeway.http.HttpModule;
import com.jujin.freeway.http.WebServer;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.staticfile.StaticResourceMount;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.Module2;
import com.jujin.freeway.ioc.RuntimeHook;

/**
 * Claude View — A web app for viewing Claude Code memories and conversation history.
 * <p>
 * Entry point. Run with: {@code java -jar claude-view-server.jar}
 * or via Maven: {@code mvn exec:java -Dexec.mainClass="com.jujin.claudeview.App"}
 */
public final class App implements Module2 {

    @Override
    public void bind(Binder b) {
        b.install(new HttpModule());

        // Print access URL on startup
        b.contribute(RuntimeHook.class)
            .add("printer", new RuntimeHook() {
                public void start(com.jujin.freeway.ioc.Container c) {
                    var server = c.get(WebServer.class);
                    System.out.println();
                    System.out.println("  🧠 Claude View ready:  http://localhost:" + server.port());
                    System.out.println();
                }
                public void stop(com.jujin.freeway.ioc.Container c) {}
            });

        // Create service instances directly — they have no @Inject dependencies
        ClaudeDataService dataService = new ClaudeDataServiceImpl();
        SessionService sessionService = new SessionServiceImpl(dataService);
        ConversationService conversationService = new ConversationServiceImpl(dataService, sessionService);
        MemoryService memoryService = new MemoryServiceImpl(dataService);

        // Bind them so they're accessible from outside if needed
        b.bind(ClaudeDataService.class).to(dataService);
        b.bind(SessionService.class).to(sessionService);
        b.bind(ConversationService.class).to(conversationService);
        b.bind(MemoryService.class).to(memoryService);

        // --- REST API Routes ---

        // Projects
        b.contribute(RouteGroup.class)
            .add(RouteGroup.of("/api",
                Route.get("/projects", ctx ->
                    ctx.sendJson(200, dataService.listProjects())),

                Route.get("/projects/{projectName}/sessions", ctx -> {
                    String projectName = ctx.pathVar("projectName");
                    var sessions = dataService.listSessionIds(projectName).stream()
                        .map(sid -> conversationService.getSessionInfo(projectName, sid))
                        .toList();
                    ctx.sendJson(200, sessions);
                }),

                Route.get("/projects/{projectName}/sessions/{sessionId}", ctx -> {
                    String projectName = ctx.pathVar("projectName");
                    String sessionId = ctx.pathVar("sessionId");
                    ctx.sendJson(200, conversationService.getSessionInfo(projectName, sessionId));
                })
            ));

        // Conversations
        b.contribute(RouteGroup.class)
            .add(RouteGroup.of("/api",
                Route.get("/conversations/{projectName}/{sessionId}", ctx -> {
                    String projectName = ctx.pathVar("projectName");
                    String sessionId = ctx.pathVar("sessionId");
                    int offset = parseIntParam(ctx.queryParam("offset"), 0);
                    int limit = parseIntParam(ctx.queryParam("limit"), 50);
                    limit = Math.min(limit, 500);
                    ctx.sendJson(200, conversationService.readConversation(projectName, sessionId, offset, limit));
                }),

                Route.get("/conversations/{projectName}/{sessionId}/search", ctx -> {
                    String projectName = ctx.pathVar("projectName");
                    String sessionId = ctx.pathVar("sessionId");
                    String q = ctx.queryParam("q");
                    if (q == null || q.isBlank()) {
                        ctx.sendJson(200, List.of());
                        return;
                    }
                    ctx.sendJson(200, conversationService.searchInSession(projectName, sessionId, q));
                })
            ));

        // Memories
        b.contribute(RouteGroup.class)
            .add(RouteGroup.of("/api",
                Route.get("/memories", ctx -> {
                    String project = ctx.queryParam("project");
                    if (project != null && !project.isBlank()) {
                        ctx.sendJson(200, memoryService.listMemories(project));
                    } else {
                        ctx.sendJson(200, memoryService.listAllMemories());
                    }
                }),

                Route.get("/memories/{projectName}/{fileName}", ctx -> {
                    String projectName = ctx.pathVar("projectName");
                    String fileName = ctx.pathVar("fileName");
                    ctx.sendJson(200, memoryService.getMemory(projectName, fileName));
                }),

                Route.put("/memories/{projectName}/{fileName}", String.class, (ctx, body) -> {
                    String projectName = ctx.pathVar("projectName");
                    String fileName = ctx.pathVar("fileName");
                    var json = JsonUtils.parseObject(body);
                    MemoryFile mem = new MemoryFile(
                        json.getString("name"),
                        json.getString("description"),
                        json.getString("type"),
                        projectName,
                        fileName,
                        json.getString("content"),
                        json.getString("originSessionId"),
                        System.currentTimeMillis()
                    );
                    memoryService.saveMemory(projectName, fileName, mem);
                    ctx.sendJson(200, Map.of("ok", true));
                }),

                Route.post("/memories/{projectName}", String.class, (ctx, body) -> {
                    String projectName = ctx.pathVar("projectName");
                    var json = JsonUtils.parseObject(body);
                    String name = json.getString("name");
                    if (name == null || name.isBlank()) {
                        ctx.sendJson(400, Map.of("error", "name is required"));
                        return;
                    }
                    String fileName = name.replaceAll("[^a-zA-Z0-9_\\-]", "-") + ".md";
                    MemoryFile mem = new MemoryFile(
                        name,
                        json.getString("description"),
                        json.getString("type"),
                        projectName,
                        fileName,
                        json.getString("content"),
                        json.getString("originSessionId"),
                        System.currentTimeMillis()
                    );
                    memoryService.saveMemory(projectName, fileName, mem);
                    ctx.sendJson(201, mem);
                }),

                Route.delete("/memories/{projectName}/{fileName}", ctx -> {
                    String projectName = ctx.pathVar("projectName");
                    String fileName = ctx.pathVar("fileName");
                    memoryService.deleteMemory(projectName, fileName);
                    ctx.sendJson(200, Map.of("ok", true));
                })
            ));

        // History
        b.contribute(RouteGroup.class)
            .add(RouteGroup.of("/api",
                Route.get("/history", ctx -> {
                    Path historyFile = dataService.historyPath();
                    if (!Files.exists(historyFile)) {
                        ctx.sendJson(200, List.of());
                        return;
                    }

                    int offset = parseIntParam(ctx.queryParam("offset"), 0);
                    int limit = parseIntParam(ctx.queryParam("limit"), 100);
                    limit = Math.min(limit, 500);

                    var allLines = Files.readAllLines(historyFile);
                    int total = allLines.size();
                    int start = Math.min(offset, total);
                    int end = Math.min(start + limit, total);

                    var entries = new ArrayList<>();
                    for (int i = start; i < end; i++) {
                        var obj = JsonUtils.parseObject(allLines.get(i));
                        entries.add(Map.of(
                            "display", (Object) obj.getString("display"),
                            "timestamp", obj.getLong("timestamp"),
                            "project", (Object) obj.getString("project"),
                            "sessionId", (Object) obj.getString("sessionId")
                        ));
                    }

                    ctx.sendJson(200, Map.of("total", total, "offset", offset, "limit", limit, "entries", entries));
                })
            ));

        // Active sessions
        b.contribute(RouteGroup.class)
            .add(RouteGroup.of("/api",
                Route.get("/sessions/active", ctx ->
                    ctx.sendJson(200, sessionService.listActive()))
            ));

        // Global search
        b.contribute(RouteGroup.class)
            .add(RouteGroup.of("/api",
                Route.get("/search", ctx -> {
                    String q = ctx.queryParam("q");
                    if (q == null || q.isBlank()) {
                        ctx.sendJson(200, Map.of("memories", List.of(), "conversations", List.of()));
                        return;
                    }

                    String lowerQ = q.toLowerCase();
                    var memResults = new ArrayList<>();
                    for (var proj : dataService.listProjects()) {
                        if (proj.hasMemory()) {
                            for (var mem : memoryService.listMemories(proj.dirName())) {
                                if (mem.content() != null && mem.content().toLowerCase().contains(lowerQ)) {
                                    memResults.add(mem);
                                }
                            }
                        }
                    }

                    var convResults = new ArrayList<>();
                    for (var proj : dataService.listProjects()) {
                        for (String sid : dataService.listSessionIds(proj.dirName())) {
                            try {
                                var info = conversationService.getSessionInfo(proj.dirName(), sid);
                                if (info.title() != null && info.title().toLowerCase().contains(lowerQ)) {
                                    convResults.add(info);
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    ctx.sendJson(200, Map.of("memories", memResults, "conversations", convResults));
                })
            ));

        // Highlights — curated conversations with substantial Claude output
        b.contribute(RouteGroup.class)
            .add(RouteGroup.of("/api",
                Route.get("/highlights", ctx -> {
                    int minLines = parseIntParam(ctx.queryParam("minLines"), 30);
                    int minOutput = parseIntParam(ctx.queryParam("minOutput"), 200);
                    int limit = parseIntParam(ctx.queryParam("limit"), 20);

                    var results = new ArrayList<Map<String, Object>>();

                    for (var proj : dataService.listProjects()) {
                        for (String sid : dataService.listSessionIds(proj.dirName())) {
                            if (results.size() >= limit) break;

                            Path file = dataService.conversationPath(proj.dirName(), sid);
                            if (!Files.exists(file)) continue;

                            var lines = Files.readAllLines(file);
                            if (lines.size() < minLines) continue;

                            // Extract clean dialogue (no thinking, no tool_use input)
                            var dialogue = new ArrayList<Map<String, Object>>();
                            long assistantCharCount = 0;

                            for (String line : lines) {
                                var obj = JsonUtils.parseObject(line);
                                String type = obj.getString("type");

                                if ("user".equals(type)) {
                                    var msg = obj.getObject("message");
                                    if (msg == null) continue;
                                    Object c = msg.get("content");
                                    if (c instanceof String text && !text.startsWith("<")) {
                                        dialogue.add(Map.of("role", "user", "content", (Object) text));
                                    }
                                } else if ("assistant".equals(type)) {
                                    var msg = obj.getObject("message");
                                    if (msg == null) continue;
                                    String model = msg.getString("model");
                                    Object raw = msg.get("content");
                                    String textContent = "";

                                    if (raw instanceof String s) {
                                        textContent = s;
                                    } else if (raw instanceof com.jujin.freeway.commons.json.JsonArray arr) {
                                        var sb = new StringBuilder();
                                        for (int i = 0; i < arr.size(); i++) {
                                            var block = arr.getObject(i);
                                            if (block == null) continue;
                                            String bt = block.getString("type");
                                            if ("text".equals(bt)) {
                                                String t = block.getString("text");
                                                if (t != null) sb.append(t);
                                            }
                                            // Skip thinking, tool_use blocks
                                        }
                                        textContent = sb.toString();
                                    }

                                    if (!textContent.isBlank()) {
                                        assistantCharCount += textContent.length();
                                        var entry = new HashMap<String, Object>();
                                        entry.put("role", "assistant");
                                        entry.put("content", textContent);
                                        if (model != null) entry.put("model", model);
                                        dialogue.add(entry);
                                    }
                                }
                            }

                            if (assistantCharCount < minOutput) continue;

                            var info = conversationService.getSessionInfo(proj.dirName(), sid);

                            results.add(Map.of(
                                "projectName", (Object) proj.dirName(),
                                "projectPath", (Object) proj.displayPath(),
                                "sessionId", (Object) sid,
                                "title", (Object) info.title(),
                                "model", (Object) info.model(),
                                "lineCount", lines.size(),
                                "assistantChars", assistantCharCount,
                                "dialogueCount", dialogue.size(),
                                "createdAt", (Object) info.createdAt(),
                                "dialogue", (Object) dialogue
                            ));
                        }
                    }

                    // Sort by assistant output length (most substantial first)
                    results.sort((x, y) ->
                        Long.compare((long) y.get("assistantChars"), (long) x.get("assistantChars")));

                    ctx.sendJson(200, results);
                })
            ));

        // Static file serving for Vue3 frontend with SPA fallthrough
        // fallthrough(true) ensures missing files pass through to the filter below
        b.contribute(StaticResourceMount.class)
            .add(StaticResourceMount.classpath("/", "/web").fallthrough(true));

        // SPA fallback filter: serve index.html for SPA routes that don't match static files
        // This filter runs AFTER static mounts but BEFORE route dispatch.
        b.contribute(com.jujin.freeway.http.filter.HttpFilter.class)
            .add("spa-fallback", (ctx, next) -> {
                String path = ctx.path();
                // API paths: let them reach route handlers
                if (path.startsWith("/api/")) {
                    next.handle(ctx);
                    return;
                }
                // Paths with file extensions: let them be handled (should have been caught by static mount)
                if (path.contains(".")) {
                    next.handle(ctx);
                    return;
                }
                // SPA route — serve index.html
                var stream = App.class.getResourceAsStream("/web/index.html");
                if (stream != null) {
                    try (stream) {
                        ctx.status(200);
                        ctx.headerSet("Content-Type", "text/html; charset=utf-8");
                        ctx.output(stream.readAllBytes());
                    }
                } else {
                    next.handle(ctx);
                }
            });
    }

    private static int parseIntParam(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void main(String[] args) {
        // Default to random port unless user specifies one
        var merged = new String[args.length + 1];
        System.arraycopy(args, 0, merged, 0, args.length);
        merged[args.length] = "--freeway.web.server.port=0";
        FreewayApp.run(merged, new App());
    }
}
