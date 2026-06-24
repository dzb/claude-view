<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

function isActive(path: string) {
  return route.path.startsWith(path)
}
</script>

<template>
  <div class="app">
    <nav class="sidebar">
      <div class="logo">
        <span class="logo-icon">◈</span> Claude View
      </div>
      <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
        <span class="nav-icon">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1" y="1" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/><rect x="9" y="1" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/><rect x="1" y="9" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/><rect x="9" y="9" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.5"/></svg>
        </span>
        Dashboard
      </router-link>
      <router-link to="/highlights" class="nav-item" :class="{ active: isActive('/highlights') }">
        <span class="nav-icon">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 1l2.5 5 5.5.8-4 3.9.9 5.3L8 13.5 3.1 16l.9-5.3-4-3.9 5.5-.8L8 1z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
        </span>
        Highlights
      </router-link>
      <router-link to="/memories" class="nav-item" :class="{ active: isActive('/memories') }">
        <span class="nav-icon">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M3 2h7a2 2 0 012 2v8a2 2 0 01-2 2H3a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="currentColor" stroke-width="1.5"/><path d="M5 5h3M5 8h4M5 11h2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        </span>
        Memories
      </router-link>
      <router-link to="/history" class="nav-item" :class="{ active: isActive('/history') }">
        <span class="nav-icon">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 4.5V8l2.5 1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </span>
        History
      </router-link>
    </nav>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style>
:root {
  --bg: #f8f9fa;
  --sidebar-bg: #2d3139;
  --sidebar-text: #9da1a8;
  --sidebar-active: #f4f4f5;
  --sidebar-accent: #f97316;
  --sidebar-hover: rgba(249, 115, 22, 0.06);
  --card-bg: #ffffff;
  --border: #e9ecef;
  --text: #212529;
  --text-muted: #6c757d;
  --primary: #4f6ef7;
  --primary-light: #eef0ff;
  --success: #20c997;
  --warning: #f6c343;
}

.app {
  display: flex;
  height: 100vh;
  background: var(--bg);
}

.sidebar {
  width: 230px;
  background: linear-gradient(165deg, #2d3139 0%, #2a2e35 60%, #262a30 100%);
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.logo {
  color: #d4d6da;
  font-size: 17px;
  font-weight: 700;
  padding: 0 20px 28px;
  letter-spacing: 0.3px;
}
.logo-icon {
  display: inline-block;
  margin-right: 6px;
  font-size: 20px;
  color: var(--sidebar-accent);
}

.nav-item {
  color: var(--sidebar-text);
  text-decoration: none;
  padding: 11px 20px;
  font-size: 13.5px;
  transition: all 0.2s;
  border-left: 3px solid transparent;
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-icon {
  display: inline-flex;
  align-items: center;
  opacity: 0.45;
  transition: opacity 0.2s;
}
.nav-item:hover .nav-icon,
.nav-item.active .nav-icon {
  opacity: 1;
}

.nav-item:hover {
  color: var(--sidebar-accent);
  background: rgba(249, 115, 22, 0.08);
}

.nav-item.active {
  color: var(--sidebar-accent);
  background: rgba(249, 115, 22, 0.1);
  border-left-color: var(--sidebar-accent);
  font-weight: 600;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
}

.card {
  background: var(--card-bg);
  border-radius: 10px;
  border: 1px solid var(--border);
  padding: 20px;
  margin-bottom: 16px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--card-bg);
  color: var(--text);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn:hover { background: var(--bg); }
.btn-primary {
  /* same as .btn, kept as alias */
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.badge-success { background: #d3f9e8; color: #0d6e4a; }
.badge-warning { background: #fef3d0; color: #8a6300; }
.badge-info { background: var(--primary-light); color: var(--primary); }

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--text);
}

.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.empty {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

/* ---- Markdown body (global, available for all views) ---- */
.markdown-body {
  font-size: 14px;
  line-height: 1.7;
  color: #24292e;
}
.markdown-body h1, .markdown-body h2, .markdown-body h3,
.markdown-body h4, .markdown-body h5, .markdown-body h6 {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.4;
}
.markdown-body h1 { font-size: 1.4em; border-bottom: 1px solid #e9ecef; padding-bottom: 6px; }
.markdown-body h2 { font-size: 1.25em; border-bottom: 1px solid #e9ecef; padding-bottom: 5px; }
.markdown-body h3 { font-size: 1.1em; }
.markdown-body p { margin-bottom: 10px; }
.markdown-body a { color: var(--primary); text-decoration: none; }
.markdown-body a:hover { text-decoration: underline; }
.markdown-body code {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12.5px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
}
.markdown-body pre {
  background: #f6f8fa;
  padding: 14px;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 12px;
  border: 1px solid #e9ecef;
}
.markdown-body pre code {
  background: none;
  padding: 0;
  font-size: 12.5px;
}
.markdown-body ul, .markdown-body ol { padding-left: 22px; margin-bottom: 10px; }
.markdown-body li { margin-bottom: 3px; }
.markdown-body blockquote {
  border-left: 3px solid var(--primary);
  padding: 6px 14px;
  color: #57606a;
  margin: 10px 0;
  background: #f8f9fa;
  border-radius: 0 4px 4px 0;
}
.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}
.markdown-body table-wrapper,
.markdown-body .table-wrapper {
  display: block;
  width: 100%;
  overflow-x: auto;
}
.markdown-body table th,
.markdown-body table td {
  padding: 8px 13px;
  border: 1px solid #d0d7de;
  text-align: left;
  font-size: 13px;
}
.markdown-body table th { font-weight: 600; background: #f6f8fa; }
.markdown-body table tr:nth-child(even) { background: #f8f9fa; }
.markdown-body table tr:hover { background: #eef1f5; }
.markdown-body strong { font-weight: 600; }
.markdown-body img { max-width: 100%; border-radius: 4px; }
.markdown-body hr { border: none; border-top: 1px solid #e9ecef; margin: 16px 0; }
.markdown-body input[type="checkbox"] {
  appearance: none;
  -webkit-appearance: none;
  width: 15px; height: 15px;
  border: 1.5px solid #b0b8c0;
  border-radius: 3px;
  margin-right: 6px;
  vertical-align: -2px;
  cursor: default;
  position: relative;
}
.markdown-body input[type="checkbox"]:checked {
  background: #4f6ef7;
  border-color: #4f6ef7;
}
.markdown-body input[type="checkbox"]:checked::after {
  content: '';
  position: absolute;
  left: 3.5px; top: 1px;
  width: 5px; height: 9px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

/* Flat symbols (✓ ✗ etc.) — pure CSS drawn, no font dependency */
.markdown-body .sym {
  display: inline-block;
  width: 14px;
  height: 14px;
  vertical-align: -2px;
  position: relative;
  margin: 0 1px;
}
.markdown-body .sym-check::after {
  content: '';
  position: absolute;
  left: 2px;
  top: 0px;
  width: 5px;
  height: 9px;
  border: solid #1a7f4b;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.markdown-body .sym-cross::before,
.markdown-body .sym-cross::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 9px;
  height: 1.5px;
  background: #cf222e;
}
.markdown-body .sym-cross::before { transform: translate(-50%, -50%) rotate(45deg); }
.markdown-body .sym-cross::after  { transform: translate(-50%, -50%) rotate(-45deg); }
</style>
