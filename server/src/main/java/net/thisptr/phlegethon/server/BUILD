load("@io_bazel_rules_docker//java:image.bzl", "java_image")

java_library(
    name = "libs",
    srcs = glob(["Application.java"]),
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage",
        "//server/src/main/java/net/thisptr/phlegethon/server/controller",
        "//server/src/main/java/net/thisptr/phlegethon/service",
        "@maven//:com_zaxxer_HikariCP",
        "@maven//:javax_validation_validation_api",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_boot_spring_boot_starter_web",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_image(
    name = "docker-image",
    srcs = glob(["Main.java"]),
    main_class = "net.thisptr.phlegethon.server.Main",
    runtime_deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage/s3",
        "@maven//:mysql_mysql_connector_java",
    ],
    deps = [
        ":libs",
        "@maven//:org_springframework_boot_spring_boot",
    ],
)

java_binary(
    name = "server",
    srcs = glob(["Main.java"]),
    main_class = "net.thisptr.phlegethon.server.Main",
    runtime_deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage/s3",
        "@maven//:mysql_mysql_connector_java",
    ],
    deps = [
        ":libs",
        "@maven//:org_springframework_boot_spring_boot",
    ],
)
