<template>
  <div class="editor">
    <source-tab-form class="left" :config="config" />
    <tool-bar />
    <source-tab-tools class="right" />
  </div>
</template>
<script setup>
import bookSourceConfig from "@/utils/bookSourceEditConfig.js";
import rssSourceConfig from "@/utils/rssSourceEditConfig.js";
import "@/assets/main.css";

const config = ref({});

if (/bookSource/i.test(location.href)) {
  config.value = bookSourceConfig;
  document.title = "书源管理";
} else {
  config.value = rssSourceConfig;
  document.title = "订阅源管理";
}
</script>
<style lang="scss" scoped>
.editor {
  display: flex;
  height: 100vh;
  overflow: hidden;
  .left {
    flex: 1;
    margin-left: 20px;
  }
  .right {
    width: 360px;
    margin-right: 20px;
  }
  #loading {
    position: fixed;
    top: 100px;
    left: 90vw;
  }
}
</style>
