import { defineStore } from 'pinia'

export const useConnectionStore = defineStore('connection', {
  state: () => {
    return {
      connectStatus: '正在连接后端服务器……',
      connectType: 'primary' as 'primary' | 'success' | 'danger',
      newConnect: false,
    }
  },
  actions: {
    setConnectStatus(connectStatus: string) {
      if (this.newConnect === true) return
      this.connectStatus = connectStatus
    },
    setConnectType(connectType: 'primary' | 'success' | 'danger') {
      if (this.newConnect === true) return
      this.connectType = connectType
    },
    setNewConnect(newConnect: boolean) {
      this.newConnect = newConnect
    },
  },
})
