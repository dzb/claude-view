<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, type HistoryEntry, type Project } from '@/api/client'

const router = useRouter()
const entries = ref<HistoryEntry[]>([])
const total = ref(0)
const offset = ref(0)
const limit = 100
const loading = ref(true)
const projects = ref<Project[]>([])

onMounted(async () => {
  try {
    const [hist, proj] = await Promise.all([
      api.getHistory(0, limit),
      api.getProjects(),
    ])
    entries.value = hist.entries
    total.value = hist.total
    projects.value = proj
  } finally {
    loading.value = false
  }
})

async function loadMore() {
  offset.value += limit
  const hist = await api.getHistory(offset.value, limit)
  entries.value.push(...hist.entries)
  total.value = hist.total
}

function openConversation(entry: HistoryEntry) {
  // Find project dir name from path
  const proj = projects.value.find(p => p.displayPath === entry.project)
  if (proj && entry.sessionId) {
    router.push(`/conversation/${proj.dirName}/${entry.sessionId}`)
  }
}

function formatTime(ts: number) {
  if (!ts) return ''
  return new Date(ts).toLocaleString()
}

function hasMore() {
  return offset.value + limit < total.value
}
</script>

<template>
  <div>
    <h1 class="page-title">
  <svg width="20" height="20" viewBox="0 0 16 16" fill="none" style="vertical-align:-3px;margin-right:6px;"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 4.5V8l2.5 1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
  Prompt History
</h1>
    <p style="font-size:13px; color:var(--text-muted); margin-bottom:20px;">
      {{ entries.length }} of {{ total }} entries from ~/.claude/history.jsonl
    </p>

    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="entries.length === 0" class="empty">No history found</div>

    <div v-for="e in entries" :key="e.sessionId + '-' + e.timestamp" class="card"
         style="cursor:pointer;" @click="openConversation(e)">
      <div style="font-size:14px; margin-bottom:4px;">
        {{ e.display }}
      </div>
      <div style="font-size:12px; color:var(--text-muted); display:flex; gap:12px;">
        <span>{{ formatTime(e.timestamp) }}</span>
        <span>{{ e.project }}</span>
        <span style="font-family:monospace;">{{ e.sessionId?.substring(0, 8) }}</span>
      </div>
    </div>

    <div v-if="hasMore()" style="text-align:center; margin-top:16px;">
      <button class="btn btn-primary" @click="loadMore">Load More</button>
    </div>
  </div>
</template>
