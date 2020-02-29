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
	bazel run //server/src/main/java/net/thisptr/phlegethon/server:server-main -- --spring.config.additional-location=file://`pwd`/

.PHONY: server/image
server/image:
	bazel run //server/src/main/java/net/thisptr/phlegethon/server:image-bundle -- --norun

.PHONY: jfr-uploader/run
jfr-uploader/run:
	bazel run //jfr-uploader

.PHONY: jfr-uploader/image
jfr-uploader/image:
	bazel run //jfr-uploader:image-bundle -- --norun

.PHONY: test-app/run
test-app/run:
	bazel run //tools/test-app --jvmopt="-XX:StartFlightRecording=settings=profile,maxsize=128m,dumponexit=true,filename=/tmp/test-app/" --jvmopt="-XX:FlightRecorderOptions=repository=/tmp/test-app,maxchunksize=1m"
