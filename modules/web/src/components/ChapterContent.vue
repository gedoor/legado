<template>
  <div v-for="(para, index) in props.carray" :key="index">
    <img
      class="full"
      v-if="/^\s*<img[^>]*src[^>]+>$/.test(para)"
      :src="getImageSrc(para)"
      loading="lazy"
    />
    <p v-else :style="style" v-html="para" />
  </div>
</template>

<script setup>
import config from "../plugins/config";
const store = useBookStore();
const props = defineProps(["carray"]);

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

function getImageSrc(content) {
  const imgPattern = /<img[^>]*src="([^"]*(?:"[^>]+\})?)"[^>]*>/;
  return content.match(imgPattern)[1];
}

watch(fontSize, () => {
  store.setShowContent(false);
  nextTick(() => {
    store.setShowContent(true);
  });
});
</script>

<style lang="scss" scoped>
p {
  display: block;
  word-wrap: break-word;
  word-break: break-all;

  :deep(img) {
    height: 1em;
  }
}

.full {
  display: block;
  width: 100%;
}
</style>
