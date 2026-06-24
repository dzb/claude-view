<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, type SessionInfo, type ConversationLine } from '@/api/client'
import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const marked = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code: string, lang: string) {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value
      }
      return hljs.highlightAuto(code).value
    },
  }),
  { gfm: true, breaks: false }
)

const route = useRoute()
const router = useRouter()
const projectName = route.params.projectName as string
const sessionId = route.params.sessionId as string

const session = ref<SessionInfo | null>(null)
const lines = ref<ConversationLine[]>([])
const loading = ref(true)
const offset = ref(0)
const totalLines = ref(0)
const limit = 100
const searchQuery = ref('')
const searchResults = ref<ConversationLine[]>([])
const showThinking = ref<Record<string, boolean>>({})

onMounted(async () => {
  try {
    session.value = await api.getSession(projectName, sessionId)
    await loadLines()
  } finally {
    loading.value = false
  }
})

async function loadLines() {
  const page = await api.getConversation(projectName, sessionId, offset.value, limit)
  totalLines.value = page.totalLines
  lines.value = page.lines
}

async function nextPage() {
  offset.value += limit
  await loadLines()
}

async function prevPage() {
  offset.value = Math.max(0, offset.value - limit)
  await loadLines()
}

async function doSearch() {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }
  searchResults.value = await api.searchConversation(projectName, sessionId, searchQuery.value)
}

function renderMarkdown(text: string | null | undefined): string {
  if (!text) return ''
  let html = marked.parse(text) as string
  // Replace Unicode check/cross with CSS-only flat symbols
  html = html.replace(/[✓✔✅]/g, '<span class="sym sym-check"></span>')
  html = html.replace(/[✗✘❌]/g, '<span class="sym sym-cross"></span>')
  return html
}

function formatTime(ts: string) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString()
}

function totalTokens(usage: Record<string, number> | null | undefined): number {
  if (!usage) return 0
  return (usage.input_tokens || 0) + (usage.output_tokens || 0)
}

function toggleThinking(uuid: string) {
  showThinking.value[uuid] = !showThinking.value[uuid]
}

// Noise types to filter out — system metadata, not real conversation
const NOISE_TYPES = new Set([
  'mode', 'permission-mode', 'file-history-snapshot',
  'attachment', 'ai-title', 'last-prompt', 'queue-operation'
])

const displayLines = computed(() => {
  const source = searchResults.value.length > 0 ? searchResults.value : lines.value
  return source.filter(l => {
    if (NOISE_TYPES.has(l.type || '')) return false
    // Filter out lines that are purely tool calls with no text content
    if (l.toolUseName && !l.content && !l.thinking) return false
    return true
  })
})

const hasMore = computed(() => offset.value + limit < totalLines.value)
const hasPrev = computed(() => offset.value > 0)
</script>

<template>
  <div>
    <button class="btn" @click="router.push(`/conversations/${projectName}`)" style="margin-bottom:16px;">← Back</button>

    <div v-if="loading" class="loading">Loading...</div>

    <template v-else-if="session">
      <h1 class="page-title">{{ session.title }}</h1>
      <div style="font-size:12px; color:var(--text-muted); margin-bottom:16px; display:flex; gap:16px;">
        <span>{{ session.lineCount }} lines</span>
        <span>Model: {{ session.model }}</span>
        <span v-if="session.createdAt">Started: {{ new Date(session.createdAt).toLocaleString() }}</span>
      </div>

      <!-- Search -->
      <div style="display:flex; gap:8px; margin-bottom:20px;">
        <input v-model="searchQuery" @keyup.enter="doSearch" placeholder="Search in conversation..."
          style="flex:1; padding:8px 12px; border:1px solid var(--border); border-radius:6px; font-size:13px;" />
        <button class="btn btn-primary" @click="doSearch">Search</button>
        <button v-if="searchResults.length" class="btn" @click="searchResults = []; searchQuery = '';">Clear</button>
      </div>

      <!-- Pagination -->
      <div v-if="searchResults.length === 0" style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
        <span style="font-size:13px; color:var(--text-muted);">
          Lines {{ offset + 1 }}–{{ Math.min(offset + limit, totalLines) }} of {{ totalLines }}
        </span>
        <div style="display:flex; gap:8px;">
          <button class="btn" :disabled="!hasPrev" @click="prevPage">← Prev</button>
          <button class="btn" :disabled="!hasMore" @click="nextPage">Next →</button>
        </div>
      </div>

      <!-- Conversation Lines -->
      <div v-for="line in displayLines" :key="line.uuid || line.lineNumber" style="margin-bottom:12px;">
        <!-- User message -->
        <div v-if="line.type === 'user' && line.content" :style="{ padding: '10px 14px', borderRadius: '8px', background: 'var(--primary-light)' }">
          <div style="font-size:11px; color:var(--text-muted); margin-bottom:4px;">You · {{ formatTime(line.timestamp) }}</div>
          <div style="white-space:pre-wrap; font-size:14px;">{{ line.content }}</div>
        </div>

        <!-- Assistant message -->
        <div v-else-if="line.type === 'assistant'" :style="{ padding: '10px 14px', borderRadius: '8px', background: 'var(--card-bg)', border: '1px solid var(--border)' }">
          <div style="font-size:11px; color:var(--text-muted); margin-bottom:4px; display:flex; gap:12px;">
            <span>🤖 {{ line.model || 'Assistant' }}</span>
            <span>{{ formatTime(line.timestamp) }}</span>
            <span v-if="line.usage">Tokens: {{ totalTokens(line.usage) }}</span>
            <span v-if="line.isSidechain" class="badge badge-info">sub-agent</span>
            <span v-if="line.attributionAgent">{{ line.attributionAgent }}</span>
          </div>

          <!-- Thinking -->
          <div v-if="line.thinking" style="margin-bottom:8px;">
            <button class="btn" style="font-size:11px;"
              @click="toggleThinking(line.uuid || String(line.lineNumber))">
              {{ showThinking[line.uuid || line.lineNumber] ? '🧠 Hide' : '🧠 Show' }} Thinking
            </button>
            <div v-if="showThinking[line.uuid || line.lineNumber]"
              style="margin-top:8px; padding:10px; background:var(--thinking-bg); border-radius:6px; font-size:12px; white-space:pre-wrap; max-height:300px; overflow-y:auto;">
              {{ line.thinking }}
            </div>
          </div>

          <!-- Text content -->
          <div v-if="line.content" class="markdown-body" v-html="renderMarkdown(line.content)"></div>

        </div>

      </div>

      <!-- Bottom pagination -->
      <div v-if="searchResults.length === 0 && totalLines > limit" style="display:flex; justify-content:center; gap:8px; margin-top:16px;">
        <button class="btn" :disabled="!hasPrev" @click="prevPage">← Prev</button>
        <span style="padding:8px; font-size:13px; color:var(--text-muted);">
          {{ offset + 1 }}–{{ Math.min(offset + limit, totalLines) }} / {{ totalLines }}
        </span>
        <button class="btn" :disabled="!hasMore" @click="nextPage">Next →</button>
      </div>
    </template>
  </div>
</template>

<style>
/* All markdown styles now in App.vue */
</style>
