<template>
  <div
    class="chapter-wrapper"
    :style="bodyTheme"
    :class="{ night: isNight, day: !isNight }"
    @click="showToolBar = !showToolBar"
  >
    <div class="tool-bar" :style="leftBarTheme">
      <div class="tools">
        <el-popover
          placement="right"
          :width="popupWidth"
          trigger="click"
          :show-arrow="false"
          v-model:visible="popCataVisible"
          popper-class="pop-cata"
        >
          <PopCatalog @getContent="getContent" class="popup" />
          <template #reference>
            <div class="tool-icon" :class="{ 'no-point': noPoint }">
              <div class="iconfont">&#58905;</div>
              <div class="icon-text">目录</div>
            </div>
          </template>
        </el-popover>
        <el-popover
          placement="right"
          :width="popupWidth"
          trigger="click"
          :show-arrow="false"
          v-model:visible="readSettingsVisible"
          popper-class="pop-setting"
        >
          <read-settings class="popup" />
          <template #reference>
            <div class="tool-icon" :class="{ 'no-point': noPoint }">
              <div class="iconfont">&#58971;</div>
              <div class="icon-text">设置</div>
            </div>
          </template>
        </el-popover>
        <div class="tool-icon" @click="toShelf">
          <div class="iconfont">&#58892;</div>
          <div class="icon-text">书架</div>
        </div>
        <div class="tool-icon" :class="{ 'no-point': noPoint }" @click="toTop">
          <div class="iconfont">&#58914;</div>
          <div class="icon-text">顶部</div>
        </div>
        <div
          class="tool-icon"
          :class="{ 'no-point': noPoint }"
          @click="toBottom"
        >
          <div class="iconfont">&#58915;</div>
          <div class="icon-text">底部</div>
        </div>
      </div>
    </div>
    <div class="read-bar" :style="rightBarTheme">
      <div class="tools">
        <div
          class="tool-icon"
          :class="{ 'no-point': noPoint }"
          @click="toPreChapter"
        >
          <div class="iconfont">&#58920;</div>
          <span v-if="miniInterface">上一章</span>
        </div>
        <div
          class="tool-icon"
          :class="{ 'no-point': noPoint }"
          @click="toNextChapter"
        >
          <span v-if="miniInterface">下一章</span>
          <div class="iconfont">&#58913;</div>
        </div>
      </div>
    </div>
    <div class="chapter-bar"></div>
    <div class="chapter" ref="content" :style="chapterTheme">
      <div class="content">
        <div class="top-bar" ref="top"></div>
        <div v-for="data in chapterData" :key="data.index" ref="chapter">
          <div class="title" :index="data.index" v-if="showContent">
            {{ data.title }}
          </div>
          <chapter-content :carray="data.content" v-if="showContent" />
        </div>
        <div class="loading" ref="loading"></div>
        <div class="bottom-bar" ref="bottom"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import jump from "@/plugins/jump";
import settings from "@/plugins/config";
import API from "@api";
import loadingSvg from "@element-plus/icons-svg/loading.svg?raw";

const showLoading = ref(false);
const loadingSerive = ref(null);
const content = ref();

watch(showLoading, (loading) => {
  if (!loading) return loadingSerive.value?.close();
  loadingSerive.value = ElLoading.service({
    target: content.value,
    spinner: loadingSvg,
    text: "正在获取信息",
    lock: true,
  });
});

const store = useBookStore();
// 读取阅读配置
try {
  const browerConfig = JSON.parse(localStorage.getItem("config"));
  if (browerConfig != null) store.setConfig(browerConfig);
} catch {
  localStorage.removeItem("config");
}
const loading = ref();

const noPoint = ref(true);

const showToolBar = ref(false);
const chapterData = ref([]);
const scrollObserve = ref(null);
const readingObserve = ref(null);

const {
  catalog,
  popCataVisible,
  readSettingsVisible,
  miniInterface,
  showContent,
} = storeToRefs(store);

const { chapterPos, index: chapterIndex } = toRefs(store.readingBook);

const { theme, infiniteLoading } = toRefs(store.config);

// 主题部分
const bodyColor = computed(() => settings.themes[theme.value].body);
const chapterColor = computed(() => settings.themes[theme.value].content);
const popupColor = computed(() => settings.themes[theme.value].popup);

const readWidth = computed(() => {
  if (!miniInterface.value) {
    return store.config.readWidth - 130 + "px";
  } else {
    return window.innerWidth + "px";
  }
});
const popupWidth = computed(() => {
  if (!miniInterface.value) {
    return store.config.readWidth - 33;
  } else {
    return window.innerWidth - 33;
  }
});
const bodyTheme = computed(() => {
  return {
    background: settings.themes[theme.value].body,
  };
});
const chapterTheme = computed(() => {
  return {
    background: settings.themes[theme.value].content,
    width: readWidth.value,
  };
});
const leftBarTheme = computed(() => {
  return {
    background: settings.themes[theme.value].popup,
    marginLeft: miniInterface.value
      ? 0
      : -(store.config.readWidth / 2 + 68) + "px",
    display: miniInterface.value && !showToolBar.value ? "none" : "block",
  };
});
const rightBarTheme = computed(() => {
  return {
    background: settings.themes[theme.value].popup,
    marginRight: miniInterface.value
      ? 0
      : -(store.config.readWidth / 2 + 52) + "px",
    display: miniInterface.value && !showToolBar.value ? "none" : "block",
  };
});
const isNight = ref(false);
watchEffect(() => {
  isNight.value = theme.value == 6;
});
watch(bodyColor, (color) => {
  bodyTheme.value.background = color;
});
watch(chapterColor, (color) => {
  chapterTheme.value.background = color;
});
watch(readWidth, (width) => {
  chapterTheme.value.width = width;
  let leftToolMargin = -((parseInt(width) + 130) / 2 + 68) + "px";
  let rightToolMargin = -((parseInt(width) + 130) / 2 + 52) + "px";
  leftBarTheme.value.marginLeft = leftToolMargin;
  rightBarTheme.value.marginRight = rightToolMargin;
});
watch(popupColor, (color) => {
  leftBarTheme.value.background = color;
  rightBarTheme.value.background = color;
});

watchEffect(() => {
  if (chapterData.value.length > 0) {
    store.setContentLoading(false);
    //添加章节内容到observe
    addReadingObserve();
  }
});

watchEffect(() => {
  document.title = catalog.value[chapterIndex.value]?.title || document.title;
  store.saveBookProcess();
});

watchEffect(() => {
  if (!infiniteLoading.value) {
    scrollObserve.value?.disconnect();
  } else {
    scrollObserve.value?.observe(loading.value);
  }
});

const top = ref();
const getContent = (index, reloadChapter = true, chapterPos = 0) => {
  if (reloadChapter) {
    //展示进度条
    store.setShowContent(false);
    showLoading.value = true;
    //强制滚回顶层
    jump(top.value, { duration: 0 });
    //从目录，按钮切换章节时保存进度 预加载时不保存
    saveReadingBookProgressToBrowser(index, chapterPos);
  }
  let bookUrl = sessionStorage.getItem("bookUrl");
  let { title, index: chapterIndex } = catalog.value[index];

  API.getBookContent(bookUrl, chapterIndex).then(
    (res) => {
      if (res.data.isSuccess) {
        let data = res.data.data;
        let content = data.split(/\n+/);
        updateChapterData({ index, content, title }, reloadChapter);
      } else {
        ElMessage({ message: res.data.errorMsg, type: "error" });
        let content = [res.data.errorMsg];
        updateChapterData({ index, content, title }, reloadChapter);
      }
      store.setContentLoading(true);
      showLoading.value = false;
      noPoint.value = false;
      store.setShowContent(true);
      if (!res.data.isSuccess) {
        throw res.data;
      }
    },
    (err) => {
      ElMessage({ message: "获取章节内容失败", type: "error" });
      let content = ["获取章节内容失败！"];
      updateChapterData({ index, content, title }, reloadChapter);
      showLoading.value = false;
      store.setShowContent(true);
      throw err;
    }
  );
};
const chapter = ref();
const toChapterPos = (chapterPos) => {
  if (!chapterPos) return;
  nextTick(() => {
    //计算chapterPos对应的段落行数
    let wordCount = 0;
    let index = chapterData.value[0].content.findIndex((paragraph) => {
      wordCount += paragraph.length;
      return wordCount >= chapterPos.value;
    });
    if (index == -1) index = chapterData.value[0].content.length - 1;
    if (index == 0) return; //第一行不跳转
    //跳转
    jump(chapter.value[0].children[1].children[index], {
      duration: 0,
      callback: () => (chapterPos.value = 0),
    });
  });
};
//计算当前章节阅读的字数
const computeChapterPos = () => {
  //dom没渲染时 返回0
  if (!chapter.value[0]) return;
  //计算当前阅读进度对应的element
  let index = chapterData.value.findIndex(
    (chapter) => chapter.index == chapterIndex.value
  );
  if (index == -1) return;
  let element = chapter.value[index].children[1].children;
  //计算已读字数
  let mChapterPos = 0;
  for (let paragraph of element) {
    let text = paragraph.innerText;
    mChapterPos += text.length;
    if (paragraph.getBoundingClientRect().top >= 0) {
      chapterPos.value = mChapterPos;
      break;
    }
  }
};
const bottom = ref();
const toTop = () => {
  jump(top.value);
};
const toBottom = () => {
  jump(bottom.value);
};
const toNextChapter = () => {
  store.setContentLoading(true);
  let index = chapterIndex.value + 1;
  if (typeof catalog.value[index] !== "undefined") {
    ElMessage({
      message: "下一章",
      type: "info",
    });
    getContent(index);
  } else {
    ElMessage({
      message: "本章是最后一章",
      type: "error",
    });
  }
};
const toPreChapter = () => {
  store.setContentLoading(true);
  let index = chapterIndex.value - 1;
  if (typeof catalog.value[index] !== "undefined") {
    ElMessage({
      message: "上一章",
      type: "info",
    });
    getContent(index);
  } else {
    ElMessage({
      message: "本章是第一章",
      type: "error",
    });
  }
};
const saveReadingBookProgressToBrowser = (index, pos = chapterPos.value) => {
  //保存localStorage
  let bookUrl = sessionStorage.getItem("bookUrl");
  var book = JSON.parse(localStorage.getItem(bookUrl));
  book.index = index;
  book.chapterPos = pos;
  localStorage.setItem(bookUrl, JSON.stringify(book));
  //最近阅读
  book = JSON.parse(localStorage.getItem("readingRecent"));
  book.chapterIndex = index;
  book.chapterPos = pos;
  localStorage.setItem("readingRecent", JSON.stringify(book));
  //保存vuex
  chapterIndex.value = index;
  chapterPos.value = pos;
  //保存sessionStorage
  sessionStorage.setItem("chapterIndex", index);
  sessionStorage.setItem("chapterPos", String(pos));
};
const updateChapterData = async (data, reloadChapter) => {
  if (reloadChapter) {
    chapterData.value.splice(0);
  }
  chapterData.value.push(data);
};
const loadMore = () => {
  let index = chapterData.value.slice(-1)[0].index;
  if (catalog.value.length - 1 > index) {
    getContent(index + 1, false);
  }
};
const router = useRouter();
const toShelf = () => {
  router.push("/");
};
//监听方向键
const handleKeyPress = (event) => {
  switch (event.key) {
    case "ArrowLeft":
      event.stopPropagation();
      event.preventDefault();
      toPreChapter();
      break;
    case "ArrowRight":
      event.stopPropagation();
      event.preventDefault();
      toNextChapter();
      break;
    case "ArrowUp":
      event.stopPropagation();
      event.preventDefault();
      if (document.documentElement.scrollTop === 0) {
        ElMessage({
          message: "已到达页面顶部",
          type: "warn",
        });
      } else {
        jump(0 - document.documentElement.clientHeight + 100);
      }
      break;
    case "ArrowDown":
      event.stopPropagation();
      event.preventDefault();
      if (
        document.documentElement.clientHeight +
          document.documentElement.scrollTop ===
        document.documentElement.scrollHeight
      ) {
        ElMessage({
          message: "已到达页面底部",
          type: "warn",
        });
      } else {
        jump(document.documentElement.clientHeight - 100);
      }
      break;
  }
};
//IntersectionObserver回调 底部加载
const handleIScrollObserve = (entries) => {
  if (showLoading.value) return;
  for (let { isIntersecting } of entries) {
    if (!isIntersecting) return;
    loadMore();
  }
};
//IntersectionObserver回调 当前阅读章节序号
const handleIReadingObserve = (entries) => {
  nextTick(() => {
    for (let { isIntersecting, target, boundingClientRect } of entries) {
      let titleElement = target.querySelector(".title");
      if (!titleElement) return;
      let chapterTitleIndex = parseInt(titleElement.getAttribute("index"));
      if (isIntersecting) {
        chapterIndex.value = chapterTitleIndex;
      } else {
        if (boundingClientRect.top < 0) {
          chapterIndex.value = chapterTitleIndex + 1;
        } else {
          chapterIndex.value = chapterTitleIndex - 1;
        }
      }
    }
  });
};
//添加所有章节到observe
const addReadingObserve = () => {
  nextTick(() => {
    let chapterElements = chapter.value;
    if (!chapterElements) return;
    chapterElements.forEach((el) => readingObserve.value.observe(el));
  });
};
/*
onBeforeRouteLeave((to, from, next) => {
  if (
    store.searchBooks.every((book) => book.bookUrl != store.readingBook.bookUrl)
  ) {
    next();
  } else {
    alert(111);
    next(false);
  }
});
window.addEventListener("beforeunload", (e) => {
  e.preventDefault();
  e.returnValue = "";
  alert(111);
});
*/
onMounted(() => {
  showLoading.value = true;
  //获取书籍数据
  let bookUrl = sessionStorage.getItem("bookUrl");
  let bookName = sessionStorage.getItem("bookName");
  let bookAuthor = sessionStorage.getItem("bookAuthor");
  let chapterIndex = Number(sessionStorage.getItem("chapterIndex") || 0);
  let chapterPos = Number(sessionStorage.getItem("chapterPos") || 0);
  var book = JSON.parse(localStorage.getItem(bookUrl));
  if (
    book == null ||
    chapterIndex != book.index ||
    chapterPos != book.chapterPos
  ) {
    book = {
      bookName: bookName,
      bookAuthor: bookAuthor,
      bookUrl: bookUrl,
      index: chapterIndex,
      chapterPos: chapterPos,
    };
    localStorage.setItem(bookUrl, JSON.stringify(book));
  }

  API.getChapterList(bookUrl).then(
    (res) => {
      showLoading.value = false;
      if (!res.data.isSuccess) {
        ElMessage({ message: res.data.errorMsg, type: "error" });
        setTimeout(toShelf, 500);
        return;
      }
      let data = res.data.data;
      store.setCatalog(data);
      store.setReadingBook(book);

      getContent(chapterIndex, true, chapterPos);

      window.addEventListener("keyup", handleKeyPress);
      //监听底部加载
      scrollObserve.value = new IntersectionObserver(handleIScrollObserve, {
        rootMargin: "-100% 0% 20% 0%",
      });
      infiniteLoading.value && scrollObserve.value.observe(loading.value);
      //监听当前阅读章节
      readingObserve.value = new IntersectionObserver(handleIReadingObserve);
      //第二次点击同一本书 页面标题不会变化
      document.title = null;
      document.title = bookName + " | " + catalog.value[chapterIndex].title;
    },
    (err) => {
      showLoading.value = false;
      ElMessage({ message: "获取书籍目录失败", type: "error" });
      throw err;
    }
  );
});

onUnmounted(() => {
  window.removeEventListener("keyup", handleKeyPress);
  readSettingsVisible.value = false;
  popCataVisible.value = false;
  scrollObserve.value?.disconnect();
  readingObserve.value?.disconnect();
});
</script>

<style lang="scss" scoped>
:deep(.pop-setting) {
  margin-left: 68px;
  top: 0;
}

:deep(.pop-cata) {
  margin-left: 10px;
}

.chapter-wrapper {
  padding: 0 4%;
  flex-direction: column;
  align-items: center;

  :deep(.no-point) {
    pointer-events: none;
  }

  .tool-bar {
    position: fixed;
    top: 0;
    left: 50%;
    z-index: 100;

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        font-size: 18px;
        width: 58px;
        height: 48px;
        text-align: center;
        padding-top: 12px;
        cursor: pointer;
        outline: none;

        .iconfont {
          font-family: iconfont;
          width: 16px;
          height: 16px;
          font-size: 16px;
          margin: 0 auto 6px;
        }

        .icon-text {
          font-size: 12px;
        }
      }
    }
  }

  .read-bar {
    position: fixed;
    bottom: 0;
    right: 50%;
    z-index: 100;

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        font-size: 18px;
        width: 42px;
        height: 31px;
        padding-top: 12px;
        text-align: center;
        align-items: center;
        cursor: pointer;
        outline: none;
        margin-top: -1px;

        .iconfont {
          font-family: iconfont;
          width: 16px;
          height: 16px;
          font-size: 16px;
          margin: 0 auto 6px;
        }
      }
    }
  }

  .chapter {
    font-family: "Microsoft YaHei", PingFangSC-Regular, HelveticaNeue-Light,
      "Helvetica Neue Light", sans-serif;
    text-align: left;
    padding: 0 65px;
    min-height: 100vh;
    width: 670px;
    margin: 0 auto;

    :deep(.el-loading-mask) {
      background-color: rgba(0, 0, 0, 0);
    }
    :deep(.el-loading-spinner) {
      font-size: 36px;
      color: #b5b5b5;
    }

    :deep(.el-loading-text) {
      font-weight: 500;
      color: #b5b5b5;
    }

    .content {
      overflow: hidden;
      font-size: 18px;
      line-height: 1.8;
      font-family: "Microsoft YaHei", PingFangSC-Regular, HelveticaNeue-Light,
        "Helvetica Neue Light", sans-serif;

      .title {
        margin-bottom: 57px;
        font: 24px / 32px PingFangSC-Regular, HelveticaNeue-Light,
          "Helvetica Neue Light", "Microsoft YaHei", sans-serif;
      }

      .bottom-bar,
      .top-bar {
        height: 64px;
      }
    }
  }
}

.day {
  :deep(.popup) {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
  }

  :deep(.tool-icon) {
    border: 1px solid rgba(0, 0, 0, 0.1);
    margin-top: -1px;
    color: #000;

    .icon-text {
      color: rgba(0, 0, 0, 0.4);
    }
  }

  :deep(.chapter) {
    border: 1px solid #d8d8d8;
    color: #262626;
  }
}

.night {
  :deep(.popup) {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.48), 0 0 6px rgba(0, 0, 0, 0.16);
  }

  :deep(.tool-icon) {
    border: 1px solid #444;
    margin-top: -1px;
    color: #666;

    .icon-text {
      color: #666;
    }
  }

  :deep(.chapter) {
    border: 1px solid #444;
    color: #666;
  }

  :deep(.popper__arrow) {
    background: #666;
  }
}

@media screen and (max-width: 750px) {
  .chapter-wrapper {
    padding: 0;

    .tool-bar {
      left: 0;
      width: 100vw;
      margin-left: 0 !important;

      .tools {
        flex-direction: row;
        justify-content: space-between;

        .tool-icon {
          border: none;
        }
      }
    }

    .read-bar {
      right: 0;
      width: 100vw;
      margin-right: 0 !important;

      .tools {
        flex-direction: row;
        justify-content: space-between;
        padding: 0 15px;

        .tool-icon {
          border: none;
          width: auto;

          .iconfont {
            display: inline-block;
          }
        }
      }
    }

    .chapter {
      width: 100vw !important;
      padding: 0 20px;
      box-sizing: border-box;
    }
  }
}
</style>
