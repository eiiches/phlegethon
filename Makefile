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

.PHONY: server/run-image
server/run-image: server/image
	docker run --network host --rm --name phlegethon-server -v `pwd`/application.yaml:/etc/phlegethon/application.yaml:ro phlegethon/server --spring.config.additional-location=file:///etc/phlegethon/

.PHONY: server/image
server/image:
	bazel run //server/src/main/java/net/thisptr/phlegethon/server:image-bundle -- --norun

.PHONY: server/create-test-namespace
server/create-test-namespace:
	curl -X POST -H 'Content-type: application/json' -d '{"name": "test", "config": {"retention_seconds": 172800}}' "http://localhost:8080/v1/namespaces"

.PHONY: jfr-uploader/run
jfr-uploader/run:
	bazel run //jfr-uploader -- --label app_name=test-app --url http://localhost:8080 --namespace test --jfr-repository /tmp/test-app --delete

.PHONY: jfr-uploader/run-image
jfr-uploader/run-image: jfr-uploader/image
	docker run --network host --rm --name jfr-uploader -v /tmp/test-app:/data phlegethon/jfr-uploader --label app_name=test-app --url http://localhost:8080 --namespace test --jfr-repository /data --delete

.PHONY: jfr-uploader/image
jfr-uploader/image:
	bazel run //jfr-uploader:image-bundle -- --norun

.PHONY: test-app/run
test-app/run:
	bazel run //tools/test-app --jvmopt="-XX:StartFlightRecording=settings=profile,maxsize=128m,dumponexit=true,filename=/tmp/test-app/" --jvmopt="-XX:FlightRecorderOptions=repository=/tmp/test-app,maxchunksize=1m"
