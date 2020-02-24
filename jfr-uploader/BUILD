load("@io_bazel_rules_go//go:def.bzl", "go_binary", "go_library")
load("@io_bazel_rules_docker//go:image.bzl", "go_image")

go_image(
    name = "image",
    binary = "jfr-uploader",
)

go_library(
    name = "go_default_library",
    srcs = ["main.go"],
    importpath = "github.com/example/project/jfr-uploader",
    visibility = ["//visibility:private"],
    deps = ["@com_github_urfave_cli_v2//:go_default_library"],
)

go_binary(
    name = "jfr-uploader",
    embed = [":go_default_library"],
    visibility = ["//visibility:public"],
)
