.PHONY: build
build:
	bazel build //...

.PHONY: gazelle
gazelle:
	bazel run //:gazelle

.PHONY: run-server
run-server:
	bazel run //server/src/main/java/net/thisptr/phlegethon/server:server

.PHONY: run-jfr-uploader
run-jfr-uploader:
	bazel run //jfr-uploader:image
