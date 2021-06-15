docker run --rm \
	-d \
	-ti \
	--privileged \
	-p 6080:6080 \
	-p 4723:4723 \
	-v "$1":/root/tmp \
	-v "$2":/root/secret \
	--name docker_android \
	budtmo/docker-android-x86-11.0
