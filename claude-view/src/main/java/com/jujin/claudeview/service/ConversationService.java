package com.jujin.claudeview.service;

import java.util.List;

import com.jujin.claudeview.model.ConversationLine;
import com.jujin.claudeview.model.ConversationPage;
import com.jujin.claudeview.model.SessionInfo;

public interface ConversationService {

    /** Get session summary info. */
    SessionInfo getSessionInfo(String projectDirName, String sessionId);

    /** Read a page of conversation lines. */
    ConversationPage readConversation(String projectDirName, String sessionId, int offset, int limit);

    /** Read all lines matching a keyword search within a session. */
    List<ConversationLine> searchInSession(String projectDirName, String sessionId, String keyword);
}
