<template>
  <el-input
    v-model="searchKey"
    class="search"
    :prefix-icon="Search"
    placeholder="筛选源"
  />
  <div class="tool">
    <el-button @click="importSourceFile" :icon="Folder">打开</el-button>
    <el-button
      :disabled="sourcesFiltered.length === 0"
      @click="outExport"
      :icon="Download"
    >
      导出</el-button
    >
    <el-button
      type="danger"
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
  <el-checkbox-group id="source-list" v-model="sourceUrlSelect">
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
import API from "@api";
import { Folder, Delete, Download, Search } from "@element-plus/icons-vue";
import {
  isSourceMatches,
  getSourceUniqueKey,
  convertSourcesToMap,
} from "@utils/souce";
import VirtualList from "vue3-virtual-scroll-list";
import SourceItem from "./SourceItem.vue";

const store = useSourceStore();
const sourceUrlSelect = ref([]);
const searchKey = ref("");
const { sources, sourcesMap } = storeToRefs(store);

// 筛选源
/** @type Ref<import('@/source').Source[]> */
const sourcesFiltered = computed(() => {
  const key = searchKey.value;
  if (key === "") return sources.value;
  return (
    sources.value
      // @ts-ignore
      .filter((source) => isSourceMatches(source, key))
  );
});
// 计算当前筛选关键词下的选中源
/** @type Ref<import('@/source').Source[]> */
const sourceSelect = computed(() => {
  const urls = sourceUrlSelect.value;
  if (urls.length == 0) return [];
  const sourcesFilteredMap =
    searchKey.value == ""
      ? sourcesMap.value
      : convertSourcesToMap(sourcesFiltered.value);
  return urls.reduce((sources, sourceUrl) => {
    const source = sourcesFilteredMap.get(sourceUrl);
    if (source) sources.push(source);
    return sources;
  }, []);
});

const deleteSelectSources = () => {
  const sourceSelectValue = sourceSelect.value;
  API.deleteSource(sourceSelectValue).then(({ data }) => {
    if (!data.isSuccess) return ElMessage.error(data.errorMsg);
    store.deleteSources(sourceSelectValue);
    const sourceUrlSelectRawValue = toRaw(sourceUrlSelect.value);
    sourceSelectValue.forEach((source) => {
      const index = sourceUrlSelectRawValue.indexOf(getSourceUniqueKey(source));
      if (index > -1) sourceUrlSelectRawValue.splice(index, 1);
    });
    sourceUrlSelect.value = sourceUrlSelectRawValue;
  });
};
const clearAllSources = () => {
  store.clearAllSource();
  sourceUrlSelect.value = [];
};

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

const isBookSource = /bookSource/.test(window.location.href);
const outExport = () => {
  const exportFile = document.createElement("a");
  let sources =
      sourceUrlSelect.value.length === 0
        ? sourcesFiltered.value
        : sourceSelect.value,
    sourceType = isBookSource ? "BookSource" : "RssSource";

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
