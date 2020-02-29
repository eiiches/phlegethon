load("@io_bazel_rules_docker//java:image.bzl", "java_image")

java_library(
    name = "storage",
    srcs = glob(["*.java"]),
    visibility = ["//server:__subpackages__"],
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/model",
        "@maven//:joda_time_joda_time",
    ],
)
