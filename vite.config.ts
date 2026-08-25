/* eslint-disable node/prefer-global/process */
import type { ConfigEnv } from 'vite'
import { readFileSync } from 'node:fs'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import { parseString } from 'xml2js'

// https://vitejs.dev/config/
export default ({ mode }: ConfigEnv) => {
  const env = loadEnv(mode, process.cwd())

  const {
    VITE_BASE_URI,
    VITE_ALLOWED_HOSTS,
    VITE_PROXY_API_URL,
  } = env

  const backVersion = (): string => {
    let version
    const pomXml = readFileSync('./pom.xml', 'utf8')
    parseString(pomXml, (err, result) => {
      if (err)
        console.error(err)
      else version = result.project.version[0]
    })

    return JSON.stringify(version)
  }

  return defineConfig({
    base: `${VITE_BASE_URI}/ui`,
    root: './src/main/webapp',
    envDir: '../../../',
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src/main/webapp/src', import.meta.url)),
      },
    },
    server: {
      allowedHosts: JSON.parse(VITE_ALLOWED_HOSTS ?? '[]'),
      proxy: {
        '^(?:/[^/]*)?/(?=api|app)': {
          target: VITE_PROXY_API_URL,
          changeOrigin: true,
        },
      },
    },
    build: {
      sourcemap: true,
      rollupOptions: {
        input: {
          index: './index.html',
          admin: './admin.html',
        },
        external: [
          /\/commun\/.*/,
        ],
      },
    },
    define: {
      __BACK_VERSION__: backVersion(),
    },
  })
}
