# (paths must be absolute)
# ./test.sh <dir that contains twitch.apk> <dir that contains secrets>

[ -z "$1" ] && exit 1
[ -z "$2" ] && exit 1

echo "start environment"
./startDocker.sh "$1" "$2"

echo "wait for device to be ready"
./wait.sh

echo "install bttv"
docker exec docker_android adb install /root/tmp/twitch.apk

echo "login"
docker exec docker_android adb root
docker exec docker_android adb push /root/secret/user.xml /data/data/tv.twitch.bttvandroid.app/shared_prefs/user.xml

# run tests
npx wdio

echo "stop/rm docker"
# docker stop docker_android
