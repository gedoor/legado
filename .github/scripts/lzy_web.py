import requests, os, datetime, sys


class lzyCloud(object):
    def __init__(self):
        self._session = requests.Session()
        self._timeout = 30  # 每个请求的超时(不包含下载响应体的用时)
        self._doupload_url = 'https://pc.woozooo.com/doupload.php'
        self._account_url = 'https://pc.woozooo.com/account.php'
        self._cookies = None
        self._headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.72 Safari/537.36 Edg/89.0.774.45',
            'Accept-Language': 'zh-CN,zh;q=0.9',
            'Referer': 'https://pc.woozooo.com/account.php?action=login'
        }
        # disable_warnings(InsecureRequestWarning)  # 全局禁用 SSL 警告

    # 日志打印
    def _log(self, msg):
        utc_time = datetime.datetime.utcnow()
        china_time = utc_time + datetime.timedelta(hours=8)
        print(f"[{china_time.strftime('%Y.%m.%d %H:%M:%S')}] {msg}")

    # get请求
    def _get(self, url, **kwargs):
        try:
            kwargs.setdefault('timeout', self._timeout)
            kwargs.setdefault('headers', self._headers)
            return self._session.get(url, verify=True, **kwargs)
        except (ConnectionError, requests.RequestException):
            return None

    # post请求
    def _post(self, url, data, **kwargs):
        try:
            kwargs.setdefault('timeout', self._timeout)
            kwargs.setdefault('headers', self._headers)
            return self._session.post(url, data, verify=True, **kwargs)
        except (ConnectionError, requests.RequestException):
            return None

    # 通过cookie登录
    def login_by_cookie(self, cookies: dict) -> bool:
        self._cookies = cookies
        self._session.cookies.update(self._cookies)
        res = self._get(self._account_url)
        if not res:
            self._log('登录失败,请重试')
            return False
        if '网盘用户登录' in res.text:
            self._log('登录失败,请更新Cookie')
            return False
        else:
            self._log('登录成功')
            return True

    # 上传文件
    def _upload_file(self, file_dir, folder_id):
        file_name = os.path.basename(file_dir)
        self._headers['Referer'] = 'https://up.woozooo.com/mydisk.php?item=files&action=index&u=' + self._cookies.get(
            "ylogin")
        data = {
            "task": "1",
            "folder_id": folder_id,
            "id": "WU_FILE_0",
            "name": file_name,
        }
        files = {'upload_file': (file_name, open(file_dir, "rb"), 'application/octet-stream')}
        res = self._post(self._doupload_url, data=data, files=files).json()
        self._log(f"{file_dir} -> {res['info']}")
        return res['zt'] == 1

    # 上传文件夹内的文件
    def _upload_folder(self, folder_dir, folder_id):
        file_list = os.listdir(folder_dir)
        for file in file_list:
            path = os.path.join(folder_dir, file)
            if os.path.isfile(path):
                self._upload_file(path, folder_id)
            else:
                self._upload_folder(path, folder_id)

    # 上传文件/文件夹内的文件
    def upload(self, dir, folder_id):
        if dir is None:
            self._log('请指定上传的文件路径')
            return
        if folder_id is None:
            self._log('请指定蓝奏云的文件夹id')
            return
        if os.path.isfile(dir):
            self._upload_file(dir, str(folder_id))
        else:
            self._upload_folder(dir, str(folder_id))


if __name__ == '__main__':
    argv = sys.argv[1:]
    if len(argv) != 2:
        print('参数错误,请以这种格式重新尝试\npython lzy_web.py 需上传的路径 蓝奏云文件夹id')
    # 需上传的路径
    upload_path = argv[0]
    # 蓝奏云文件夹id
    lzy_folder_id = argv[1]

    # Cookie 中 phpdisk_info 的值
    phpdisk_info = os.environ.get('phpdisk_info')
    # Cookie 中 ylogin 的值
    ylogin = os.environ.get('ylogin')

    # 小饼干
    cookie = {
        'ylogin': ylogin,
        'phpdisk_info': phpdisk_info
    }

    lzy = lzyCloud()
    if lzy.login_by_cookie(cookie):
        lzy.upload(upload_path, lzy_folder_id)