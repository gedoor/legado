<template>
  <div class="title">{{ title }}</div>
  <div v-for="(para, index) in carray" :key="index">
    <img
      class="full"
      v-if="/^\s*<img[^>]*src[^>]+>$/.test(para)"
      :src="getImageSrc(para)"
      @error.once="proxyImage"
      loading="lazy"
    />
    <p v-else :style="style" v-html="para" />
  </div>
</template>

<script setup>
import config from "../plugins/config";
import { getImageFromLegado, isLegadoUrl } from "../plugins/utils";

const store = useBookStore();
const props = defineProps({
  carray: { type: Array, required: true },
  title: { type: String, required: true },
  spacing: { type: Object, required: true },
});

const fontFamily = computed(() => {
  if (store.config.font >= 0) {
    return config.fonts[store.config.font];
  }
  return { fontFamily: store.config.customFontName };
});
const fontSize = computed(() => {
  return store.config.fontSize + "px";
});
const style = computed(() => {
  let style = fontFamily.value;
  style.fontSize = fontSize.value;
  return style;
});

const getImageSrc = (content) => {
  const imgPattern = /<img[^>]*src="([^"]*(?:"[^>]+\})?)"[^>]*>/;
  const src = content.match(imgPattern)[1];
  if (isLegadoUrl(src)) return getImageFromLegado(src);
  return src;
};
const proxyImage = (event) => {
  event.target.src = getImageFromLegado(event.target.src);
};

watch(fontSize, () => {
  store.setShowContent(false);
  nextTick(() => {
    store.setShowContent(true);
  });
});

const letterSpacing = computed(() => props.spacing.letter); //字间距 倍数
const lineSpacing = computed(() => 1 + props.spacing.line); //行距 基础行距1
const paragraphSpacing = computed(() => props.spacing.paragraph); //段距
</script>

<style lang="scss" scoped>
.title {
  margin-bottom: 57px;
  font: 24px / 32px PingFangSC-Regular, HelveticaNeue-Light,
    "Helvetica Neue Light", "Microsoft YaHei", sans-serif;
}
p {
  display: block;
  word-wrap: break-word;
  word-break: break-all;

  letter-spacing: calc(v-bind("letterSpacing") * 1em);
  line-height: v-bind("lineSpacing");
  margin: calc(v-bind("paragraphSpacing") * 1em) 0;

  :deep(img) {
    height: 1em;
  }
}

.full {
  display: block;
  width: 100%;
}
</style>
