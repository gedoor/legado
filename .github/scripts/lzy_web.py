# encoding:utf-8
# 蓝奏云上传文件
# Author: celetor
# Date: 2023-12-05

import datetime
import os
import requests
import sys
import time

# Cookie 中 phpdisk_info 的值
cookie_phpdisk_info = os.environ.get('phpdisk_info')
# Cookie 中 ylogin 的值
cookie_ylogin = os.environ.get('ylogin')

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0',
    'Accept-Language': 'zh-CN,zh;q=0.9',
    'Referer': 'https://pc.woozooo.com/account.php?action=login'
}

cookie = {
    'ylogin': cookie_ylogin,
    'phpdisk_info': cookie_phpdisk_info
}


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
    upload_url = f"https://up.woozooo.com/fileup.php?uid={cookie_ylogin}"
    headers['Referer'] = f'https://up.woozooo.com/mydisk.php?item=files&action=index&u={cookie_ylogin}'
    post_data = {
        "task": "1",
        "folder_id": f'{folder_id}',
        "id": "WU_FILE_0",
        "name": file_name,
    }
    files = {'upload_file': (file_name, open(file_dir, "rb"), 'application/octet-stream')}

    retry_time = 0
    retry_time_max = 2  # 最大重试次数
    while retry_time <= retry_time_max:
        log(f'开始第{retry_time + 1}次请求')
        try:
            response = requests.post(upload_url, data=post_data, files=files,
                                     headers=headers, cookies=cookie, timeout=3600)
            res = response.json()
            log(f"{file_dir} -> {res['info']}")
            if res['zt'] == 1:
                break
            else:
                log(f'第{retry_time + 1}次请求失败: {response.text}')
                retry_time += 1
                time.sleep(2)
        except Exception as e:
            log(f'第{retry_time + 1}次请求异常: {e}')
            retry_time += 1
            time.sleep(2)


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
