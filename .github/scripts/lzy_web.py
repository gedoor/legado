import requests, os, datetime, sys

# Cookie 中 phpdisk_info 的值
cookie_phpdisk_info = os.environ.get('phpdisk_info')
# Cookie 中 ylogin 的值
cookie_ylogin = os.environ.get('ylogin')

# 请求头
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.72 Safari/537.36 Edg/89.0.774.45',
    'Accept-Language': 'zh-CN,zh;q=0.9',
    'Referer': 'https://pc.woozooo.com/account.php?action=login'
}

# 小饼干
cookie = {
    'ylogin': cookie_ylogin,
    'phpdisk_info': cookie_phpdisk_info
}


# 日志打印
def log(msg):
    utc_time = datetime.datetime.utcnow()
    china_time = utc_time + datetime.timedelta(hours=8)
    print(f"[{china_time.strftime('%Y.%m.%d %H:%M:%S')}] {msg}")


# 检查是否已登录
def login_by_cookie():
    url_account = "https://pc.woozooo.com/account.php"
    if cookie['phpdisk_info'] is None:
        log('ERROR: 请指定 Cookie 中 phpdisk_info 的值！')
        return False
    if cookie['ylogin'] is None:
        log('ERROR: 请指定 Cookie 中 ylogin 的值！')
        return False
    res = requests.get(url_account, headers=headers, cookies=cookie, verify=True)
    if '网盘用户登录' in res.text:
        log('ERROR: 登录失败,请更新Cookie')
        return False
    else:
        log('登录成功')
        return True


# 上传文件
def upload_file(file_dir, folder_id):
    file_name = os.path.basename(file_dir)
    url_upload = "https://up.woozooo.com/fileup.php"
    headers['Referer'] = f'https://up.woozooo.com/mydisk.php?item=files&action=index&u={cookie_ylogin}'
    post_data = {
        "task": "1",
        "folder_id": folder_id,
        "id": "WU_FILE_0",
        "name": file_name,
    }
    files = {'upload_file': (file_name, open(file_dir, "rb"), 'application/octet-stream')}
    res = requests.post(url_upload, data=post_data, files=files, headers=headers, cookies=cookie, timeout=120).json()
    log(f"{file_dir} -> {res['info']}")
    return res['zt'] == 1


# 上传文件夹内的文件
def upload_folder(folder_dir, folder_id):
    file_list = sorted(os.listdir(folder_dir), reverse=True)
    for file in file_list:
        path = os.path.join(folder_dir, file)
        if os.path.isfile(path):
            upload_file(path, folder_id)
        else:
            upload_folder(path, folder_id)


# 上传
def upload(dir, folder_id):
    if dir is None:
        log('ERROR: 请指定上传的文件路径')
        return
    if folder_id is None:
        log('ERROR: 请指定蓝奏云的文件夹id')
        return
    if os.path.isfile(dir):
        upload_file(dir, str(folder_id))
    else:
        upload_folder(dir, str(folder_id))


if __name__ == '__main__':
    argv = sys.argv[1:]
    if len(argv) != 2:
        log('ERROR: 参数错误,请以这种格式重新尝试\npython lzy_web.py 需上传的路径 蓝奏云文件夹id')
    # 需上传的路径
    upload_path = argv[0]
    # 蓝奏云文件夹id
    lzy_folder_id = argv[1]
    if login_by_cookie():
        upload(upload_path, lzy_folder_id)
