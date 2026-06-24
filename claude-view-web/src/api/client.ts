const BASE = '/api'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) throw new Error(`API error: ${res.status}`)
  return res.json()
}

export interface Project {
  dirName: string
  displayPath: string
  sessionIds: string[]
  sessionCount: number
  hasMemory: boolean
  lastActivityAt: number
}

export interface SessionInfo {
  sessionId: string
  projectDirName: string
  lineCount: number
  fileSize: number
  title: string
  model: string
  createdAt: string
  updatedAt: string
  durationMs: number
  isActive: boolean
}

export interface ConversationLine {
  uuid: string
  type: string
  role: string
  timestamp: string
  model: string
  content: string
  thinking: string
  thinkingSignature: string
  usage: Record<string, number>
  toolUseName: string
  toolUseInput: Record<string, any>
  isSidechain: boolean
  agentId: string
  attributionAgent: string
  lineNumber: number
}

export interface ConversationPage {
  sessionId: string
  totalLines: number
  offset: number
  limit: number
  lines: ConversationLine[]
}

export interface MemoryFile {
  name: string
  description: string
  type: string
  projectDirName: string
  fileName: string
  content: string
  originSessionId: string
  lastModified: number
}

export interface HistoryEntry {
  display: string
  timestamp: number
  project: string
  sessionId: string
}

export interface HistoryPage {
  total: number
  offset: number
  limit: number
  entries: HistoryEntry[]
}

export interface HighlightDialogue {
  role: string
  content: string
  model?: string
}

export interface HighlightSession {
  projectName: string
  projectPath: string
  sessionId: string
  title: string
  model: string
  lineCount: number
  assistantChars: number
  dialogueCount: number
  createdAt: string
  dialogue: HighlightDialogue[]
}

export interface ActiveSession {
  pid: number
  sessionId: string
  cwd: string
  status: string
  version: string
  startedAt: number
  updatedAt: number
}

export const api = {
  getProjects: () => request<Project[]>('/projects'),

  getSessions: (projectName: string) =>
    request<SessionInfo[]>(`/projects/${projectName}/sessions`),

  getSession: (projectName: string, sessionId: string) =>
    request<SessionInfo>(`/projects/${projectName}/sessions/${sessionId}`),

  getConversation: (projectName: string, sessionId: string, offset = 0, limit = 50) =>
    request<ConversationPage>(
      `/conversations/${projectName}/${sessionId}?offset=${offset}&limit=${limit}`
    ),

  searchConversation: (projectName: string, sessionId: string, q: string) =>
    request<ConversationLine[]>(
      `/conversations/${projectName}/${sessionId}/search?q=${encodeURIComponent(q)}`
    ),

  getMemories: (projectName?: string) =>
    request<MemoryFile[]>(`/memories${projectName ? `?project=${projectName}` : ''}`),

  getMemory: (projectName: string, fileName: string) =>
    request<MemoryFile>(`/memories/${projectName}/${fileName}`),

  saveMemory: (projectName: string, fileName: string, data: Partial<MemoryFile>) =>
    request<any>(`/memories/${projectName}/${fileName}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  createMemory: (projectName: string, data: Partial<MemoryFile>) =>
    request<MemoryFile>(`/memories/${projectName}`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  deleteMemory: (projectName: string, fileName: string) =>
    request<any>(`/memories/${projectName}/${fileName}`, { method: 'DELETE' }),

  getHistory: (offset = 0, limit = 100) =>
    request<HistoryPage>(`/history?offset=${offset}&limit=${limit}`),

  getActiveSessions: () => request<ActiveSession[]>('/sessions/active'),

  getHighlights: (minLines = 30, minOutput = 200, limit = 20) =>
    request<HighlightSession[]>(
      `/highlights?minLines=${minLines}&minOutput=${minOutput}&limit=${limit}`
    ),

  search: (q: string) =>
    request<{ memories: MemoryFile[]; conversations: SessionInfo[] }>(
      `/search?q=${encodeURIComponent(q)}`
    ),
}
