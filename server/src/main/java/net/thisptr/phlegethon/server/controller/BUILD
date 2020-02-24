java_library(
    name = "controller",
    srcs = glob(["*.java"]),
    visibility = [
        "//server/src/main/java/net/thisptr/phlegethon/server:__pkg__",
    ],
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage",
        "//server/src/main/java/net/thisptr/phlegethon/misc",
        "//server/src/main/java/net/thisptr/phlegethon/model",
        "//server/src/main/java/net/thisptr/phlegethon/service",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_google_errorprone_error_prone_annotations",
        "@maven//:javax_servlet_javax_servlet_api",
        "@maven//:joda_time_joda_time",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_core",
        "@maven//:org_springframework_spring_web",
    ],
)
