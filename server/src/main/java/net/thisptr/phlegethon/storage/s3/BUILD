load("@io_bazel_rules_docker//java:image.bzl", "java_image")

java_library(
    name = "s3",
    srcs = glob(["*.java"]),
    visibility = ["//server/src/main/java/net/thisptr/phlegethon/server:__subpackages__"],
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/storage",
        "@maven//:com_amazonaws_aws_java_sdk_core",
        "@maven//:com_amazonaws_aws_java_sdk_s3",
        "@maven//:javax_validation_validation_api",
        "@maven//:org_hibernate_validator_hibernate_validator",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
