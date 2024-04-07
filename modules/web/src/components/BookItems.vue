<template>
  <div class="books-wrapper">
    <div class="wrapper">
      <div
        class="book"
        v-for="book in books"
        :key="book.bookUrl"
        @click="handleClick(book)"
      >
        <div class="cover-img">
          <img
            class="cover"
            :src="getCover(book.coverUrl)"
            :key="book.coverUrl"
            alt=""
            loading="lazy"
          />
        </div>
        <div class="info">
          <div class="name">{{ book.name }}</div>
          <div class="sub">
            <div class="author">
              {{ book.author }}
            </div>
            <div class="tags" v-if="isSearch">
              <el-tag
                v-for="tag in book.kind?.split(',').slice(0, 2)"
                :key="tag"
              >
                {{ tag }}
              </el-tag>
            </div>
            <div class="update-info" v-if="!isSearch">
              <div class="dot">•</div>
              <div class="size">共{{ book.totalChapterNum }}章</div>
              <div class="dot">•</div>
              <div class="date">{{ dateFormat(book.lastCheckTime) }}</div>
            </div>
          </div>
          <div class="intro" v-if="isSearch">{{ book.intro }}</div>

          <div class="dur-chapter" v-if="!isSearch">
            已读：{{ book.durChapterTitle }}
          </div>
          <div class="last-chapter">最新：{{ book.latestChapterTitle }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { dateFormat } from "../utils/utils";
const props = defineProps(["books", "isSearch"]);
const emit = defineEmits(["bookClick"]);
const handleClick = (book) => emit("bookClick", book);
const getCover = (coverUrl) => {
  return /^data:/.test(coverUrl)
    ? coverUrl
    : (import.meta.env.VITE_API || location.origin) +
        "/cover?path=" +
        encodeURIComponent(coverUrl);
};

const subJustify = computed(() =>
  props.isSearch ? "space-between" : "flex-start",
);
</script>

<style lang="scss" scoped>
.books-wrapper {
  overflow: auto;

  .wrapper {
    display: grid;
    grid-template-columns: repeat(auto-fill, 380px);
    justify-content: space-around;
    grid-gap: 10px;

    .book {
      user-select: none;
      display: flex;
      cursor: pointer;
      margin-bottom: 18px;
      padding: 24px 24px;
      width: 360px;
      flex-direction: row;
      justify-content: space-around;

      .cover-img {
        width: 84px;
        height: 112px;

        .cover {
          width: 84px;
          height: 112px;
        }
      }

      .info {
        display: flex;
        flex-direction: column;
        justify-content: space-around;
        align-items: left;
        height: 112px;
        margin-left: 20px;
        flex: 1;
        overflow: hidden;
        
        .name {
          width: fit-content;
          font-size: 16px;
          font-weight: 700;
          color: #33373d;
        }

        .sub {
          display: flex;
          flex-direction: row;
          align-items: baseline;
          justify-content: v-bind("subJustify");
          font-size: 12px;
          font-weight: 600;
          color: #6b6b6b;
          .tags {
            :deep(.el-tag) {
              margin-right: 0.5em;
            }
          }
          .update-info {
            display: flex;
            .dot {
              margin: 0 7px;
            }
          }
        }

        .intro,
        .dur-chapter,
        .last-chapter {
          color: #969ba3;
          font-size: 13px;
          margin-top: 3px;
          font-weight: 500;
          word-wrap: break-word;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 1;
          text-align: left;
        }
      }
    }

    .book:hover {
      background: rgba(0, 0, 0, 0.1);
      transition-duration: 0.5s;
    }
  }

  .wrapper:last-child {
    margin-right: auto;
  }
}

.books-wrapper::-webkit-scrollbar {
  width: 0 !important;
}

@media screen and (max-width: 750px) {
  .books-wrapper {
    .wrapper {
      display: flex;
      flex-direction: column;

      .book {
        box-sizing: border-box;
        width: 100%;
        margin-bottom: 0;
        padding: 10px 20px;
      }
    }
  }
}
</style>
