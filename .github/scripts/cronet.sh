#bin/sh

global_rematch() {
     local s="$2" regex=$1 debug=$3
     if [[ -z "$1" || -z "$2" ]]; then
         echo -e "usage: global_rematch <regex> <string> <debug flag>"
     else
         if [[ ! $s =~ $regex ]]; then
             echo -e "info: not matched!"
         fi
         while [[ $s =~ $regex ]]; do
             if [[ -n ${BASH_REMATCH[1]} ]];then
                 [[  -n $debug ]] && echo "${BASH_REMATCH[0]}"
                 echo "${BASH_REMATCH[1]}"
                 s=`echo $s | sed s/${BASH_REMATCH[1]}//g`
             else
                 echo -e "info: regex not has match group!"
             fi
         done
     fi
 }

echo "start download cronet info..."

##beta version
# curl https://storage.googleapis.com/chromium-cronet/ -s > cronet
# global_rematch 'android.([0-9\.]+).Release.VERSION' "`cat cronet`" | tail -1 > lastest_cronet_version

##stable version
##fetch chrome stable release page
##https://chromereleases.googleblog.com/search/label/Stable%20updates
##match "released Chrome 98 (98.0.4758.101) for Android"
curl https://chromereleases.googleblog.com/search/label/Stable%20updates -s > stable_page
global_rematch 'released Chrome[ 0-9]+\(([0-9\.]+)\).{5}Android' "`cat stable_page`" | head -1 > lastest_cronet_version

lastest_cronet_version=`cat lastest_cronet_version`
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
