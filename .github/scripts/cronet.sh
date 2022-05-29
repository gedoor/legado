#!/usr/bin/env bash

echo "fetch release info from https://chromiumdash.appspot.com ..."

branch="Stable"

lastest_cronet_version=`curl -s "https://chromiumdash.appspot.com/fetch_releases?channel=$branch&platform=Android&num=1&offset=0" | jq .[0].version -r`
echo "lastest_cronet_version: $lastest_cronet_version"
#lastest_cronet_version=100.0.4845.0

function checkVersionExit() {
    local jar_url="https://storage.googleapis.com/chromium-cronet/android/$lastest_cronet_version/Release/cronet/cronet_api.jar"
    statusCode=$(curl -s -I -w %{http_code} "$jar_url" -o /dev/null)
    if [ $statusCode == "404" ];then
        echo "storage.googleapis.com return 404 for cronet $lastest_cronet_version"
        exit
    fi
}

path=$GITHUB_WORKSPACE/gradle.properties
current_cronet_version=`cat $path | grep CronetVersion | sed s/CronetVersion=//`
echo "current_cronet_version: $current_cronet_version"

if [[  $current_cronet_version == $lastest_cronet_version ]];then
    echo "cronet is already latest"
else
    checkVersionExit
    sed -i s/CronetVersion=.*/CronetVersion=$lastest_cronet_version/ $path
    sed "15a* 更新cronet: $lastest_cronet_version" -i $GITHUB_WORKSPACE/app/src/main/assets/updateLog.md
    echo "start download latest cronet"
    chmod +x gradlew
    ./gradlew app:downloadCronet
fi

