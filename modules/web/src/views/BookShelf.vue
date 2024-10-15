<template>
  <div :class="{ 'index-wrapper': true, night: isNight, day: !isNight }">
    <div class="navigation-wrapper">
      <div class="navigation-title-wrapper">
        <div class="navigation-title">阅读</div>
        <div class="navigation-sub-title">清风不识字，何故乱翻书</div>
      </div>
      <div class="search-wrapper">
        <el-input
          placeholder="搜索书籍，在线书籍自动加入书架"
          v-model="searchWord"
          class="search-input"
          :prefix-icon="SearchIcon"
          @keyup.enter="searchBook"
        >
        </el-input>
      </div>
      <div class="bottom-wrapper">
        <div class="recent-wrapper">
          <div class="recent-title">最近阅读</div>
          <div class="reading-recent">
            <el-tag
              :type="
                readingRecent.name == '尚无阅读记录' ? 'warning' : 'primary'
              "
              class="recent-book"
              size="large"
              @click="
                toDetail(
                  readingRecent.bookUrl,
                  readingRecent.name,
                  readingRecent.author,
                  readingRecent.chapterIndex,
                  readingRecent.chapterPos,
                  readingRecent.isSeachBook,
                  true,
                )
              "
              :class="{ 'no-point': readingRecent.bookUrl == '' }"
            >
              {{ readingRecent.name }}
            </el-tag>
          </div>
        </div>
        <div class="setting-wrapper">
          <div class="setting-title">基本设定</div>
          <div class="setting-item">
            <el-tag
              :type="connectType"
              size="large"
              class="setting-connect"
              :class="{ 'no-point': newConnect }"
              @click="setLegadoRetmoteUrl"
            >
              {{ connectStatus }}
            </el-tag>
          </div>
        </div>
      </div>
      <div class="bottom-icons">
        <a
          href="https://github.com/gedoor/legado_web_bookshelf"
          target="_blank"
        >
          <div class="bottom-icon">
            <img :src="githubUrl" alt="" />
          </div>
        </a>
      </div>
    </div>
    <div class="shelf-wrapper" ref="shelfWrapper">
      <book-items
        :books="books"
        @bookClick="handleBookClick"
        :isSearch="isSearching"
      ></book-items>
    </div>
  </div>
</template>

<script setup lang="ts">
import '@/assets/bookshelf.css'
import '@/assets/fonts/shelffont.css'
import { useBookStore } from '@/store'
import githubUrl from '@/assets/imgs/github.png'
import { useLoading } from '@/hooks/loading'
import { Search as SearchIcon } from '@element-plus/icons-vue'
import { baseURL_localStorage_key } from '@/api/axios'
import API, {
  legado_http_entry_point,
  parseLeagdoHttpUrlWithDefault,
  setApiEntryPoint,
} from '@api'
import { validatorHttpUrl } from '@/utils/utils'
import type { Book, SeachBook } from '@/book'
import type { webReadConfig } from '@/web'

const store = useBookStore()
const isNight = computed(() => store.isNight)

/** shortcuts of `store.setConfig` */
const applyReadConfig = (config?: webReadConfig) => {
  try {
    if (config !== undefined) store.setConfig(config)
  } catch {
    ElMessage.info('阅读界面配置解析错误')
  }
}

const readingRecent = ref<typeof store.readingBook>({
  name: '尚无阅读记录',
  author: '',
  bookUrl: '',
  chapterIndex: 0,
  chapterPos: 0,
  isSeachBook: false,
})

const shelfWrapper = ref<HTMLElement>()
//const shelfWrapper = useTemplateRef<HTMLElement>("shelfWrapper")
const { showLoading, closeLoading, loadingWrapper, isLoading } = useLoading(
  shelfWrapper,
  '正在获取书籍信息',
)

// 书架书籍和在线书籍搜索
const books = shallowRef<Book[] | SeachBook[]>([])
const shelf = computed(() => store.shelf)
const searchWord = ref('')
const isSearching = ref(false)
watchEffect(() => {
  if (isSearching.value && searchWord.value != '') return
  isSearching.value = false
  books.value = []
  if (searchWord.value == '') {
    books.value = shelf.value
    return
  }
  books.value = shelf.value.filter(book => {
    return (
      book.name.includes(searchWord.value) ||
      book.author.includes(searchWord.value)
    )
  })
})
//搜索在线书籍
const searchBook = () => {
  if (searchWord.value == '') return
  books.value = []
  store.clearSearchBooks()
  showLoading()
  isSearching.value = true
  API.search(
    searchWord.value,
    searcBooks => {
      if (isLoading) {
        closeLoading()
      }
      try {
        store.setSearchBooks(searcBooks)
        books.value = store.searchBooks
        //store.searchBooks.forEach((item) => books.value.push(item));
      } catch (e) {
        ElMessage.error('后端数据错误')
        throw e
      }
    },
    () => {
      closeLoading()
      if (books.value.length == 0) {
        ElMessage.info('搜索结果为空')
      }
    },
  )
}

//连接状态
const connectStatus = computed(() => store.connectStatus)
const connectType = computed(() => store.connectType)
const newConnect = computed(() => store.newConnect)
const setLegadoRetmoteUrl = () => {
  ElMessageBox.prompt(
    '请输入 后端地址 ( 如：http://127.0.0.1:9527 或者通过内网穿透的地址)',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: legado_http_entry_point,
      inputValidator: value => validatorHttpUrl(value),
      inputErrorMessage: '输入的格式不对',
      beforeClose: (action, instance, done) => {
        if (action === 'confirm') {
          store.setNewConnect(true)
          instance.confirmButtonLoading = true
          instance.confirmButtonText = '校验中……'
          // instance.inputValue
          const url = new URL(instance.inputValue).toString()
          API.getReadConfig(url)
            //API.getBookShelf()
            .then(function (config) {
              applyReadConfig(config)
              instance.confirmButtonLoading = false
              store.setConnectType('success')
              store.clearSearchBooks()
              store.setNewConnect(false)
              setApiEntryPoint(...parseLeagdoHttpUrlWithDefault(url))
              if (url === location.origin) {
                localStorage.removeItem(baseURL_localStorage_key)
              } else {
                localStorage.setItem(baseURL_localStorage_key, url)
              }
              store.setConnectStatus('已连接 ' + url.toString())
              fetchBookShelfData()
              done()
            })
            .catch(function (error) {
              instance.confirmButtonLoading = false
              instance.confirmButtonText = '确定'
              ElMessage.error('访问失败，请检查您输入的 url')
              store.setNewConnect(false)
              throw error
            })
        } else {
          done()
        }
      },
    },
  )
}

const router = useRouter()
const handleBookClick = async (book: SeachBook | Book) => {
  // 判断是否为 searchBook
  const isSeachBook = 'respondTime' in book
  if (isSeachBook) {
    await API.saveBook(book)
  }
  const {
    bookUrl,
    name,
    author,
    // @ts-expect-error: descruct with default value
    durChapterIndex = 0,
    // @ts-expect-error: descruct with default value
    durChapterPos = 0,
  } = book

  toDetail(bookUrl, name, author, durChapterIndex, durChapterPos, isSeachBook)
}
const toDetail = (
  bookUrl: string,
  bookName: string,
  bookAuthor: string,
  chapterIndex: number,
  chapterPos: number,
  isSeachBook: boolean | undefined = false,
  fromReadRecentClick = false,
) => {
  if (bookName === '尚无阅读记录') return
  // 最近书籍不再书架上 自动搜索
  if (
    fromReadRecentClick &&
    shelf.value.every(book => book.bookUrl !== bookUrl)
  ) {
    searchWord.value = bookName
    searchBook()
    return
  }
  sessionStorage.setItem('bookUrl', bookUrl)
  sessionStorage.setItem('bookName', bookName)
  sessionStorage.setItem('bookAuthor', bookAuthor)
  sessionStorage.setItem('chapterIndex', String(chapterIndex))
  sessionStorage.setItem('chapterPos', String(chapterPos))
  sessionStorage.setItem('isSeachBook', String(isSeachBook))
  readingRecent.value = {
    name: bookName,
    author: bookAuthor,
    bookUrl,
    chapterIndex,
    chapterPos,
    isSeachBook,
  }
  localStorage.setItem('readingRecent', JSON.stringify(readingRecent.value))
  router.push({
    path: '/chapter',
  })
}

const loadShelf = async () => {
  try {
    await store.loadWebConfig()
    await store.saveBookProgress()
    //确保各种网络情况下同步请求先完成
    await fetchBookShelfData()
  } catch (error: unknown) {
    store.setConnectType('danger')
    store.setConnectStatus('连接异常')
    store.setNewConnect(false)
    throw error
  }
}

const fetchBookShelfData = async () => {
  await store.loadBookShelf()
  store.setConnectType('primary')
  store.setConnectStatus('已连接 ' + legado_http_entry_point)
  store.setNewConnect(false)
}
onMounted(() => {
  //获取最近阅读书籍
  const readingRecentStr = localStorage.getItem('readingRecent')
  if (readingRecentStr != null) {
    readingRecent.value = JSON.parse(readingRecentStr)
    if (typeof readingRecent.value.chapterIndex == 'undefined') {
      readingRecent.value.chapterIndex = 0
    }
  }
  console.log('bookshelf mounted')
  loadingWrapper(loadShelf())
})
</script>

<style lang="scss" scoped>
.index-wrapper {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: row;

  .navigation-wrapper {
    width: 260px;
    min-width: 260px;
    padding: 48px 36px;
    background-color: #f7f7f7;

    .navigation-title {
      font-size: 24px;
      font-weight: 500;
      font-family: FZZCYSK;
    }

    .navigation-sub-title {
      font-size: 16px;
      font-weight: 300;
      font-family: FZZCYSK;
      margin-top: 16px;
      color: #b1b1b1;
    }

    .search-wrapper {
      .search-input {
        border-radius: 50%;
        margin-top: 24px;

        :deep(.el-input__wrapper) {
          border-radius: 50px;
          border-color: #e3e3e3;
        }
      }
    }

    .bottom-wrapper {
      display: flex;
      flex-direction: column;
    }

    .recent-wrapper {
      margin-top: 36px;

      .recent-title {
        font-size: 14px;
        color: #b1b1b1;
        font-family: FZZCYSK;
      }

      .reading-recent {
        margin: 18px 0;

        .recent-book {
          font-size: 10px;
          /*           // font-weight: 400;
          // margin: 12px 0;
          // font-weight: 500;
          // color: #6B7C87; */
          cursor: pointer;
          /*           // padding: 6px 18px; */
        }
      }
    }

    .setting-wrapper {
      margin-top: 36px;

      .setting-title {
        font-size: 14px;
        color: #b1b1b1;
        font-family: FZZCYSK;
      }

      .no-point {
        pointer-events: none;
      }

      .setting-connect {
        font-size: 8px;
        margin-top: 16px;
        /*         // color: #6B7C87; */
        cursor: pointer;
      }
    }

    .bottom-icons {
      position: fixed;
      bottom: 0;
      height: 120px;
      width: 260px;
      align-items: center;
      display: flex;
      flex-direction: row;
    }
  }

  .shelf-wrapper {
    padding: 48px 48px;
    width: 100%;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    overflow: hidden;
  }
}

@media screen and (max-width: 750px) {
  .index-wrapper {
    overflow-x: hidden;
    flex-direction: column;

    .navigation-wrapper {
      padding: 20px 24px;
      box-sizing: border-box;
      width: 100%;

      .navigation-title-wrapper {
        white-space: nowrap;
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
      }

      .bottom-wrapper {
        flex-direction: row;

        > * {
          flex-grow: 1;
          margin-top: 18px;

          .reading-recent,
          .setting-item {
            margin-bottom: 0px;
          }
        }
      }

      .bottom-icons {
        display: none;
      }
    }

    .shelf-wrapper {
      padding: 0;
      flex-grow: 1;

      :deep(.el-loading-spinner) {
        display: none;
      }
    }
  }
}

.night {
  .navigation-wrapper {
    background-color: #454545;

    .navigation-title {
      color: #aeaeae;
    }

    .search-wrapper {
      .search-input {
        .el-input__wrapper {
          background-color: #454545;
        }

        .el-input__inner {
          color: #b1b1b1;
        }
      }
    }
  }

  :deep(.shelf-wrapper) {
    background-color: #161819;
  }
}
</style>
