<template>
  <div class="wrapper">
    <div
      v-for="cata in catas"
      class="cata-text"
      :key="cata.url"
      :class="{ selected: isSelected(cata.index) }"
      @click="gotoChapter(cata)"
    >
      {{ cata.title }}
    </div>
  </div>
</template>
<script setup>
const props = defineProps(["index", "source", "gotoChapter"]);

const store = useBookStore();

const index = computed(() => store.readingBook.index);

const isSelected = (idx) => {
  return idx == index.value;
};

// 处理pc mobile
const catas = computed(() => {
  return props.source?.catas ?? [props.source];
});
</script>

<style lang="scss" scoped>
.selected {
  color: #eb4259;
}
.wrapper {
  display: flex;

  .cata-text {
    width: 100%;
    margin-right: 26px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}
</style>
