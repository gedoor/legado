<template>
  <div class="title" data-chapterpos="0" ref="titleRef">{{ title }}</div>
  <div
    v-for="(para, index) in contents"
    :key="index"
    ref="paragraphRef"
    :data-chapterpos="chapterPos[index]"
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
import { getImageFromLegado, isLegadoUrl } from "@/utils/utils";
import jump from "@/plugins/jump";

const props = defineProps({
  chapterIndex: { type: Number, required: true },
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
const chapterPos = computed(() => {
  let pos = -1;
  return Array.from(props.contents, (content) => {
    pos += calculateWordCount(content) + 1; //计算上一段的换行符
    return pos;
  });
});

const titleRef = ref();
const paragraphRef = ref();
const scrollToReadedLength = (length) => {
  if (length === 0) return;
  let paragraphIndex = chapterPos.value.findIndex(
    (wordCount) => wordCount >= length,
  );
  if (paragraphIndex === -1) return;
  nextTick(() => {
    jump(paragraphRef.value[paragraphIndex], {
      duration: 0,
    });
  });
};
defineExpose({
  scrollToReadedLength,
});
let intersectionObserver = null;
const emit = defineEmits(["readedLengthChange"]);
onMounted(() => {
  intersectionObserver = new IntersectionObserver(
    (entries) => {
      for (let { target, isIntersecting } of entries) {
        if (isIntersecting) {
          emit(
            "readedLengthChange",
            props.chapterIndex,
            parseInt(target.dataset.chapterpos),
          );
        }
      }
    },
    {
      rootMargin: `0px 0px -${window.innerHeight - 24}px 0px`,
    },
  );
  intersectionObserver.observe(titleRef.value);
  paragraphRef.value.forEach((element) => {
    intersectionObserver.observe(element);
  });
});

onUnmounted(() => {
  intersectionObserver?.disconnect();
  intersectionObserver = null;
});
</script>

<style lang="scss" scoped>
.title {
  margin-bottom: 57px;
  font:
    24px / 32px PingFangSC-Regular,
    HelveticaNeue-Light,
    "Helvetica Neue Light",
    "Microsoft YaHei",
    sans-serif;
}

p {
  display: block;
  word-wrap: break-word;
  // word-break: break-all;

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
