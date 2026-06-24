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

function activeCountText(project: Project): string {
  const cwd = project.displayPath || project.dirName
  const active = activeSessions.value.filter(s => s.cwd === cwd)
  return active.length ? `${active.length} active` : ''
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
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-value">{{ projects.length }}</div>
          <div class="stat-label">Projects</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ projects.reduce((s, p) => s + p.sessionCount, 0) }}</div>
          <div class="stat-label">Conversations</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ activeSessions.length }}</div>
          <div class="stat-label">Active Sessions</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">
            <svg width="20" height="20" viewBox="0 0 16 16" fill="none" style="vertical-align:-2px;margin-right:4px;"><path d="M3 2h7a2 2 0 012 2v8a2 2 0 01-2 2H3a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="#8b7e6a" stroke-width="1.5"/><path d="M5 5h3M5 8h4M5 11h2" stroke="#8b7e6a" stroke-width="1.5" stroke-linecap="round"/></svg>
            {{ projects.filter(p => p.hasMemory).length }}
          </div>
          <div class="stat-label">With Memory</div>
        </div>
      </div>

      <!-- Projects -->
      <h2 class="section-title">Projects</h2>
      <div v-if="projects.length === 0" class="empty">No projects found</div>
      <div v-else class="project-grid">
        <div
          v-for="p in projects"
          :key="p.dirName"
          class="project-card"
          @click="openConversations(p)"
        >
          <div class="project-card-header">
            <span class="project-path" :title="p.displayPath">{{ p.displayPath }}</span>
            <span v-if="p.hasMemory" class="memory-tag">
              <svg width="12" height="12" viewBox="0 0 16 16" fill="none" style="vertical-align:-2px;"><path d="M3 2h7a2 2 0 012 2v8a2 2 0 01-2 2H3a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="currentColor" stroke-width="1.5"/><path d="M5 5h3M5 8h4M5 11h2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              memory
            </span>
          </div>

          <div class="project-card-meta">
            <span>{{ p.sessionCount }} sessions</span>
            <span v-if="p.lastActivityAt">{{ formatTime(p.lastActivityAt) }}</span>
          </div>

          <div class="project-card-footer">
            <span v-if="activeCountText(p)" class="badge badge-success">{{ activeCountText(p) }}</span>
            <span v-else class="badge badge-inactive">idle</span>
            <button class="btn" @click.stop="openMemories(p)">Memories</button>
            <button class="btn btn-primary" @click.stop="openConversations(p)">View</button>
          </div>
        </div>
      </div>

      <!-- Active Sessions -->
      <h2 v-if="activeSessions.length" class="section-title" style="margin-top:28px;">
        Active Sessions
      </h2>
      <div v-for="s in activeSessions" :key="s.sessionId" class="card active-session-row">
        <div>
          <div class="active-session-cwd">{{ s.cwd }}</div>
          <div class="active-session-meta">
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

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 28px;
}

.stat-card {
  background: var(--card-bg);
  border-radius: 10px;
  border: 1px solid var(--border);
  padding: 18px 14px;
  text-align: center;
  transition: box-shadow 0.15s;
}
.stat-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text);
  line-height: 1.2;
}
.stat-label {
  color: var(--text-muted);
  font-size: 12.5px;
  margin-top: 4px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
  color: var(--text);
}

/* ---------- Project Grid ---------- */
.project-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.project-card {
  background: var(--card-bg);
  border-radius: 10px;
  border: 1px solid var(--border);
  padding: 18px 20px;
  cursor: pointer;
  transition: box-shadow 0.15s, border-color 0.15s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.project-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  border-color: #c8cdd4;
}

.project-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.project-path {
  font-weight: 600;
  font-size: 13.5px;
  color: var(--text);
  word-break: break-all;
  line-height: 1.4;
  /* clamp to 2 lines */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
}

.project-card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 2px;
}

.project-card-footer .btn {
  font-size: 12px;
  padding: 5px 12px;
}

.badge-inactive {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  background: #f0f0f0;
  color: #888;
}

/* ---------- Active Sessions ---------- */
.active-session-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.active-session-cwd {
  font-weight: 600;
  font-size: 14px;
}

.active-session-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 3px;
}

/* ---------- Responsive ---------- */
@media (max-width: 1100px) {
  .project-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 700px) {
  .project-grid {
    grid-template-columns: 1fr;
  }
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
