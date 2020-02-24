Phlegethon - Remote JFR Storage &amp; Viewer
============================================

*Status: The development has just started. Far from feature complete. Not ready for use.*

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

Running a jfr-uploader
----------------------

1. Build a Docker image.

   ```sh
   make jfr-uploader/docker-image
   ```

2. Start your application with FlightRecorder enabled. Avoid using default repository=/tmp directory.

   ```sh
   java -XX:StartFlightRecording=settings=default,maxsize=128m -XX:FlightRecorderOptions=repository=/tmp/jfr-uploader-test,maxchunksize=12m ...
   ```

3. Start a jfr-uploader.

   ```sh
   docker run --rm --name phlegethon-jfr-uploader -v `pwd` run //jfr-uploader:docker-image -labels container_name=test -url http://localhost:8080 -namespace test -repository /tmp/jfr-uploader-test
   ```

API
---

* GET /v1/namespaces

* POST /v1/namespaces

* GET /v1/namespaces/{namespace}

* DELETE /v1/namespaces/{namespace}

* PUT /v1/namespaces/{namespace}

* POST /v1/namespaces/{namespace}/recordings/upload?label.{label1_name}={label1_value}&amp;...&amp;type=jfr

* GET /v1/namespaces/{namespace}/streams/search?label.{label1_name}={label1_value}&amp;...&amp;start={start_unix_epoch_millis}&amp;end={end_unix_epoch_millis}&amp;type=jfr

* GET /v1/namespaces/{namespace}/streams/{stream_id}

* GET /v1/namespaces/{namespace}/streams/{stream_id}/recordings?start={start_unix_epoch_millis}&amp;end={end_unix_epoch_millis}

* GET /v1/namespaces/{namespace}/streams/{stream_id}/recordings/{recording_name}

* GET /v1/namespaces/{namespace}/streams/{stream_id}/recordings/{recording_name}/download

