<!--
 Copyright (C) 2023 GIP-RECIA, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<script setup lang="ts">
import { computed, onBeforeMount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

const { t } = useI18n()

const route = useRoute()

const isAdmin = computed<boolean>(() => (
  route.matched.some(r => r.name === 'adminList')
))

// TODO : replace with API CALL
const configuration = ref({
  front: {
    rui: {
      header: {
        componentPath: '/resource-server/webjars/gip-recia__ui-webcomponents/dist/r-header.js',
        props: {
          'template-api-url': '/commun/portal_template_api.tpl.json',
          'navigation-drawer-visible': '',
        },
      },
      footer: {
        componentPath: '/resource-server/webjars/gip-recia__ui-webcomponents/dist/r-footer.js',
        props: {
          'template-api-url': '/commun/portal_template_api.tpl.json',
        },
      },
    },
  },
})

const { header, footer } = configuration.value.front.rui
if (header) {
  const rHeaderScript = document.createElement('script')
  rHeaderScript.setAttribute('src', header.componentPath)
  rHeaderScript.setAttribute('charset', 'utf-8')
  document.head.appendChild(rHeaderScript)
}
if (footer) {
  const rFooterScript = document.createElement('script')
  rFooterScript.setAttribute('src', footer.componentPath)
  rFooterScript.setAttribute('charset', 'utf-8')
  document.head.appendChild(rFooterScript)
}

onBeforeMount(() => {
  document.title = __APP_NAME__
})

watch(
  route,
  (val) => {
    const { name } = val
    const title = ['list', 'adminList'].includes(name as string)
      ? t(`page.${name as string}.h1`)
      : __APP_NAME__

    if (document.title !== title)
      document.title = title
  },
  { immediate: true },
)
</script>

<template>
  <nav
    role="navigation"
    aria-label="Accès rapide"
    class="skip-links"
  >
    <ul>
      <li>
        <a href="#main">Contenu</a>
      </li>
    </ul>
  </nav>
  <header>
    <r-header
      v-if="configuration"
      v-bind="configuration!.front.rui?.header?.props"
      :fname="isAdmin ? 'AdminListesDiffusion' : 'ListesDiffusion'"
    />
  </header>
  <main
    id="main"
    tabindex="-1"
  >
    <router-view />
  </main>
  <footer>
    <r-footer
      v-if="configuration"
      v-bind="configuration!.front.rui?.footer?.props"
    />
  </footer>
</template>
