package com.jujin.claudeview.service;

import java.util.List;

import com.jujin.claudeview.model.MemoryFile;

public interface MemoryService {

    /** List all memories across all projects. */
    List<MemoryFile> listAllMemories();

    /** List memories for a specific project. */
    List<MemoryFile> listMemories(String projectDirName);

    /** Get a single memory file's content and metadata. */
    MemoryFile getMemory(String projectDirName, String fileName);

    /** Create or update a memory file. */
    void saveMemory(String projectDirName, String fileName, MemoryFile memory);

    /** Delete a memory file. */
    void deleteMemory(String projectDirName, String fileName);
}
