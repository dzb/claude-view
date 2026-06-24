<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, type Project, type ActiveSession } from '@/api/client'

const router = useRouter()
const projects = ref<Project[]>([])
const activeSessions = ref<ActiveSession[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [p, s] = await Promise.all([api.getProjects(), api.getActiveSessions()])
    projects.value = p
    activeSessions.value = s
  } finally {
    loading.value = false
  }
})

function openConversations(project: Project) {
  router.push(`/conversations/${project.dirName}`)
}

function openMemories(project: Project) {
  router.push(`/memories/${project.dirName}`)
}

function formatTime(ts: number) {
  if (!ts) return ''
  return new Date(ts).toLocaleString()
}
</script>

<template>
  <div>
    <h1 class="page-title">
  <svg width="20" height="20" viewBox="0 0 16 16" fill="none" style="vertical-align:-3px;margin-right:6px;"><rect x="1" y="1" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/><rect x="9" y="1" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/><rect x="1" y="9" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/><rect x="9" y="9" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/></svg>
  Dashboard
</h1>

    <div v-if="loading" class="loading">Loading...</div>

    <template v-else>
      <!-- Stats row -->
      <div style="display: flex; gap: 16px; margin-bottom: 24px;">
        <div class="card" style="flex:1; text-align:center;">
          <div style="font-size:28px; font-weight:700;">{{ projects.length }}</div>
          <div style="color:var(--text-muted); font-size:13px;">Projects</div>
        </div>
        <div class="card" style="flex:1; text-align:center;">
          <div style="font-size:28px; font-weight:700;">
            {{ projects.reduce((s, p) => s + p.sessionCount, 0) }}
          </div>
          <div style="color:var(--text-muted); font-size:13px;">Conversations</div>
        </div>
        <div class="card" style="flex:1; text-align:center;">
          <div style="font-size:28px; font-weight:700;">{{ activeSessions.length }}</div>
          <div style="color:var(--text-muted); font-size:13px;">Active Sessions</div>
        </div>
        <div class="card" style="flex:1; text-align:center;">
          <div style="font-size:28px; font-weight:700;">
            {{ projects.filter(p => p.hasMemory).length }}
          </div>
          <div style="color:var(--text-muted); font-size:13px;">With Memory</div>
        </div>
      </div>

      <!-- Projects -->
      <h2 style="font-size:16px; margin-bottom:12px; color:var(--text);">Projects</h2>
      <div v-if="projects.length === 0" class="empty">No projects found</div>
      <div v-for="p in projects" :key="p.dirName" class="card" style="display:flex; align-items:center; justify-content:space-between;">
        <div>
          <div style="font-weight:600; margin-bottom:4px;">{{ p.displayPath }}</div>
          <div style="font-size:12px; color:var(--text-muted);">
            {{ p.sessionCount }} sessions
            <span v-if="p.lastActivityAt"> · Last: {{ formatTime(p.lastActivityAt) }}</span>
          </div>
        </div>
        <div style="display:flex; gap:8px;">
          <span v-if="p.hasMemory" class="badge badge-info">memory</span>
          <button class="btn" @click="openMemories(p)">Memories</button>
          <button class="btn btn-primary" @click="openConversations(p)">View</button>
        </div>
      </div>

      <!-- Active Sessions -->
      <h2 v-if="activeSessions.length" style="font-size:16px; margin:24px 0 12px; color:var(--text);">
        Active Sessions
      </h2>
      <div v-for="s in activeSessions" :key="s.sessionId" class="card" style="display:flex; align-items:center; justify-content:space-between;">
        <div>
          <div style="font-weight:600;">{{ s.cwd }}</div>
          <div style="font-size:12px; color:var(--text-muted);">
            PID {{ s.pid }} · v{{ s.version }} · {{ s.status }}
          </div>
        </div>
        <span class="badge" :class="s.status === 'busy' ? 'badge-warning' : 'badge-success'">
          {{ s.status }}
        </span>
      </div>
    </template>
  </div>
</template>
