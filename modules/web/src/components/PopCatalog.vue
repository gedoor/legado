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
      :extra-props="{ gotoChapter, currentChapterIndex }"
    />
  </div>
</template>

<script setup lang="ts">
import VirtualList from 'vue3-virtual-scroll-list'
import settings from '../config/themeConfig'
import '../assets/fonts/popfont.css'
import CatalogItem from './CatalogItem.vue'
import type { BookChapter } from '@/book'

const store = useBookStore()

const { catalog, popCataVisible, miniInterface } = storeToRefs(store)

//主题
const isNight = computed(() => store.theme)
const theme = computed(() => store.theme)
const popupTheme = computed(() => {
  return {
    background: settings.themes[theme.value].popup,
  }
})

//虚拟列表 数据源
const virtualListdata = computed(() => {
  const catalogValue = catalog.value
  if (miniInterface.value) return catalogValue

  // pc端 virtualListIitem有2个章节
  const length = Math.ceil(catalogValue.length / 2)
  const virtualListDataSource = new Array<{
    index: number
    catas: BookChapter[]
  }>(length)

  let i = 0
  while (i < length) {
    virtualListDataSource[i] = {
      index: i,
      catas: catalogValue.slice(2 * i, 2 * i + 2),
    }
    i++
  }
  return virtualListDataSource
})

//打开目录 计算当前章节对应的虚拟列表位置
const virtualListRef = ref()
const currentChapterIndex = computed({
  get: () => store.readingBook.chapterIndex,
  set: value => (store.readingBook.chapterIndex = value),
})
const virtualListIndex = computed(() => {
  const index = currentChapterIndex.value
  if (miniInterface.value) return index
  // pc端 virtualListIitem有2个章节
  return Math.floor(index / 2)
})
onUpdated(() => {
  // dom更新触发ResizeObserver，更新虚拟列表内部的sizes Map
  if (!popCataVisible.value) return
  virtualListRef.value.scrollToIndex(virtualListIndex.value)
})

// 点击加载对应章节内容
const emit = defineEmits(['getContent'])
const gotoChapter = (chapter: BookChapter) => {
  const chapterIndex = catalog.value.indexOf(chapter)
  currentChapterIndex.value = chapterIndex
  store.setPopCataVisible(false)
  store.setContentLoading(true)
  store.saveBookProgress()
  emit('getContent', chapterIndex)
}
</script>

<style lang="scss" scoped>
.cata-wrapper {
  margin: -16px;
  padding: 18px 0 24px 25px;

  /* background: #ede7da url('../assets/imgs/themes/popup_1.png') repeat; */
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
      /*width: 50%;*/
      height: 40px;
      cursor: pointer;
      font:
        16px / 40px PingFangSC-Regular,
        HelveticaNeue-Light,
        'Helvetica Neue Light',
        'Microsoft YaHei',
        sans-serif;
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
