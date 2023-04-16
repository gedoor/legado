<template>
  <el-checkbox
    size="large"
    border
    :label="source"
    :class="{ error: errorPushSources.includes(source) }"
    @change="handleSourceClick(source)"
  >
    {{ source.bookSourceName || source.sourceName }}
  </el-checkbox>
</template>

<script setup>
defineProps(["source"]);
const store = useSourceStore();
const { errorPushSources } = storeToRefs(store);
const handleSourceClick = (source) => {
  store.changeCurrentSource(source);
};
</script>
<style lang="scss" scoped>
.error {
  border-color: var(--el-color-error) !important;
  color: var(--el-color-error) !important;
  --el-checkbox-checked-text-color: var(--el-color-error);
  --el-checkbox-checked-bg-color: var(--el-color-error);
  --el-checkbox-checked-input-border-color: var(--el-color-error);
}
</style>
