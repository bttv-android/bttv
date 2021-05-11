docker run --rm \
	-d \
	-ti \
	--privileged \
	-p 6080:6080 \
	-p 5554:5554 \
	-p 5555:5555 \
	-p 4723:4723 \
	-v "$1":/root/tmp \
	--name docker_android \
	budtmo/docker-android-x86-11.0
