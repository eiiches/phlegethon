java_library(
    name = "controller",
    srcs = glob(["*.java"]),
    visibility = [
        "//server/src/main/java/net/thisptr/phlegethon/server:__pkg__",
    ],
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/model",
        "//server/src/main/java/net/thisptr/phlegethon/service",
        "//server/src/main/java/net/thisptr/phlegethon/storage",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_google_errorprone_error_prone_annotations",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_web",
    ],
)
