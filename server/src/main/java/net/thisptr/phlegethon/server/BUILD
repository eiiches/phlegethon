load("@io_bazel_rules_docker//java:image.bzl", "java_image")

java_image(
    name = "server",
    srcs = glob(["*.java"]),
    main_class = "net.thisptr.phlegethon.server.Server",
    deps = [
        "@maven//:org_slf4j_slf4j_api",
    ],
)
