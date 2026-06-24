<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, type MemoryFile, type Project } from '@/api/client'
import { marked } from 'marked'

const route = useRoute()
const router = useRouter()
const projectName = route.params.projectName as string | undefined

const memories = ref<MemoryFile[]>([])
const projects = ref<Project[]>([])
const loading = ref(true)
const editMode = ref(false)
const editing = ref<MemoryFile | null>(null)
const editContent = ref('')
const editName = ref('')
const editDesc = ref('')
const editType = ref('reference')
const createMode = ref(false)

onMounted(async () => {
  try {
    const [m, p] = await Promise.all([
      api.getMemories(projectName),
      api.getProjects(),
    ])
    memories.value = m
    projects.value = p
  } finally {
    loading.value = false
  }
})

function openEdit(mem: MemoryFile) {
  editing.value = mem
  editContent.value = mem.content || ''
  editName.value = mem.name
  editDesc.value = mem.description || ''
  editType.value = mem.type || 'reference'
  editMode.value = true
}

async function saveEdit() {
  if (!editing.value) return
  await api.saveMemory(editing.value.projectDirName, editing.value.fileName, {
    name: editName.value,
    description: editDesc.value,
    type: editType.value,
    content: editContent.value,
  })
  editMode.value = false
  editing.value = null
  memories.value = await api.getMemories(projectName)
}

async function deleteMemory(mem: MemoryFile) {
  if (!confirm(`Delete "${mem.name}"?`)) return
  await api.deleteMemory(mem.projectDirName, mem.fileName)
  memories.value = await api.getMemories(projectName)
}

function startCreate() {
  createMode.value = true
  editName.value = ''
  editDesc.value = ''
  editType.value = 'reference'
  editContent.value = ''
}

async function doCreate() {
  await api.createMemory(projectName || projects.value[0]?.dirName || '', {
    name: editName.value,
    description: editDesc.value,
    type: editType.value,
    content: editContent.value,
  })
  createMode.value = false
  memories.value = await api.getMemories(projectName)
}

function cancelEdit() {
  editMode.value = false
  createMode.value = false
  editing.value = null
}

function renderMarkdown(text: string | null | undefined): string {
  if (!text) return ''
  return marked(text) as string
}

function projectLabel(dirName: string) {
  const p = projects.value.find(x => x.dirName === dirName)
  return p ? p.displayPath : dirName
}

const typeColors: Record<string, string> = {
  feedback: 'badge-warning',
  project: 'badge-info',
  reference: 'badge-success',
  user: 'badge-info',
}

const grouped = computed(() => {
  const map: Record<string, MemoryFile[]> = {}
  for (const m of memories.value) {
    const key = m.projectDirName
    if (!map[key]) map[key] = []
    map[key].push(m)
  }
  return map
})
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
      <h1 class="page-title" style="margin-bottom:0;">
        <svg width="20" height="20" viewBox="0 0 16 16" fill="none" style="vertical-align:-3px;margin-right:6px;"><path d="M3 2h7a2 2 0 012 2v8a2 2 0 01-2 2H3a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="currentColor" stroke-width="1.5"/><path d="M5 5h3M5 8h4M5 11h2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        {{ projectName ? `Memories · ${projectName}` : 'All Memories' }}
      </h1>
      <button class="btn btn-primary" @click="startCreate">+ New Memory</button>
    </div>

    <div v-if="loading" class="loading">Loading...</div>

    <!-- Create modal -->
    <div v-if="createMode" class="card" style="border:2px solid var(--primary);">
      <h3 style="margin-bottom:12px;">New Memory</h3>
      <div style="display:flex; gap:12px; margin-bottom:12px;">
        <div style="flex:1;">
          <label style="font-size:12px; color:var(--text-muted);">Name</label>
          <input v-model="editName" style="width:100%; padding:6px 10px; border:1px solid var(--border); border-radius:4px;" />
        </div>
        <div style="flex:1;">
          <label style="font-size:12px; color:var(--text-muted);">Description</label>
          <input v-model="editDesc" style="width:100%; padding:6px 10px; border:1px solid var(--border); border-radius:4px;" />
        </div>
        <div>
          <label style="font-size:12px; color:var(--text-muted);">Type</label>
          <select v-model="editType" style="padding:6px 10px; border:1px solid var(--border); border-radius:4px;">
            <option value="reference">reference</option>
            <option value="feedback">feedback</option>
            <option value="project">project</option>
          </select>
        </div>
      </div>
      <textarea v-model="editContent" rows="12"
        style="width:100%; padding:10px; border:1px solid var(--border); border-radius:4px; font-family:monospace; font-size:13px;"></textarea>
      <div style="margin-top:12px; display:flex; gap:8px;">
        <button class="btn btn-primary" @click="doCreate" :disabled="!editName.trim()">Create</button>
        <button class="btn" @click="cancelEdit">Cancel</button>
      </div>
    </div>

    <!-- Edit modal -->
    <div v-if="editMode && editing" class="card" style="border:2px solid var(--primary);">
      <h3 style="margin-bottom:12px;">Edit: {{ editing.name }}</h3>
      <div style="font-size:12px; color:var(--text-muted); margin-bottom:8px;">
        {{ editing.projectDirName }} / {{ editing.fileName }}
      </div>
      <div style="display:flex; gap:12px; margin-bottom:12px;">
        <div style="flex:1;">
          <label style="font-size:12px; color:var(--text-muted);">Name</label>
          <input v-model="editName" style="width:100%; padding:6px 10px; border:1px solid var(--border); border-radius:4px;" />
        </div>
        <div style="flex:1;">
          <label style="font-size:12px; color:var(--text-muted);">Description</label>
          <input v-model="editDesc" style="width:100%; padding:6px 10px; border:1px solid var(--border); border-radius:4px;" />
        </div>
        <div>
          <label style="font-size:12px; color:var(--text-muted);">Type</label>
          <select v-model="editType" style="padding:6px 10px; border:1px solid var(--border); border-radius:4px;">
            <option value="reference">reference</option>
            <option value="feedback">feedback</option>
            <option value="project">project</option>
          </select>
        </div>
      </div>
      <textarea v-model="editContent" rows="16"
        style="width:100%; padding:10px; border:1px solid var(--border); border-radius:4px; font-family:monospace; font-size:13px;"></textarea>
      <div style="margin-top:12px; display:flex; gap:8px;">
        <button class="btn btn-primary" @click="saveEdit">Save</button>
        <button class="btn" @click="cancelEdit">Cancel</button>
      </div>
    </div>

    <!-- Memory list -->
    <div v-if="!loading && memories.length === 0 && !createMode" class="empty">No memories found</div>

    <template v-for="(list, dirName) in grouped" :key="dirName">
      <h3 v-if="!projectName" style="font-size:14px; margin:16px 0 8px; color:var(--text-muted);">
        {{ projectLabel(dirName) }}
      </h3>
      <div v-for="mem in list" :key="mem.fileName" class="card" style="cursor:pointer;" @click="openEdit(mem)">
        <div style="display:flex; justify-content:space-between; align-items:start;">
          <div style="flex:1;">
            <div style="font-weight:600; margin-bottom:4px;">
              {{ mem.name }}
              <span class="badge" :class="typeColors[mem.type] || 'badge-info'" style="margin-left:8px;">{{ mem.type }}</span>
            </div>
            <div v-if="mem.description" style="font-size:12px; color:var(--text-muted); margin-bottom:6px;">{{ mem.description }}</div>
            <div class="markdown-body" v-html="renderMarkdown(mem.content?.substring(0, 200) + (mem.content && mem.content.length > 200 ? '...' : ''))"></div>
          </div>
          <button class="btn" @click.stop="deleteMemory(mem)" style="margin-left:8px; color:#e04040; border-color:#e04040;">Delete</button>
        </div>
      </div>
    </template>
  </div>
</template>
