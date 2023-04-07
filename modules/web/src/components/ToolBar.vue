<template>
  <div class="menu flex-column-center">
    <el-button
      v-for="button in buttons"
      size="large"
      :key="button.name"
      @click="button.action"
    >
      {{ button.name }}
    </el-button>
    <el-button size="large" @click="() => (hotkeysDialogVisible = true)"
      >快捷键</el-button
    >
  </div>
  <el-dialog
    v-model="hotkeysDialogVisible"
    :show-close="false"
    :before-close="stopRecordKeyDown"
  >
    <template #header="{ titleClass, titleId }">
      <div class="hotkeys-header flex-space-between">
        <div :id="titleId" :class="titleClass">
          快捷键设置
          <span v-if="recordKeyDowning">
            <el-text> / 录入中 </el-text>
          </span>
        </div>
        <el-button
          :disabled="recordKeyDowning"
          @click="bindHotKeys"
          :icon="CircleCheckFilled"
          >保存</el-button
        >
      </div>
    </template>

    <div class="hotkeys-settings flex-column-center">
      <div
        v-for="(button, index) in buttons"
        :key="button.name"
        class="hotkeys-item flex-space-between"
      >
        <span class="title"
          ><el-text>{{ button.name }}</el-text></span
        >
        <div class="hotkeys-item__content">
          <div v-for="(key, index) in button.hotKeys" :key="key">
            <kbd>{{ key }}</kbd>
            <span v-if="index + 1 < button.hotKeys.length">
              <el-text>+</el-text>
            </span>
          </div>
          <span v-if="button.hotKeys.length == 0">未设置</span>
        </div>
        <el-button
          :disabled="recordKeyDowning"
          text
          :icon="Edit"
          @click="recordKeyDown(index)"
          >编辑</el-button
        >
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import API from "@api";
import { CircleCheckFilled, Edit } from "@element-plus/icons-vue";
import hotkeys from "hotkeys-js";
import { isInvaildSource } from "../utils/souce";

const store = useSourceStore();
const pull = () => {
  API.getSources().then(({ data }) => {
    if (data.isSuccess) {
      store.changeTabName("editList");
      store.saveSources(data.data);
      ElMessage({
        message: `成功拉取${data.data.length}条源`,
        type: "success",
      });
    } else {
      ElMessage({
        message: data.errorMsg ?? "后端错误",
        type: "error",
      });
    }
  });
};

const push = () => {
  let sources = store.sources;
  store.changeTabName("editList");
  if (sources.length === 0) {
    return ElMessage({
      message: "空空如也",
      type: "info",
    });
  }
  ElMessage({
    message: "正在推送中",
    type: "info",
  });
  API.saveSources(sources).then(({ data }) => {
    if (data.isSuccess) {
      let okData = data.data;
      if (Array.isArray(okData)) {
        let failMsg = ``;
        if (sources.length > okData.length) {
          failMsg = "\n推送失败的源将用红色字体标注!";
          store.setPushReturnSources(okData);
        }
        ElMessage({
          message: `批量推送源到「阅读3.0APP」\n共计: ${
            sources.length
          } 条\n成功: ${okData.length} 条\n失败: ${
            sources.length - okData.length
          } 条${failMsg}`,
          type: "success",
        });
      }
    } else {
      ElMessage({
        message: `批量推送源失败!\nErrorMsg: ${data.errorMsg}`,
        type: "error",
      });
    }
  });
};

const conver2Tab = () => {
  store.changeTabName("editTab");
  store.changeEditTabSource(store.currentSource);
};
const conver2Source = () => {
  store.changeCurrentSource(store.editTabSource);
};

const undo = () => {
  store.editHistoryUndo();
};

const clearEdit = () => {
  store.clearEdit();
  ElMessage({
    message: "已清除",
    type: "success",
  });
};

const redo = () => {
  store.clearEdit();
  store.clearAllHistory();
  ElMessage({
    message: "已清除所有历史记录",
    type: "success",
  });
};

const saveSource = () => {
  let isBookSource = /bookSource/.test(location.href),
    /** @type {import("@/source.js").Source} */
    source = store.currentSource;
  if (isInvaildSource(source)) {
    API.saveSource(source).then(({ data }) => {
      if (data.isSuccess) {
        ElMessage({
          message: `源《${
            isBookSource ? source.bookSourceName : source.sourceName
          }》已成功保存到「阅读3.0APP」`,
          type: "success",
        });
        //save to store
        store.saveCurrentSource();
      } else {
        ElMessage({
          message: `源《${
            isBookSource ? source.bookSourceName : source.sourceName
          }》保存失败!\nErrorMsg: ${data.errorMsg}`,
          type: "error",
        });
      }
    });
  } else {
    ElMessage({
      message: `请检查<必填>项是否全部填写`,
      type: "error",
    });
  }
};

const debug = () => {
  store.startDebug();
};

const buttons = ref(
  Array.of(
    { name: "⇈推送源", hotKeys: [], action: push },
    { name: "⇊拉取源", hotKeys: [], action: pull },
    { name: "⋙生成源", hotKeys: [], action: conver2Tab },
    { name: "⋘编辑源", hotKeys: [], action: conver2Source },
    { name: "✗清空表单", hotKeys: [], action: clearEdit },
    { name: "↶撤销操作", hotKeys: [], action: undo },
    { name: "↷重做操作", hotKeys: [], action: redo },
    { name: "⇏调试源", hotKeys: [], action: debug },
    { name: "✓保存源", hotKeys: [], action: saveSource }
  )
);
const hotkeysDialogVisible = ref(true);

const recordKeyDowning = ref(false);

const recordKeyDownIndex = ref(-1);

const stopRecordKeyDown = () => {
  recordKeyDowning.value = false;
};

watch(hotkeysDialogVisible, (visibale) => {
  if (!visibale) return hotkeys.unbind("*");
  hotkeys.unbind();
  /**监听按键 */
  hotkeys("*", (event) => {
    event.preventDefault();
    if (recordKeyDowning.value && recordKeyDownIndex.value > -1)
      buttons.value[recordKeyDownIndex.value].hotKeys =
        // @ts-ignore
        hotkeys.getPressedKeyString();
  });
});

const recordKeyDown = (index) => {
  recordKeyDowning.value = true;
  ElMessage({
    message: "按ESC键或者点击空白处结束录入",
    type: "info",
  });
  buttons.value[index].hotKeys = [];
  recordKeyDownIndex.value = index;
};

const bindHotKeys = () => {
  hotkeysDialogVisible.value = false;
  const hotKeysConfig = [];
  buttons.value.forEach(({ hotKeys, action }) => {
    hotkeys(hotKeys.join("+"), (event) => {
      event.preventDefault();
      action.call(null);
    });
    hotKeysConfig.push(hotKeys);
  });
  saveHotkeysConfig(hotKeysConfig);
};

const saveHotkeysConfig = (config) => {
  localStorage.setItem("legado_web_hotkeys", JSON.stringify(config));
};

const readHotkeysConfig = () => {
  try {
    const config = JSON.parse(localStorage.getItem("legado_web_hotkeys"));
    if (!Array.isArray(config) || config.length == 0) return;
    buttons.value.forEach((button, index) => (button.hotKeys = config[index]));
    hotkeysDialogVisible.value = false;
    bindHotKeys();
  } catch {
    ElMessage({ message: "快捷键配置错误", type: "error" });
    localStorage.removeItem("legado_web_hotkeys");
  }
};

onMounted(() => {
  /**读取热键配置 */
  readHotkeysConfig();
});
</script>

<style lang="scss" scoped>
.flex-space-between {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.flex-column-center {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.menu > .el-button {
  margin: 4px;
  padding: 1em;
  width: 6em;
}

.hotkeys-item {
  .title {
    width: 5em;
    display: flex;
    justify-content: flex-end;
    margin-right: 1em;
  }
  &__content {
    display: flex;
    flex-wrap: wrap;
    flex: 1;
    div {
      margin-bottom: 1em;
    }
    span {
      margin: 0.5em;
    }
  }
}
</style>
