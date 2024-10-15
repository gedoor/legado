<template>
  <el-checkbox
    size="large"
    border
    :value="sourceUrl"
    :class="{
      error: isSaveError,
      edit: sourceUrl == currentSourceUrl,
    }"
  >
    {{ getSourceName(source) }}
    <el-button text :icon="Edit" @click="handleSourceClick(source)" />
  </el-checkbox>
</template>

<script setup lang="ts">
import { Edit } from '@element-plus/icons-vue'
import { getSourceUniqueKey, getSourceName } from '@/utils/souce'
import type { Source } from '@/source'

const props = defineProps<{
  source: Source
}>()

const store = useSourceStore()

const currentSourceUrl = computed(() => store.currentSourceUrl)
const sourceUrl = computed(() => getSourceUniqueKey(props.source))

const handleSourceClick = (source: Source) => {
  store.changeCurrentSource(source)
}
const isSaveError = computed(() => {
  const map = store.savedSourcesMap
  if (map.size == 0) return false
  return !map.has(sourceUrl.value)
})
</script>
<style lang="scss" scoped>
:deep(.el-checkbox__label) {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.error {
  border-color: var(--el-color-error) !important;
  color: var(--el-color-error) !important;
  --el-checkbox-checked-text-color: var(--el-color-error);
  --el-checkbox-checked-bg-color: var(--el-color-error);
  --el-checkbox-checked-input-border-color: var(--el-color-error);
}
.edit {
  border-color: var(--el-color-dark) !important;
}
</style>
