/**
 * Copyright (C) 2023 GIP-RECIA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { createRouter, createWebHistory } from 'vue-router'

const isDev = import.meta.env.DEV

const devRoutes: never[] = []

const prodRoutes: never[] = []

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'list',
      component: () => import('@/views/ListView.vue'),
    },
    {
      path: '/admin',
      name: 'adminList',
      component: () => import('@/views/AdminListView.vue'),
    },
    {
      path: '/:pathName(.*)',
      redirect: () => {
        return { name: 'list' }
      },
    },
    ...(
      isDev
        ? devRoutes
        : prodRoutes
    ),
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    else if (to.fullPath === from.fullPath) {
      return {}
    }
    else {
      return {
        top: 0,
        // behavior: 'smooth',
      }
    }
  },
})

export default router
