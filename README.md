Phlegethon - Remote JFR Storage &amp; Viewer
============================================

*Status: The development has just started. Far from feature complete. Not ready for use.*

Features
--------

* HTTP API to upload and manage JFR recordings
* Organize JFR recordings by labels
* A jfr-uploader agent (written in Go), which watches a local directory and uploads jfr recordings.
  * Docker image to deploy as a sidecar container in k8s.
* **[TODO]** WEB UI to inspect CPU profiles, etc.

Requirements
------------

* MySQL 8.0.17 or newer.
* Amazon S3 or S3-compatible object storage (e.g. [minio](https://github.com/minio/minio)).

Running a server
----------------

1. Build a Docker image.

   ```sh
   make server/docker-image
   ```

2. Start MySQL and Minio.

   ```sh
   docker run --name mysql -p 3306:3306 --rm -e MYSQL_ROOT_PASSWORD=12345678 -e MYSQL_DATABASE=phlegethon mysql
   docker run --rm --name minio -p 9000:9000 -e MINIO_ACCESS_KEY=test -e MINIO_SECRET_KEY=12345678 minio/minio server /data
   ```

3. Edit a configuration file and start a server.

   ```sh
   cat > application.yaml <<EOF
   phlegethon:
       storage:
           type: s3
           s3:
               bucket_name: test
               endpoint: http://localhost:9000
               region: ap-northeast-1
               access_key_id: test
               secret_access_key: '12345678'
       db:
           url: jdbc:mysql://localhost:3306/phlegethon
           username: root
           password: '12345678'
           properties:
               cachePrepStmts: true
   EOF
   docker run --rm --name phlegethon-server -v `pwd`/application.yaml:/etc/phlegethon/application.yaml:ro -p 8080:8080 bazel/server/src/main/java/net/thisptr/phlegethon/server:docker-image --spring.config.additional-location=file::///etc/phlegethon/
   ```

4. Create a `test` namespace

   ```sh
   curl -X POST -H 'Content-type: application/json' -d '{"name": "test", "config": {"retention_seconds": 172800}}' "http://<SERVER_IP>:8080/v1/namespaces"
   ```

Running a jfr-uploader
----------------------

1. Build a Docker image.

   ```sh
   make jfr-uploader/docker-image
   ```

2. Start your application with FlightRecorder enabled. We recommend against using /tmp (the default) as `repository`.

   ```sh
   java -XX:StartFlightRecording=settings=default,maxsize=128m,dumponexit=true,filename=/tmp/jfr-uploader-test/ -XX:FlightRecorderOptions=repository=/tmp/jfr-uploader-test,maxchunksize=12m ...
   ```

   See [Advanced Runtime Options for Java](https://docs.oracle.com/en/java/javase/13/docs/specs/man/java.html#advanced-runtime-options-for-java) for details.

3. Start a jfr-uploader. See `--help` for details.

   ```sh
   docker run --rm --name jfr-uploader -v /tmp/jfr-uploader-test:/data bazel/jfr-uploader:image --label container_name=test --url http://<SERVER_IP>:8080 --namespace test --jfr-repository /data --delete
   ```

API
---

* GET /v1/namespaces

* POST /v1/namespaces

* GET /v1/namespaces/{namespace}

* DELETE /v1/namespaces/{namespace}

  Deleting a namespace will lazily delete all the related data from the object storage and database.

* PUT /v1/namespaces/{namespace}

* POST /v1/namespaces/{namespace}/recordings/upload?label.{label1_name}={label1_value}&amp;...&amp;type=jfr

* GET /v1/namespaces/{namespace}/streams/search?label.{label1_name}={label1_value}&amp;...&amp;start={start_unix_epoch_millis}&amp;end={end_unix_epoch_millis}&amp;type=jfr

* GET /v1/namespaces/{namespace}/streams/{stream_id}

* DELETE /v1/namespaces/{namespace}/streams/{stream_id}

  Deleting a stream will not actually delete the corresponding recordings in the object storage. They are still subject to retention policy set on the namespace.

* GET /v1/namespaces/{namespace}/streams/{stream_id}/recordings?start={start_unix_epoch_millis}&amp;end={end_unix_epoch_millis} *[NOT IMPLEMENTED]*

* GET /v1/namespaces/{namespace}/streams/{stream_id}/recordings/{recording_name}

* DELETE /v1/namespaces/{namespace}/streams/{stream_id}/recordings/{recording_name}

* GET /v1/namespaces/{namespace}/streams/{stream_id}/recordings/{recording_name}/download

