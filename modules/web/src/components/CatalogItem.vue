<template>
  <div
    class="cata-text"
    :class="{ selected: isSelected(source.index) }"
    @click="gotoChapter(source)"
  >
    {{ source.title }}
  </div>
</template>
<script setup>
defineProps(["index", "source", "gotoChapter"]);

const store = useBookStore();

const index = computed(() => store.readingBook.index);

const isSelected = (idx) => {
  return idx == index.value;
};
</script>

<style lang="scss" scoped>
.selected {
  color: #eb4259;
}

.cata-text {
  margin-right: 26px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
