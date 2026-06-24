<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, type SessionInfo } from '@/api/client'

const route = useRoute()
const router = useRouter()
const projectName = route.params.projectName as string
const sessions = ref<SessionInfo[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    sessions.value = await api.getSessions(projectName)
  } finally {
    loading.value = false
  }
})

function openDetail(session: SessionInfo) {
  router.push(`/conversation/${projectName}/${session.sessionId}`)
}

function formatTime(ts: string) {
  if (!ts) return ''
  return new Date(ts).toLocaleString()
}

function formatDuration(ms: number) {
  if (!ms) return ''
  const m = Math.floor(ms / 60000)
  if (m < 60) return `${m}m`
  return `${Math.floor(m / 60)}h ${m % 60}m`
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <div>
    <button class="btn" @click="router.push('/')" style="margin-bottom:16px;">← Back</button>
    <h1 class="page-title">Conversations</h1>
    <p style="color:var(--text-muted); font-size:13px; margin-bottom:16px;">{{ projectName }}</p>

    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="sessions.length === 0" class="empty">No conversations found</div>

    <div v-for="s in sessions" :key="s.sessionId" class="card" style="cursor:pointer;"
         @click="openDetail(s)">
      <div style="display:flex; justify-content:space-between; align-items:start;">
        <div style="flex:1;">
          <div style="font-weight:600; margin-bottom:4px;">
            {{ s.title }}
            <span v-if="s.isActive" class="badge badge-warning" style="margin-left:8px;">active</span>
          </div>
          <div style="font-size:12px; color:var(--text-muted); display:flex; gap:16px; flex-wrap:wrap;">
            <span>{{ s.lineCount }} lines</span>
            <span>{{ formatSize(s.fileSize) }}</span>
            <span>Model: {{ s.model }}</span>
            <span v-if="s.createdAt">{{ formatTime(s.createdAt) }}</span>
            <span v-if="s.durationMs">Duration: {{ formatDuration(s.durationMs) }}</span>
          </div>
        </div>
        <span style="font-size:20px;">→</span>
      </div>
    </div>
  </div>
</template>
