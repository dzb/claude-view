<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, type HighlightSession } from '@/api/client'
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

const router = useRouter()
const sessions = ref<HighlightSession[]>([])
const loading = ref(true)
const expanded = ref<Record<string, boolean>>({})

onMounted(async () => {
  try {
    sessions.value = await api.getHighlights(30, 200, 20)
  } finally {
    loading.value = false
  }
})

function toggleExpand(sessionId: string) {
  expanded.value[sessionId] = !expanded.value[sessionId]
}

function openDetail(s: HighlightSession) {
  router.push(`/conversation/${s.projectName}/${s.sessionId}`)
}

function renderMd(text: string): string {
  if (!text) return ''
  let html = marked.parse(text) as string
  html = html.replace(/[✓✔✅]/g, '<span class="sym sym-check"></span>')
  html = html.replace(/[✗✘❌]/g, '<span class="sym sym-cross"></span>')
  return html
}

function formatTime(ts: string) {
  if (!ts) return ''
  return new Date(ts).toLocaleString()
}

function preview(text: string, len = 120): string {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '…' : text
}
</script>

<template>
  <div>
    <h1 class="page-title">
  <svg width="20" height="20" viewBox="0 0 16 16" fill="none" style="vertical-align:-3px;margin-right:6px;"><path d="M8 1l2.5 5 5.5.8-4 3.9.9 5.3L8 13.5 3.1 16l.9-5.3-4-3.9 5.5-.8L8 1z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
  Highlights
</h1>
    <p style="font-size:13px; color:var(--text-muted); margin-bottom:20px;">
      Curated conversations with substantial Claude output (≥30 lines, ≥200 chars of clean text, no thinking/tool noise)
    </p>

    <div v-if="loading" class="loading">Scanning conversations...</div>

    <div v-else-if="sessions.length === 0" class="empty">No conversations meet the highlight criteria yet. Have a longer conversation with Claude!</div>

    <div v-for="s in sessions" :key="s.sessionId" class="card">
      <!-- Header -->
      <div style="display:flex; justify-content:space-between; align-items:start; cursor:pointer;"
           @click="toggleExpand(s.sessionId)">
        <div style="flex:1;">
          <div style="font-weight:600; margin-bottom:4px; font-size:15px;">
            {{ s.title }}
          </div>
          <div style="font-size:12px; color:var(--text-muted); display:flex; gap:14px; flex-wrap:wrap;">
            <span>{{ s.projectPath }}</span>
            <span>{{ s.lineCount }} lines</span>
            <span>{{ s.dialogueCount }} messages</span>
            <span class="badge badge-info">{{ (s.assistantChars / 1000).toFixed(1) }}k chars</span>
            <span>{{ s.model }}</span>
            <span v-if="s.createdAt">{{ formatTime(s.createdAt) }}</span>
          </div>
        </div>
        <span style="font-size:18px; color:var(--text-muted); transition:0.2s;"
              :style="{ transform: expanded[s.sessionId] ? 'rotate(90deg)' : '' }">▶</span>
      </div>

      <!-- Expanded dialogue -->
      <div v-if="expanded[s.sessionId]" style="margin-top:16px; padding-top:16px; border-top:1px solid var(--border);">
        <div style="max-height:55vh; overflow-y:auto; margin-bottom:12px;">
          <div v-for="(d, i) in s.dialogue" :key="i" style="margin-bottom:10px;">
            <!-- User -->
            <div v-if="d.role === 'user'" style="padding:8px 12px; border-radius:6px; background:var(--primary-light); margin-bottom:6px;">
              <div style="font-size:10px; color:var(--text-muted); margin-bottom:2px;">You</div>
              <div style="font-size:13px; white-space:pre-wrap;">{{ d.content }}</div>
            </div>
            <!-- Assistant -->
            <div v-else-if="d.role === 'assistant'" style="padding:8px 12px; border-radius:6px; background:#fafafa; border:1px solid #eee;">
              <div style="font-size:10px; color:var(--text-muted); margin-bottom:2px;">
                🤖 {{ d.model || 'Claude' }}
              </div>
              <div class="markdown-body" v-html="renderMd(preview(d.content, 300))"></div>
              <div v-if="d.content && d.content.length > 300" style="font-size:11px; color:var(--text-muted); margin-top:4px;">
                … {{ d.content.length }} chars total ·
                <button class="btn" style="font-size:10px; padding:2px 6px;" @click.stop="openDetail(s)">full →</button>
              </div>
            </div>
          </div>
        </div>

        <button class="btn" @click.stop="openDetail(s)">Full →</button>
      </div>
    </div>
  </div>
</template>
