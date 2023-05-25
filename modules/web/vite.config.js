import { fileURLToPath, URL } from "node:url";
import { defineConfig, splitVendorChunkPlugin } from "vite";
import vue from "@vitejs/plugin-vue";
import Icons from "unplugin-icons/vite";
import IconsResolver from "unplugin-icons/resolver";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

// https://vitejs.dev/config/
export default ({ mode }) =>
  defineConfig({
    plugins: [
      vue(),
      splitVendorChunkPlugin(), // index vendor打包
      AutoImport({
        imports: ["vue", "vue-router", "pinia"],
        include: [/\.[tj]sx?$/, /\.vue$/, /\.vue\?vue/, /\.md$/],
        dirs: ["src/components", "src/store"],
        eslintrc: {
          enabled: true,
        },
        resolvers: [
          ElementPlusResolver(),
          IconsResolver({
            prefix: "Icon",
          }),
        ],
        dts: "./src/auto-imports.d.ts",
      }),
      Components({
        resolvers: [
          ElementPlusResolver(),
          IconsResolver({
            enabledCollections: ["ep"],
          }),
        ],
        dts: "./src/components.d.ts",
      }),
      Icons({
        autoInstall: true,
      }),
    ],
    base: mode === "development" ? "/" : "./",
    server: {
      port: 8080,
    },
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
        "@api": fileURLToPath(new URL("./src/api", import.meta.url)),
        "@utils": fileURLToPath(new URL("./src/utils/", import.meta.url)),
      },
    },
  });
