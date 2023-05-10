<template>
  <div class="cata-wrapper" :style="popupTheme">
    <div class="title">目录</div>
    <virtual-list
      style="height: 300px; overflow: auto"
      :class="{ night: isNight, day: !isNight }"
      ref="virtualListRef"
      :data-key="'index'"
      :start="index"
      wrap-class="data-wrapper"
      item-class="cata"
      :data-sources="catalog"
      :data-component="CatalogItem"
      :estimate-size="40"
      :extra-props="{ gotoChapter }"
    />
  </div>
</template>

<script setup>
import VirtualList from "vue3-virtual-scroll-list";
import jump from "../plugins/jump";
import settings from "../plugins/config";
import "../assets/fonts/popfont.css";
import CatalogItem from "./CatalogItem.vue";

const store = useBookStore();

const isNight = computed(() => theme.value == 6);
const { catalog, popCataVisible } = storeToRefs(store);

const theme = computed(() => {
  return store.config.theme;
});
const popupTheme = computed(() => {
  return {
    background: settings.themes[theme.value].popup,
  };
});

const index = computed({
  get: () => store.readingBook.index,
  set: (value) => (store.readingBook.index = value),
});
const virtualListRef = ref();
watch(popCataVisible, (visible) => {
  if (!visible) return;
  nextTick(() => {
    virtualListRef.value.scrollToIndex(index.value);
  });
});

const emit = defineEmits(["getContent"]);
const gotoChapter = (note) => {
  index.value = catalog.value.indexOf(note);
  store.setPopCataVisible(false);
  store.setContentLoading(true);
  emit("getContent", index.value);
};
</script>

<style lang="scss" scoped>
.cata-wrapper {
  margin: -16px;
  padding: 18px 0 24px 25px;

  // background: #ede7da url('../assets/imgs/themes/popup_1.png') repeat;
  .title {
    font-size: 18px;
    font-weight: 400;
    font-family: FZZCYSK;
    margin: 0 0 20px 0;
    color: #ed4259;
    width: fit-content;
    border-bottom: 1px solid #ed4259;
  }
  :deep(.data-wrapper) {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: space-between;
    .cata {
      width: 50%;
      height: 40px;
      cursor: pointer;
      font: 16px / 40px PingFangSC-Regular, HelveticaNeue-Light,
        "Helvetica Neue Light", "Microsoft YaHei", sans-serif;
    }
  }

  .night {
    :deep(.cata) {
      border-bottom: 1px solid #666;
    }
  }

  .day {
    :deep(.cata) {
      border-bottom: 1px solid #f2f2f2;
    }
  }
}
@media screen and (max-width: 500px) {
  :deep(.cata) {
    width: 100% !important;
  }
}
</style>
