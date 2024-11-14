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
            :class="{ selected: theme == index }"
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
            width="270"
            trigger="click"
            v-model:visible="customFontSavePopVisible"
          >
            <p>
              已经安装在您的设备上的字体请确认输入的字体名称完整无误，或者从网络下载字体。
            </p>
            <div style="text-align: right; margin: 0">
              <el-button
                size="small"
                plain
                @click="customFontSavePopVisible = false"
                >取消</el-button
              >
              <el-button type="primary" size="small" @click="setCustomFont()"
                >确定</el-button
              >
              <el-button type="primary" size="small" @click="loadFontFromURL()"
                >网络下载</el-button
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

<script setup lang="ts">
import '../assets/fonts/popfont.css'
import '../assets/fonts/iconfont.css'
import settings from '../config/themeConfig'
import API from '@api'
import { useDebounceFn } from '@vueuse/shared'

const store = useBookStore()
const saveConfigDebounce = useDebounceFn(
  () => API.saveReadConfig(store.config),
  500,
)
//阅读界面设置改变时保存同步配置
watch(
  () => store.config,
  () => {
    saveConfigDebounce()
  },
  {
    deep: 2, //深度为2
  },
)

//主题颜色
const theme = computed(() => store.theme)
const isNight = computed(() => store.isNight)
const moonIcon = computed(() => (theme.value == 6 ? '' : ''))
const themeColors = [
  {
    background: 'rgba(250, 245, 235, 0.8)',
  },
  {
    background: 'rgba(245, 234, 204, 0.8)',
  },
  {
    background: 'rgba(230, 242, 230, 0.8)',
  },
  {
    background: 'rgba(228, 241, 245, 0.8)',
  },
  {
    background: 'rgba(245, 228, 228, 0.8)',
  },
  {
    background: 'rgba(224, 224, 224, 0.8)',
  },
  {
    background: 'rgba(0, 0, 0, 0.5)',
  },
]
const popupTheme = computed(() => {
  return {
    background: settings.themes[theme.value].popup,
  }
})
const setTheme = (theme: number) => {
  store.config.theme = theme
}

//预置字体
const fonts = ref(['雅黑', '宋体', '楷书'])
const setFont = (font: number) => {
  store.config.font = font
}
const selectedFont = computed(() => {
  return store.config.font
})
//自定义字体
const customFontName = ref(store.config.customFontName)
const customFontSavePopVisible = ref(false)
const setCustomFont = () => {
  customFontSavePopVisible.value = false
  store.config.font = -1
  store.config.customFontName = customFontName.value
}
// 加载网络字体
const loadFontFromURL = () => {
  customFontSavePopVisible.value = false
  ElMessageBox.prompt('请输入 字体网络链接', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^https?:.+$/,
    inputErrorMessage: 'url 形式不正确',
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        instance.confirmButtonLoading = true
        instance.confirmButtonText = '下载中……'
        // instance.inputValue
        const url = instance.inputValue
        if (typeof FontFace !== 'function') {
          ElMessage.error('浏览器不支持FontFace')
          return done()
        }
        const fontface = new FontFace(customFontName.value, `url("${url}")`)
        document.fonts.add(fontface)
        fontface
          .load()
          //API.getBookShelf()
          .then(function () {
            instance.confirmButtonLoading = false
            ElMessage.info('字体加载成功！')
            setCustomFont()
            done()
          })
          .catch(function (error) {
            instance.confirmButtonLoading = false
            instance.confirmButtonText = '确定'
            ElMessage.error('下载失败，请检查您输入的 url')
            throw error
          })
      } else {
        done()
      }
    },
  })
}

//字体大小
const fontSize = computed(() => {
  return store.config.fontSize
})
const moreFontSize = () => {
  if (store.config.fontSize < 48) store.config.fontSize += 2
}
const lessFontSize = () => {
  if (store.config.fontSize > 12) store.config.fontSize -= 2
}

//字 行 段落间距
const spacing = computed(() => {
  return store.config.spacing
})
const lessLetterSpacing = () => {
  store.config.spacing.letter -= 0.01
}
const moreLetterSpacing = () => {
  store.config.spacing.letter += 0.01
}
const lessLineSpacing = () => {
  store.config.spacing.line -= 0.1
}
const moreLineSpacing = () => {
  store.config.spacing.line += 0.1
}
const lessParagraphSpacing = () => {
  store.config.spacing.paragraph -= 0.1
}
const moreParagraphSpacing = () => {
  store.config.spacing.paragraph += 0.1
}

//页面宽度
const readWidth = computed(() => {
  return store.config.readWidth
})
const moreReadWidth = () => {
  // 此时会截断页面
  if (store.config.readWidth + 160 + 2 * 68 > window.innerWidth) return
  store.config.readWidth += 160
}
const lessReadWidth = () => {
  if (store.config.readWidth > 640) store.config.readWidth -= 160
}

//翻页速度
const jumpDuration = computed(() => {
  return store.config.jumpDuration
})
const moreJumpDuration = () => {
  store.config.jumpDuration += 100
}
const lessJumpDuration = () => {
  if (store.config.jumpDuration === 0) return
  store.config.jumpDuration -= 100
}

//无限加载
const infiniteLoading = computed(() => {
  return store.config.infiniteLoading
})
const setInfiniteLoading = (loading: boolean) => {
  store.config.infiniteLoading = loading
}
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
  /*   width: 478px;
  height: 350px; */
  text-align: left;
  padding: 40px 0 40px 24px;
  background: #ede7da url('../assets/imgs/themes/popup_1.png') repeat;

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
            '-apple-system',
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
            'Helvetica Neue Light',
            'Microsoft YaHei',
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
