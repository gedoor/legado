import type { AxiosResponse } from 'axios'
import type { LeagdoApiResponse } from './api'
import API, { setWebsocketOnError, setApiEntryPoint } from './api'
import ajax from './axios'
import { validatorHttpUrl } from '@/utils/utils'

const LeagdoApiResponseKeys: string[] = Array.of('isSuccess', 'errorMsg')

const notification = ElMessage
/** Axios.Interceptor: check if resp is LeagaoLeagdoApiResponse*/
const responseCheckInterceptor = (resp: AxiosResponse) => {
  let isLeagdoApiResponse = true
  try {
    const data = resp.data

    for (const key of LeagdoApiResponseKeys) {
      if (!(key in data)) {
        isLeagdoApiResponse = false
        LeagdoApiResponseKeys.length = 0
      }
    }
    if ((data as LeagdoApiResponse<unknown>).isSuccess === true) {
      if (!('data' in data)) {
        isLeagdoApiResponse = false
      }
    }
  } catch {
    isLeagdoApiResponse = false
  }
  if (isLeagdoApiResponse === false) {
    notification.warning({ message: '后端返回内容格式错误', grouping: true })
    throw new Error()
  }
  return resp
}

const axiosErrorInterceptor = (err: unknown) => {
  notification.error({
    message: '后端连接失败，请检查阅读WEB服务或者设置其它可用链接',
    grouping: true,
  })
  throw err
}
// http全局
ajax.interceptors.response.use(responseCheckInterceptor, axiosErrorInterceptor)
// websocket
setWebsocketOnError(axiosErrorInterceptor)

/**
 * 按照阅读的默认规则 解析阅读HTTP WebSocket API入口地址
 * @returns [http_url, webSocekt_url]
 */
export const parseLeagdoHttpUrlWithDefault = (
  http_url: string | URL,
): [string, string] => {
  let url = new URL(location.origin) //默认当前网址的origin部分
  if (validatorHttpUrl(http_url)) {
    url = new URL(http_url)
  }
  const { protocol, port } = url
  // websocket服务端口 为http服务端口 + 1
  let legado_webSocket_port
  if (port !== '') {
    legado_webSocket_port = String(Number(port) + 1)
  } else {
    legado_webSocket_port = protocol.startsWith('https:') ? '444' : '81'
  }
  // websocket协议是否为加密版本
  const legado_webSocket_protocol = protocol.startsWith('https:')
    ? 'wss://'
    : 'ws://'

  const http_entry_point = url.toString()

  url.protocol = legado_webSocket_protocol
  url.port = legado_webSocket_port
  const webSocket_entry_point = url.toString()

  console.info('legado_api_config:')
  console.table({
    'http API入口': http_entry_point,
    'webSocket API入口': webSocket_entry_point,
  })
  return [http_entry_point, webSocket_entry_point]
}

//export const useLeagdoRemoteUrlDialog = () => { }

setApiEntryPoint(
  ...parseLeagdoHttpUrlWithDefault(ajax.defaults.baseURL as string),
)

export default API
export * from './api'
