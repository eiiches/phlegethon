load("@io_bazel_rules_go//go:def.bzl", "go_binary", "go_library")
load("@io_bazel_rules_docker//go:image.bzl", "go_image")
load("@io_bazel_rules_docker//container:container.bzl", "container_bundle")

go_image(
    name = "image",
    base = "@debian-image//image",
    binary = "jfr-uploader",
)

go_library(
    name = "go_default_library",
    srcs = [
        "main.go",
        "state.go",
        "uploader.go",
    ],
    importpath = "github.com/example/project/jfr-uploader",
    visibility = ["//visibility:private"],
    deps = [
        "@com_github_mattn_go_sqlite3//:go_default_library",
        "@com_github_urfave_cli_v2//:go_default_library",
        "@org_uber_go_zap//:go_default_library",
    ],
)

go_binary(
    name = "jfr-uploader",
    embed = [":go_default_library"],
    visibility = ["//visibility:public"],
)

container_bundle(
    name = "image-bundle",
    images = {
        "phlegethon/jfr-uploader:latest": ":image",
    },
)
