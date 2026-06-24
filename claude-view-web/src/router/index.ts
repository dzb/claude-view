import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('@/views/Dashboard.vue'),
    },
    {
      path: '/conversations/:projectName',
      name: 'conversations',
      component: () => import('@/views/Conversations.vue'),
    },
    {
      path: '/conversation/:projectName/:sessionId',
      name: 'conversation-detail',
      component: () => import('@/views/ConversationDetail.vue'),
    },
    {
      path: '/highlights',
      name: 'highlights',
      component: () => import('@/views/Highlights.vue'),
    },
    {
      path: '/memories',
      name: 'memories',
      component: () => import('@/views/Memories.vue'),
    },
    {
      path: '/memories/:projectName',
      name: 'project-memories',
      component: () => import('@/views/Memories.vue'),
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/views/History.vue'),
    },
  ],
})

export default router
