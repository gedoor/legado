<template>
  <div class="title" wordCount="0">{{ title }}</div>
  <div
    v-for="(para, index) in contents"
    :key="index"
    :wordCount="wordCounts[index]"
  >
    <img
      class="full"
      v-if="/^\s*<img[^>]*src[^>]+>$/.test(para)"
      :src="getImageSrc(para)"
      @error.once="proxyImage"
      loading="lazy"
    />
    <p v-else :style="{ fontFamily, fontSize }" v-html="para" />
  </div>
</template>

<script setup>
import { getImageFromLegado, isLegadoUrl } from "@/plugins/utils";

const props = defineProps({
  contents: { type: Array, required: true },
  title: { type: String, required: true },
  spacing: { type: Object, required: true },
  fontFamily: { type: String, required: true },
  fontSize: { type: String, required: true },
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

const calculateWordCount = (paragraph) => {
  const imgPattern = /<img[^>]*src="[^"]*(?:"[^>]+\})?"[^>]*>/g;
  //内嵌图片文字为1
  const imagePlaceHolder = " ";
  return paragraph.replaceAll(imgPattern, imagePlaceHolder).length;
};
const wordCounts = computed(() => {
  return Array.from(props.contents, (content) => calculateWordCount(content));
});
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

  letter-spacing: calc(v-bind("props.spacing.letter") * 1em);
  line-height: calc(1 + v-bind("props.spacing.line"));
  margin: calc(v-bind("props.spacing.paragraph") * 1em) 0;

  :deep(img) {
    height: 1em;
  }
}

.full {
  display: block;
  width: 100%;
}
</style>
