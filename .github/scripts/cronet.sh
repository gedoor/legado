#bin/sh
echo "fetch release info from https://chromiumdash.appspot.com ..."

branch="Stable"

lastest_cronet_version=`curl -s "https://chromiumdash.appspot.com/fetch_releases?channel=$branch&platform=Android&num=1&offset=0" | jq .[0].version -r`
echo "lastest_cronet_version: $lastest_cronet_version"

path=$GITHUB_WORKSPACE/gradle.properties
current_cronet_version=`cat $path | grep CronetVersion | sed s/CronetVersion=//`
echo "current_cronet_version: $current_cronet_version"

if [[  $current_cronet_version == $lastest_cronet_version ]];then
    echo "cronet is already latest"
else
    sed -i s/CronetVersion=.*/CronetVersion=$lastest_cronet_version/ $path
    sed "15a* 更新cronet: $lastest_cronet_version" -i $GITHUB_WORKSPACE/app/src/main/assets/updateLog.md
    echo "start download latest cronet"
    chmod +x gradlew
    ./gradlew app:downloadCronet
fi
