<template>
  <div class="cata-wrapper" :style="popupTheme">
    <div class="title">目录</div>
    <div
      class="data-wrapper"
      ref="cataData"
      :class="{ night: isNight, day: !isNight }"
    >
      <div class="cata">
        <div
          class="log"
          v-for="(note, index) in catalog"
          :class="{ selected: isSelected(index) }"
          :key="note.durChapterIndex"
          @click="gotoChapter(note)"
          ref="cata"
        >
          <div class="log-text">
            {{ note.title }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import jump from "../plugins/jump";
import settings from "../plugins/config";
import "../assets/fonts/popfont.css";
const store = useBookStore();

const isNight = ref(false);
const { index } = toRefs(store.readingBook);
const { catalog, popCataVisible } = storeToRefs(store);

const theme = computed(() => {
  return store.config.theme;
});

const popupTheme = computed(() => {
  return {
    background: settings.themes[theme.value].popup,
  };
});
watchEffect(() => {
  isNight.value = theme.value == 6;
});

const cata = ref();
const cataData = ref();
watch(popCataVisible, () => {
  nextTick(() => {
    let wrapper = cataData.value;
    jump(cata.value[index.value], { container: wrapper, duration: 0 });
  });
});

const isSelected = (idx) => {
  return idx == index.value;
};
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

  .data-wrapper {
    height: 300px;
    overflow: auto;

    .cata {
      display: flex;
      flex-direction: row;
      flex-wrap: wrap;
      justify-content: space-between;

      .selected {
        color: #eb4259;
      }

      .log {
        width: 50%;
        height: 40px;
        cursor: pointer;
        float: left;
        font: 16px / 40px PingFangSC-Regular, HelveticaNeue-Light,
          "Helvetica Neue Light", "Microsoft YaHei", sans-serif;

        .log-text {
          margin-right: 26px;
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
        }
      }
    }
  }

  .night {
    :deep(.log) {
      border-bottom: 1px solid #666;
    }
  }

  .day {
    :deep(.log) {
      border-bottom: 1px solid #f2f2f2;
    }
  }
}

@media screen and (max-width: 500px) {
  .cata-wrapper .data-wrapper .cata .log {
    width: 100%;
  }
}
</style>
