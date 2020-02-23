.PHONY: build
build:
	bazel build //...

.PHONY: test
test:
	bazel test //...

.PHONY: gazelle
gazelle:
	bazel run //:gazelle

.PHONY: pin-maven
pin-maven:
	bazel run @unpinned_maven//:pin

.PHONY: run-server
run-server:
	bazel run //server/src/main/java/net/thisptr/phlegethon/server:server-main -- --spring.config.additional-location=file://`pwd`/ --logging.level.root=debug

.PHONY: run-jfr-uploader
run-jfr-uploader:
	bazel run //jfr-uploader:image
