<template>
  <el-input
    v-if="isBookSource"
    id="debug-key"
    v-model="searchKey"
    placeholder="搜索书名、作者"
    :prefix-icon="Search"
    style="padding-bottom: 4px"
    @keydown.enter="startDebug"
  />
  <el-input
    id="debug-text"
    v-model="printDebug"
    type="textarea"
    readonly
    rows="29"
    placeholder="这里用于输出调试信息"
  />
</template>

<script setup>
import API from "@api";
import { Search } from "@element-plus/icons-vue";

const store = useSourceStore();

const printDebug = ref("");
const searchKey = ref("");

watchEffect(() => {
  if (store.isDebuging) startDebug();
});

const appendDebugMsg = (msg) => {
  let debugDom = document.querySelector("#debug-text");
  debugDom.scrollTop = debugDom.scrollHeight;
  printDebug.value += msg + "\n";
};
const startDebug = async () => {
  printDebug.value = "";
  await API.saveSource(store.currentSource);
  API.debug(
    store.currentSourceUrl,
    searchKey.value || store.searchKey,
    appendDebugMsg,
    store.debugFinish
  );
};

const isBookSource = computed(() => {
  return /bookSource/.test(window.location.href);
});
</script>

<style lang="scss" scoped></style>
