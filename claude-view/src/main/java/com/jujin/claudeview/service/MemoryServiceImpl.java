package com.jujin.claudeview.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jujin.claudeview.model.MemoryFile;

public class MemoryServiceImpl implements MemoryService {

    private final ClaudeDataService dataService;

    // Pattern to parse YAML frontmatter fields
    private static final Pattern NAME_PATTERN = Pattern.compile("^name:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern DESC_PATTERN = Pattern.compile("^description:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\s*type:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern SESSION_PATTERN = Pattern.compile("originSessionId:\\s*(.+)$", Pattern.MULTILINE);

    public MemoryServiceImpl(ClaudeDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public List<MemoryFile> listAllMemories() {
        List<MemoryFile> all = new ArrayList<>();
        for (var project : dataService.listProjects()) {
            if (project.hasMemory()) {
                all.addAll(listMemories(project.dirName()));
            }
        }
        return all;
    }

    @Override
    public List<MemoryFile> listMemories(String projectDirName) {
        List<MemoryFile> memories = new ArrayList<>();
        Path memDir = dataService.memoryPath(projectDirName);
        if (!Files.isDirectory(memDir)) return memories;

        try (var files = Files.list(memDir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".md"))
                .filter(p -> !"MEMORY.md".equals(p.getFileName().toString()))
                .forEach(p -> {
                    try {
                        memories.add(readMemoryFile(projectDirName, p));
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}

        return memories;
    }

    @Override
    public MemoryFile getMemory(String projectDirName, String fileName) {
        Path file = dataService.memoryPath(projectDirName).resolve(fileName);
        if (!Files.exists(file)) throw new RuntimeException("Memory file not found: " + fileName);
        try {
            return readMemoryFile(projectDirName, file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read memory file", e);
        }
    }

    @Override
    public void saveMemory(String projectDirName, String fileName, MemoryFile memory) {
        Path memDir = dataService.memoryPath(projectDirName);
        try {
            Files.createDirectories(memDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create memory directory", e);
        }

        // Build YAML frontmatter + content
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(memory.name()).append("\n");
        sb.append("description: ").append(memory.description() != null ? memory.description() : "").append("\n");
        sb.append("metadata:\n");
        sb.append("  type: ").append(memory.type() != null ? memory.type() : "reference").append("\n");
        if (memory.originSessionId() != null && !memory.originSessionId().isBlank()) {
            sb.append("  originSessionId: ").append(memory.originSessionId()).append("\n");
        }
        sb.append("---\n\n");
        sb.append(memory.content() != null ? memory.content() : "");

        try {
            Files.writeString(memDir.resolve(fileName), sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write memory file", e);
        }

        // Update MEMORY.md index
        updateMemoryIndex(projectDirName);
    }

    @Override
    public void deleteMemory(String projectDirName, String fileName) {
        Path file = dataService.memoryPath(projectDirName).resolve(fileName);
        try {
            Files.deleteIfExists(file);
            updateMemoryIndex(projectDirName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete memory file", e);
        }
    }

    private MemoryFile readMemoryFile(String projectDirName, Path file) throws IOException {
        String content = Files.readString(file);
        String fileName = file.getFileName().toString();
        long lastModified = Files.getLastModifiedTime(file).toMillis();

        // Parse YAML frontmatter
        String name = extractPattern(content, NAME_PATTERN, fileName.replace(".md", ""));
        String description = extractPattern(content, DESC_PATTERN, "");
        String type = extractPattern(content, TYPE_PATTERN, "reference");
        String sessionId = extractPattern(content, SESSION_PATTERN, null);

        // Strip frontmatter for clean content
        String bodyContent = content;
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end > 0) {
                bodyContent = content.substring(end + 3).trim();
            }
        }

        return new MemoryFile(name, description, type, projectDirName, fileName, bodyContent, sessionId, lastModified);
    }

    private String extractPattern(String content, Pattern pattern, String defaultValue) {
        Matcher m = pattern.matcher(content);
        return m.find() ? m.group(1).trim() : defaultValue;
    }

    private void updateMemoryIndex(String projectDirName) {
        Path memDir = dataService.memoryPath(projectDirName);
        List<MemoryFile> memories = listMemories(projectDirName);

        StringBuilder index = new StringBuilder();
        for (var m : memories) {
            index.append("- [").append(m.name()).append("](").append(m.fileName()).append(")");
            if (m.description() != null && !m.description().isBlank()) {
                index.append(" — ").append(m.description());
            }
            index.append("\n");
        }

        try {
            Files.writeString(memDir.resolve("MEMORY.md"), index.toString());
        } catch (IOException ignored) {}
    }
}
