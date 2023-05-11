<template>
  <div
    :class="{ 'cata-wrapper': true, visible: popCataVisible }"
    :style="popupTheme"
  >
    <div class="title">目录</div>
    <virtual-list
      style="height: 300px; overflow: auto"
      :class="{ night: isNight, day: !isNight }"
      ref="virtualListRef"
      data-key="index"
      wrap-class="data-wrapper"
      item-class="cata"
      :data-sources="virtualListdata"
      :data-component="CatalogItem"
      :estimate-size="40"
      :extra-props="{ gotoChapter }"
    />
  </div>
</template>

<script setup>
import VirtualList from "vue3-virtual-scroll-list";
import settings from "../plugins/config";
import "../assets/fonts/popfont.css";
import CatalogItem from "./CatalogItem.vue";

const store = useBookStore();

const isNight = computed(() => theme.value == 6);
const { catalog, popCataVisible, miniInterface } = storeToRefs(store);

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

const virtualListdata = computed(() => {
  let catalogValue = catalog.value;
  if (miniInterface.value) return catalogValue;

  // pc端 virtualListIitem有2个章节
  let length = Math.ceil(catalogValue.length / 2);
  let virtualListDataSource = new Array(length);

  let i = 0;
  while (i < length) {
    virtualListDataSource[i] = {
      index: i,
      catas: catalogValue.slice(2 * i, 2 * i + 2),
    };
    i++;
  }
  return virtualListDataSource;
});

const emit = defineEmits(["getContent"]);
const gotoChapter = (note) => {
  index.value = catalog.value.indexOf(note);
  store.setPopCataVisible(false);
  store.setContentLoading(true);
  emit("getContent", index.value);
};

const virtualListRef = ref();
const virtualListIndex = computed(() => {
  if (miniInterface.value) return index.value;
  return Math.floor(index.value / 2);
});

onUpdated(() => {
  // dom更新触发ResizeObserver，更新虚拟列表内部的sizes Map
  virtualListRef.value.scrollToIndex(virtualListIndex.value);
});
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
    .cata {
      //width: 50%;
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
</style>
