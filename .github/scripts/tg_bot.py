import os, sys, telebot

# 上传文件
def upload_file(tb, chat_id, file_dir):
    doc = open(file_dir, 'rb')
    tb.send_document(chat_id, doc)

# 上传文件夹内的文件
def upload_folder(tb, chat_id, folder_dir):
    file_list = sorted(os.listdir(folder_dir))
    for file in file_list:
        path = os.path.join(folder_dir, file)
        if os.path.isfile(path):
            upload_file(tb, chat_id, path)
        else:
            upload_folder(tb, chat_id, path)

# 上传
def upload(tb, chat_id, dir):
    if tb is None:
        log('ERROR: 输入正确的token')
        return
    if chat_id is None:
        log('ERROR: 输入正确的chat_id')
        return
    if dir is None:
        log('ERROR: 请指定上传的文件路径')
        return
    if os.path.isfile(dir):
        upload_file(tb, chat_id, dir)
    else:
        upload_folder(tb, chat_id, dir)

if __name__ == '__main__':
    argv = sys.argv[1:]
    if len(argv) != 3:
        log('ERROR: 参数错误,请以这种格式重新尝试\npython tg_bot.py $token $chat_id 待上传的路径')
    # Token
    TOKEN = argv[0]
    # chat_id
    chat_id = argv[1]
    # 待上传文件的路径
    upload_path = argv[2]
    #创建连接
    tb = telebot.TeleBot(TOKEN)
    #开始上传
    upload(tb, chat_id, upload_path)
