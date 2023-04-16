<template>
  <el-input
    v-model="searchKey"
    class="search"
    :prefix-icon="Search"
    placeholder="筛选源"
  />
  <div class="tool">
    <el-button @click="importSourceFile" :icon="Folder"> 打开 </el-button>
    <el-button
      :disabled="sourcesFiltered.length === 0"
      @click="outExport"
      :icon="Download"
    >
      导出</el-button
    >
    <el-button
      :icon="Delete"
      @click="deleteSelectSources"
      :disabled="sourceSelect.length === 0"
      >删除</el-button
    >
    <el-button
      type="danger"
      :icon="Delete"
      @click="clearAllSources"
      :disabled="sources.length === 0"
      >清空</el-button
    >
  </div>
  <el-checkbox-group id="source-list" v-model="sourceSelect">
    <virtual-list
      style="height: 100%; overflow-y: auto; overflow-x: hidden"
      :data-key="(source) => source.bookSourceUrl || source.sourceUrl"
      :data-sources="sourcesFiltered"
      :data-component="SourceItem"
      :estimate-size="45"
    />
  </el-checkbox-group>
</template>

<script setup>
import { Folder, Delete, Download, Search } from "@element-plus/icons-vue";
import { isSourceContains } from "../utils/souce";
import VirtualList from "vue3-virtual-scroll-list";
import SourceItem from "./SourceItem.vue";

const store = useSourceStore();
const sourceSelect = ref([]);
const searchKey = ref("");
const { sources } = storeToRefs(store);
const isBookSource = computed(() => {
  return /bookSource/.test(window.location.href);
});
const deleteSelectSources = () => {
  store.deleteSources(sourceSelect.value);
  sourceSelect.value = [];
};
const clearAllSources = () => {
  store.clearAllSource();
  sourceSelect.value = [];
};
//筛选源
const sourcesFiltered = computed(() => {
  let key = searchKey.value;
  if (key === "") return sources.value;
  return (
    sources.value
      // @ts-ignore
      .filter((source) => isSourceContains(source, key))
  );
});

//导入本地文件
const importSourceFile = () => {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".json,.txt";
  input.addEventListener("change", (e) => {
    // @ts-ignore
    const file = e.target.files[0];
    var reader = new FileReader();
    reader.readAsText(file);
    reader.onload = () => {
      try {
        // @ts-ignore
        const jsonData = JSON.parse(reader.result);
        store.saveSources(jsonData);
      } catch {
        ElMessage({
          message: "上传的源格式错误",
          type: "error",
        });
      }
    };
  });
  input.click();
};
const outExport = () => {
  const exportFile = document.createElement("a");
  let sources =
      sourceSelect.value.length === 0
        ? sourcesFiltered.value
        : sourceSelect.value,
    sourceType = isBookSource.value ? "BookSource" : "RssSource";

  exportFile.download = `${sourceType}_${Date()
    .replace(/.*?\s(\d+)\s(\d+)\s(\d+:\d+:\d+).*/, "$2$1$3")
    .replace(/:/g, "")}.json`;

  let myBlob = new Blob([JSON.stringify(sources, null, 4)], {
    type: "application/json",
  });
  exportFile.href = window.URL.createObjectURL(myBlob);
  exportFile.click();
};
</script>

<style lang="scss" scoped>
.tool {
  display: flex;
  margin: 4px 0;
  justify-content: center;
}

#source-list {
  margin-top: 6px;
  height: calc(100vh - 112px - 7px);

  :deep(.el-checkbox) {
    margin-bottom: 4px;
    width: 100%;
  }
}
</style>
