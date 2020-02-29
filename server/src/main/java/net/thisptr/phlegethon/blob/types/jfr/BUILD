java_library(
    name = "jfr",
    srcs = glob(["*.java"]),
    visibility = [
        "//server/src/main/java/net/thisptr/phlegethon/server:__subpackages__",
    ],
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob",
        "//server/src/main/java/net/thisptr/phlegethon/misc",
        "@maven//:com_github_luben_zstd_jni",
        "@maven//:com_google_errorprone_error_prone_annotations",
        "@maven//:joda_time_joda_time",
    ],
)
