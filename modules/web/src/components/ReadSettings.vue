<template>
  <div
    class="settings-wrapper"
    :style="popupTheme"
    :class="{ night: isNight, day: !isNight }"
  >
    <div class="settings-title">设置</div>
    <div class="setting-list">
      <ul>
        <li class="theme-list">
          <i>阅读主题</i>
          <span
            class="theme-item"
            v-for="(themeColor, index) in themeColors"
            :key="index"
            :style="themeColor"
            ref="themes"
            @click="setTheme(index)"
            :class="{ selected: selectedTheme == index }"
            ><em v-if="index < 6" class="iconfont">&#58980;</em
            ><em v-else class="moon-icon">{{ moonIcon }}</em></span
          >
        </li>
        <li class="font-list">
          <i>正文字体</i>
          <span
            class="font-item"
            v-for="(font, index) in fonts"
            :key="index"
            :class="{ selected: selectedFont == index }"
            @click="setFont(index)"
            >{{ font }}</span
          >
        </li>
        <li class="font-list">
          <i>自定字体</i>
          <el-tooltip effect="dark" content="自定义的字体名称" placement="top">
            <input
              type="text"
              class="font-item font-item-input"
              v-model="customFontName"
              placeholder="请输入自定义的字体名称"
            />
          </el-tooltip>

          <el-popover
            placement="top"
            width="180"
            trigger="click"
            v-model:visible="customFontSavePopVisible"
          >
            <p>
              请确认输入的字体名称完整无误，并且该字体已经安装在您的设备上。
            </p>
            <p>确定保存吗？</p>
            <div style="text-align: right; margin: 0">
              <el-button
                size="small"
                plain
                @click="customFontSavePopVisible = false"
                >取消</el-button
              >
              <el-button
                type="primary"
                size="small"
                @click="
                  setCustomFont();
                  customFontSavePopVisible = false;
                "
                >确定</el-button
              >
            </div>
            <template #reference>
              <span type="text" class="font-item">保存</span>
            </template>
          </el-popover>
        </li>
        <li class="font-size">
          <i>字体大小</i>
          <div class="resize">
            <span class="less" @click="lessFontSize"
              ><em class="iconfont">&#58966;</em></span
            ><b></b> <span class="lang">{{ fontSize }}</span
            ><b></b>
            <span class="more" @click="moreFontSize"
              ><em class="iconfont">&#58976;</em></span
            >
          </div>
        </li>
        <li class="letter-spacing">
          <i>字距</i>
          <div class="resize">
            <span class="less" @click="lessLetterSpacing"
              ><em class="iconfont">&#58966;</em></span
            ><b></b> <span class="lang">{{ spacing.letter.toFixed(2) }}</span
            ><b></b>
            <span class="more" @click="moreLetterSpacing"
              ><em class="iconfont">&#58976;</em></span
            >
          </div>
        </li>
        <li class="line-spacing">
          <i>行距</i>
          <div class="resize">
            <span class="less" @click="lessLineSpacing"
              ><em class="iconfont">&#58966;</em></span
            ><b></b> <span class="lang">{{ spacing.line.toFixed(1) }}</span
            ><b></b>
            <span class="more" @click="moreLineSpacing"
              ><em class="iconfont">&#58976;</em></span
            >
          </div>
        </li>
        <li class="paragraph-spacing">
          <i>段距</i>
          <div class="resize">
            <div class="resize">
              <span class="less" @click="lessParagraphSpacing"
                ><em class="iconfont">&#58966;</em></span
              ><b></b>
              <span class="lang">{{ spacing.paragraph.toFixed(1) }}</span
              ><b></b>
              <span class="more" @click="moreParagraphSpacing"
                ><em class="iconfont">&#58976;</em></span
              >
            </div>
          </div>
        </li>
        <li class="read-width" v-if="!store.miniInterface">
          <i>页面宽度</i>
          <div class="resize">
            <span class="less" @click="lessReadWidth"
              ><em class="iconfont">&#58965;</em></span
            ><b></b> <span class="lang">{{ readWidth }}</span
            ><b></b>
            <span class="more" @click="moreReadWidth"
              ><em class="iconfont">&#58975;</em></span
            >
          </div>
        </li>
        <li class="paragraph-spacing">
          <i>翻页速度</i>
          <div class="resize">
            <div class="resize">
              <span class="less" @click="lessJumpDuration">
                <em class="iconfont">&#xe625;</em>
              </span>
              <b></b> <span class="lang">{{ jumpDuration }}</span
              ><b></b>
              <span class="more" @click="moreJumpDuration"
                ><em class="iconfont">&#xe626;</em></span
              >
            </div>
          </div>
        </li>
        <li class="infinite-loading">
          <i>无限加载</i>
          <span
            class="infinite-loading-item"
            :key="0"
            :class="{ selected: infiniteLoading == false }"
            @click="setInfiniteLoading(false)"
            >关闭</span
          >
          <span
            class="infinite-loading-item"
            :key="1"
            :class="{ selected: infiniteLoading == true }"
            @click="setInfiniteLoading(true)"
            >开启</span
          >
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import "../assets/fonts/popfont.css";
import "../assets/fonts/iconfont.css";
import settings from "../config/themeConfig";
import API from "@api";
const store = useBookStore();

const theme = ref(0);

const isNight = ref(store.config.theme == 6);
const moonIcon = ref("");
const themeColors = shallowRef([
  {
    background: "rgba(250, 245, 235, 0.8)",
  },
  {
    background: "rgba(245, 234, 204, 0.8)",
  },
  {
    background: "rgba(230, 242, 230, 0.8)",
  },
  {
    background: "rgba(228, 241, 245, 0.8)",
  },
  {
    background: "rgba(245, 228, 228, 0.8)",
  },
  {
    background: "rgba(224, 224, 224, 0.8)",
  },
  {
    background: "rgba(0, 0, 0, 0.5)",
  },
]);
const moonIconStyle = ref({
  display: "inline",
  color: "rgba(255,255,255,0.2)",
});
const fonts = ref(["雅黑", "宋体", "楷书"]);
const customFontName = ref(store.config.customFontName);
const customFontSavePopVisible = ref(false);

onMounted(() => {
  //初始化设置项目
  var config = store.config;
  theme.value = config.theme;
  if (theme.value == 6) {
    moonIcon.value = "";
  } else {
    moonIcon.value = "";
  }
});
const config = computed(() => {
  return store.config;
});

const popupTheme = computed(() => {
  return {
    background: settings.themes[config.value.theme].popup,
  };
});
const selectedTheme = computed(() => {
  return store.config.theme;
});
const selectedFont = computed(() => {
  return store.config.font;
});

const setTheme = (theme) => {
  if (theme == 6) {
    isNight.value = true;
    moonIcon.value = "";
    moonIconStyle.value.color = "#ed4259";
  } else {
    isNight.value = false;
    moonIcon.value = "";
    moonIconStyle.value.color = "rgba(255,255,255,0.2)";
  }
  config.value.theme = theme;
  saveConfig(config.value);
};
const setFont = (font) => {
  config.value.font = font;
  saveConfig(config.value);
};
const setCustomFont = () => {
  config.value.font = -1;
  config.value.customFontName = customFontName.value;
  saveConfig(config.value);
};

const fontSize = computed(() => {
  return store.config.fontSize;
});
const moreFontSize = () => {
  if (config.value.fontSize < 48) config.value.fontSize += 2;
  saveConfig(config.value);
};
const lessFontSize = () => {
  if (config.value.fontSize > 12) config.value.fontSize -= 2;
  saveConfig(config.value);
};

const spacing = computed(() => {
  return store.config.spacing;
});
const lessLetterSpacing = () => {
  store.config.spacing.letter -= 0.01;
  saveConfig(config.value);
};
const moreLetterSpacing = () => {
  store.config.spacing.letter += 0.01;
  saveConfig(config.value);
};
const lessLineSpacing = () => {
  store.config.spacing.line -= 0.1;
  saveConfig(config.value);
};
const moreLineSpacing = () => {
  store.config.spacing.line += 0.1;
  saveConfig(config.value);
};
const lessParagraphSpacing = () => {
  store.config.spacing.paragraph -= 0.1;
  saveConfig(config.value);
};
const moreParagraphSpacing = () => {
  store.config.spacing.paragraph += 0.1;
  saveConfig(config.value);
};

const readWidth = computed(() => {
  return store.config.readWidth;
});
const moreReadWidth = () => {
  // 此时会截断页面
  if (config.value.readWidth + 160 + 2 * 68 > window.innerWidth) return;
  config.value.readWidth += 160;
  saveConfig(config.value);
};
const lessReadWidth = () => {
  if (config.value.readWidth > 640) config.value.readWidth -= 160;
  saveConfig(config.value);
};
const jumpDuration = computed(() => {
  return store.config.jumpDuration;
});
const moreJumpDuration = () => {
  store.config.jumpDuration += 100;
  saveConfig(config.value);
};
const lessJumpDuration = () => {
  if (store.config.jumpDuration === 0) return;
  store.config.jumpDuration -= 100;
  saveConfig(config.value);
};
const infiniteLoading = computed(() => {
  return store.config.infiniteLoading;
});
const setInfiniteLoading = (loading) => {
  config.value.infiniteLoading = loading;
  saveConfig(config.value);
};
const saveConfig = (config) => {
  store.setConfig(config);
  localStorage.setItem("config", JSON.stringify(config));
  uploadConfig(config);
};
const uploadConfig = (config) => {
  API.saveReadConfig(config);
};
</script>

<style lang="scss" scoped>
:deep(.iconfont) {
  font-family: iconfont;
  font-style: normal;
}

:deep(.moon-icon) {
  font-family: iconfont;
  font-style: normal;
}

.settings-wrapper {
  user-select: none;
  margin: -13px;
  // width: 478px;
  // height: 350px;
  text-align: left;
  padding: 40px 0 40px 24px;
  background: #ede7da url("../assets/imgs/themes/popup_1.png") repeat;

  .settings-title {
    font-size: 18px;
    line-height: 22px;
    margin-bottom: 28px;
    font-family: FZZCYSK;
    font-weight: 400;
  }

  .setting-list {
    max-height: calc(70vh - 50px);
    overflow: auto;

    ul {
      list-style: none outside none;
      margin: 0;
      padding: 0;

      li {
        list-style: none outside none;

        i {
          font:
            12px / 16px PingFangSC-Regular,
            "-apple-system",
            Simsun;
          display: inline-block;
          min-width: 48px;
          margin-right: 16px;
          vertical-align: middle;
          color: #666;
        }

        .theme-item {
          line-height: 32px;
          width: 34px;
          height: 34px;
          margin-right: 16px;
          margin-top: 5px;
          border-radius: 100%;
          display: inline-block;
          cursor: pointer;
          text-align: center;
          vertical-align: middle;

          .iconfont {
            display: none;
          }
        }

        .selected {
          color: #ed4259;

          .iconfont {
            display: inline;
          }
        }
      }

      .font-list,
      .infinite-loading {
        margin-top: 28px;

        .font-item,
        .infinite-loading-item {
          width: 78px;
          height: 34px;
          cursor: pointer;
          margin-right: 16px;
          border-radius: 2px;
          text-align: center;
          vertical-align: middle;
          display: inline-block;
          font:
            14px / 34px PingFangSC-Regular,
            HelveticaNeue-Light,
            "Helvetica Neue Light",
            "Microsoft YaHei",
            sans-serif;
        }
        .font-item-input {
          width: 168px;
          color: #000000;
        }
        .selected {
          color: #ed4259;
          border: 1px solid #ed4259;
        }

        .font-item:hover,
        .infinite-loading-item:hover {
          border: 1px solid #ed4259;
          color: #ed4259;
        }
      }

      .font-size,
      .read-width,
      .letter-spacing,
      .line-spacing,
      .paragraph-spacing {
        margin-top: 28px;

        .resize {
          display: inline-block;
          width: 274px;
          height: 34px;
          vertical-align: middle;
          border-radius: 2px;

          span {
            width: 89px;
            height: 34px;
            line-height: 34px;
            display: inline-block;
            cursor: pointer;
            text-align: center;
            vertical-align: middle;

            em {
              font-style: normal;
            }
          }

          .less:hover,
          .more:hover {
            color: #ed4259;
          }

          .lang {
            color: #a6a6a6;
            font-weight: 400;
            font-family: FZZCYSK;
          }

          b {
            display: inline-block;
            height: 20px;
            vertical-align: middle;
          }
        }
      }
    }
  }
}

.night {
  :deep(.theme-item) {
    border: 1px solid #666;
  }

  :deep(.selected) {
    border: 1px solid #666;
  }

  :deep(.moon-icon) {
    color: #ed4259;
  }

  :deep(.font-list),
  .infinite-loading {
    .font-item,
    .infinite-loading-item {
      border: 1px solid #666;
      background: rgba(45, 45, 45, 0.5);
    }
  }

  :deep(.resize) {
    border: 1px solid #666;
    background: rgba(45, 45, 45, 0.5);

    b {
      border-right: 1px solid #666;
    }
  }
}

.day {
  :deep(.theme-item) {
    border: 1px solid #e5e5e5;
  }

  :deep(.selected) {
    border: 1px solid #ed4259;
  }

  :deep(.moon-icon) {
    display: inline;
    color: rgba(255, 255, 255, 0.2);
  }

  :deep(.font-list),
  .infinite-loading {
    .font-item,
    .infinite-loading-item {
      background: rgba(255, 255, 255, 0.5);
      border: 1px solid rgba(0, 0, 0, 0.1);
    }
  }

  :deep(.resize) {
    border: 1px solid #e5e5e5;
    background: rgba(255, 255, 255, 0.5);

    b {
      border-right: 1px solid #e5e5e5;
    }
  }
}

@media screen and (max-width: 500px) {
  .settings-wrapper i {
    display: flex !important;
    flex-wrap: wrap;
    padding-bottom: 5px !important;
  }
}
</style>
