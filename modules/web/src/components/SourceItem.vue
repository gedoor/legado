<template>
  <el-checkbox
    size="large"
    border
    :label="source"
    :class="{ error: errorPushSources.includes(source) }"
    @change="handleSourceClick(source)"
    :key="source.bookSourceUrl"
  >
    {{ source.bookSourceName || source.sourceName }}
  </el-checkbox>
</template>

<script setup>
const { source } = defineProps(['source'])
const store = useSourceStore()
const { errorPushSources } = storeToRefs(store)
const handleSourceClick = source => {
  store.changeCurrentSource(source)
}
</script>
