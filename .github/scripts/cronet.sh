#!/usr/bin/env bash

#分支Stable Dev Beta
branch=$1
#api 最大偏移
max_offset=$2

[ -z $1 ] && branch=Stable
[ -z $2 ] && max_offset=3
[ -z $GITHUB_ENV ] && echo "Error: Unexpected github workflow environment" && exit

offset=0

function fetchExitVersion() {
    # 获取最新cronet版本
    echo "fetch $branch release info from https://chromiumdash.appspot.com ..."
    lastest_cronet_version=`curl -s "https://chromiumdash.appspot.com/fetch_releases?channel=$branch&platform=Android&num=1&offset=$offset" | jq .[0].version -r`
    echo "lastest_cronet_version: $lastest_cronet_version"
    #lastest_cronet_version=100.0.4845.0
    lastest_cronet_main_version=${lastest_cronet_version%%\.*}.0.0.0
    checkVersionExit
}
function checkVersionExit() {
    # 检查版本是否存在
    local jar_url="https://storage.googleapis.com/chromium-cronet/android/$lastest_cronet_version/Release/cronet/cronet_api.jar"
    statusCode=$(curl -s -I -w %{http_code} "$jar_url" -o /dev/null)
    if [ $statusCode == "404" ];then
        echo "storage.googleapis.com return 404 for cronet $lastest_cronet_version"
        if [[ $max_offset > $offset ]]; then
            offset=$(expr $offset + 1)
            echo "retry with offset $offset"
            fetchExitVersion
        else
            exit
        fi
    fi
}
# 添加变量到github env
function writeVariableToGithubEnv() {
    echo "$1=$2" >> $GITHUB_ENV
}

##########
# 获取本地cronet版本
path=$GITHUB_WORKSPACE/gradle.properties
current_cronet_version=`cat $path | grep CronetVersion | sed s/CronetVersion=//`
echo "current_cronet_version: $current_cronet_version"

fetchExitVersion

if [[  $current_cronet_version < $lastest_cronet_version ]];then
    # 更新gradle.properties
    sed -i s/CronetVersion=.*/CronetVersion=$lastest_cronet_version/ $path
    sed -i s/CronetMainVersion=.*/CronetMainVersion=$lastest_cronet_main_version/ $path
    # 添加更新日志
    sed "15a* 更新cronet: $lastest_cronet_version" -i $GITHUB_WORKSPACE/app/src/main/assets/updateLog.md
    # 生成pull request信息
    writeVariableToGithubEnv PR_TITLE "Bump cronet from $current_cronet_version to $lastest_cronet_version"
    writeVariableToGithubEnv PR_BODY "Changes in the [Git log](https://chromium.googlesource.com/chromium/src/+log/$current_cronet_version..$lastest_cronet_version)"
    # 生成cronet flag
    writeVariableToGithubEnv cronet ok
fi