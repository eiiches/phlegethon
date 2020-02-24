.PHONY: build
build:
	bazel build //...

.PHONY: test
test:
	bazel test //...

.PHONY: gazelle
gazelle:
	bazel run //:gazelle
	bazel run //:gazelle -- update-repos -from_file=jfr-uploader/go.mod

.PHONY: pin-maven
pin-maven:
	bazel run @unpinned_maven//:pin

.PHONY: server/run
server/run:
	bazel run //server/src/main/java/net/thisptr/phlegethon/server:server-main -- --spring.config.additional-location=file://`pwd`/ --logging.level.root=debug

.PHONY: server/docker-image
server/docker-image:
	bazel run //server/src/main/java/net/thisptr/phlegethon/server:docker-image -- --norun

.PHONY: jfr-uploader/run
jfr-uploader/run:
	bazel run //jfr-uploader

.PHONY: jfr-uploader/docker-image
jfr-uploader/docker-image:
	bazel run //jfr-uploader:image -- --norun
