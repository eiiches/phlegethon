java_library(
    name = "service",
    srcs = glob(["*.java"]),
    resources = [
        "//server/src/main/resources:ddl.sql",
    ],
    visibility = [
        "//server/src/main/java/net/thisptr/phlegethon/server:__subpackages__",
    ],
    deps = [
        # "//server/src/main/java/net/thisptr/phlegethon/storage",
        "//server/src/main/java/net/thisptr/phlegethon/misc/sql",
        "//server/src/main/java/net/thisptr/phlegethon/model",
        # "@maven//:org_springframework_spring_web",
        # "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_zaxxer_HikariCP",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:com_google_guava_guava",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
    ],
)
