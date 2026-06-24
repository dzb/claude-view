package com.jujin.claudeview.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.jujin.claudeview.model.Project;

/**
 * Core service for locating and enumerating Claude Code data.
 */
public interface ClaudeDataService {

    /** Return the .claude home directory (usually ~/.claude/). */
    Path claudeHome();

    /** List all projects found under ~/.claude/projects/. */
    List<Project> listProjects();

    /** List all conversation file names (UUID.jsonl) for a given project. */
    List<String> listSessionIds(String projectDirName);

    /** Get the full path to a conversation file. */
    Path conversationPath(String projectDirName, String sessionId);

    /** Get the full path to the memory directory for a project. */
    Path memoryPath(String projectDirName);

    /** Resolve a sanitized project directory name back to its original path. */
    String unsanitizeProjectPath(String dirName);

    /** Get the path to history.jsonl. */
    Path historyPath();

    /** Get the path to the sessions directory. */
    Path sessionsPath();
}
