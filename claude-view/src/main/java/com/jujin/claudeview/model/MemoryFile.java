package com.jujin.claudeview.model;

/**
 * A Claude Code memory file (.md with YAML frontmatter).
 */
public record MemoryFile(
    String name,            // from frontmatter: name
    String description,     // from frontmatter: description
    String type,            // from frontmatter: metadata.type (user, feedback, project, reference)
    String projectDirName,  // which project this memory belongs to
    String fileName,        // actual file name on disk
    String content,         // full markdown content
    String originSessionId, // from metadata.originSessionId
    long lastModified       // file last modified timestamp
) {}
