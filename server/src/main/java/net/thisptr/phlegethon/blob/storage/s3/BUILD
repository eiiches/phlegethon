load("@io_bazel_rules_docker//java:image.bzl", "java_image")

java_library(
    name = "s3",
    srcs = glob(["*.java"]),
    visibility = [
        "//server/src/main/java/net/thisptr/phlegethon/server:__subpackages__",
        "//server/src/test/java/net/thisptr/phlegethon/blob/storage/s3:__pkg__",
    ],
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage",
        "//server/src/main/java/net/thisptr/phlegethon/model",
        "@maven//:com_amazonaws_aws_java_sdk_core",
        "@maven//:com_amazonaws_aws_java_sdk_s3",
        "@maven//:com_google_errorprone_error_prone_annotations",
        "@maven//:javax_validation_validation_api",
        "@maven//:joda_time_joda_time",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
