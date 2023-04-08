# 阅读web端
 使用vue3 web书架和web源编辑
## 路由
* http://localhost:8080/ 书架
* http://localhost:8080/#/bookSource 书源编辑
* http://localhost:8080/#/rssSource 订阅源编辑
## 开发
> 需要阅读app提供后端服务，开发前修改环境变量`VITE_API`为阅读web服务地址

```bash
echo "VITE_API=http://<ip>:<port>" > .env.development
pnpm dev
```
