load("@io_bazel_rules_docker//java:image.bzl", "java_image")

java_image(
    name = "server",
    main_class = "net.thisptr.phlegethon.server.Server",
    srcs = glob(["*.java"]),
	deps = [
        "@maven//:org_slf4j_slf4j_api",
	],
)
