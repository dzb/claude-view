package com.jujin.claudeview.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.jujin.claudeview.model.Project;

public class ClaudeDataServiceImpl implements ClaudeDataService {

    private final Path claudeHome;

    public ClaudeDataServiceImpl() {
        String home = System.getProperty("user.home");
        this.claudeHome = Path.of(home, ".claude");
    }

    @Override
    public Path claudeHome() {
        return claudeHome;
    }

    @Override
    public List<Project> listProjects() {
        List<Project> projects = new ArrayList<>();
        Path projectsDir = claudeHome.resolve("projects");
        if (!Files.isDirectory(projectsDir)) return projects;

        try (var dirs = Files.list(projectsDir)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                String dirName = dir.getFileName().toString();
                List<String> sessionIds = listSessionIds(dirName);
                boolean hasMemory = Files.isDirectory(dir.resolve("memory")) &&
                    Files.exists(dir.resolve("memory").resolve("MEMORY.md"));

                long lastActivity = 0;
                for (String sid : sessionIds) {
                    try {
                        long mod = Files.getLastModifiedTime(dir.resolve(sid + ".jsonl")).toMillis();
                        if (mod > lastActivity) lastActivity = mod;
                    } catch (IOException ignored) {}
                }

                projects.add(new Project(
                    dirName,
                    unsanitizeProjectPath(dirName),
                    sessionIds,
                    sessionIds.size(),
                    hasMemory,
                    lastActivity
                ));
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list projects", e);
        }

        projects.sort(Comparator.comparingLong(Project::lastActivityAt).reversed());
        return projects;
    }

    @Override
    public List<String> listSessionIds(String projectDirName) {
        List<String> ids = new ArrayList<>();
        Path projectDir = claudeHome.resolve("projects").resolve(projectDirName);
        if (!Files.isDirectory(projectDir)) return ids;

        try (var files = Files.list(projectDir)) {
            files.filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(name -> name.endsWith(".jsonl"))
                .map(name -> name.substring(0, name.length() - 6))
                .forEach(ids::add);
        } catch (IOException ignored) {}

        ids.sort(Comparator.reverseOrder());
        return ids;
    }

    @Override
    public Path conversationPath(String projectDirName, String sessionId) {
        return claudeHome.resolve("projects").resolve(projectDirName)
            .resolve(sessionId + ".jsonl");
    }

    @Override
    public Path memoryPath(String projectDirName) {
        return claudeHome.resolve("projects").resolve(projectDirName).resolve("memory");
    }

    @Override
    public String unsanitizeProjectPath(String dirName) {
        // Claude Code encodes paths by replacing '/' with '-'.
        // First char is always '-' (the leading '/'). Reverse this.
        // Path segments may themselves contain '-' (e.g. "freeway-2").
        // Strategy: try replacing all dashes; if the path doesn't exist,
        // walk back replacing the last '/' back to '-' until we find a match.
        String base = dirName.substring(1).replace('-', '/');
        String candidate = "/" + base;

        if (java.nio.file.Files.isDirectory(java.nio.file.Path.of(candidate))) {
            return candidate;
        }

        // Path has embedded dashes. Walk back from the end, replacing '/' back to '-'
        // one at a time until the path exists.
        int lastSlash = candidate.lastIndexOf('/');
        while (lastSlash > 0) {
            candidate = candidate.substring(0, lastSlash) + "-" + candidate.substring(lastSlash + 1);
            if (java.nio.file.Files.isDirectory(java.nio.file.Path.of(candidate))) {
                return candidate;
            }
            lastSlash = candidate.lastIndexOf('/');
        }

        // Last resort: return the simple replace
        return "/" + base;
    }

    @Override
    public Path historyPath() {
        return claudeHome.resolve("history.jsonl");
    }

    @Override
    public Path sessionsPath() {
        return claudeHome.resolve("sessions");
    }
}
