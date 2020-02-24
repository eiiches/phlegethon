Phlegethon - Remote JFR Storage & Viewer
========================================

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

2. Start MySQL and Minio (if you don't have).

   ```sh
   docker run --name mysql -p 3306:3306 --rm -e MYSQL_ROOT_PASSWORD=12345678 -e MYSQL_DATABASE=phlegethon mysql
   docker run --rm --name minio -p 9000:9000 -e MINIO_ACCESS_KEY=test -e MINIO_SECRET_KEY=12345678 minio/minio server /data
   ```

3. Write a configuration file and run.

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