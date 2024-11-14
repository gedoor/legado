import { watch, unref, onUnmounted } from 'vue'
import { ElLoading } from 'element-plus'
import loadingSvg from '@element-plus/icons-svg/loading.svg?raw'
import 'element-plus/theme-chalk/el-loading.css'
import './loading.css'

export const useLoading = (
  target: MaybeRef<string | HTMLElement | undefined>,
  text: string,
  spinner = loadingSvg,
) => {
  // loading spinner
  const isLoading = ref(false)
  let loadingInstance: ReturnType<typeof ElLoading.service> | null = null
  const closeLoading = () => (isLoading.value = false)
  const showLoading = () => (isLoading.value = true)
  watch(isLoading, loading => {
    if (!loading) return loadingInstance?.close()
    loadingInstance = ElLoading.service({
      target: unref(target),
      spinner: spinner,
      text: text,
      lock: true,
      background: 'rgba(0, 0, 0, 0)',
    })
  })

  const loadingWrapper = (promise: Promise<unknown>) => {
    if (!(promise instanceof Promise))
      throw TypeError('loadingWrapper argument must be Promise')
    showLoading()
    return promise.finally(closeLoading)
  }

  onUnmounted(() => {
    closeLoading()
  })

  return { isLoading, showLoading, closeLoading, loadingWrapper }
}
