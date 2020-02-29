load("@io_bazel_rules_docker//java:image.bzl", "java_image")

java_library(
    name = "server",
    srcs = glob(["Application.java"]),
    resources = [
        "//server/src/main/resources:application.yaml",
    ],
    runtime_deps = [
        "//server/src/main/java/net/thisptr/phlegethon/server/controller",
        "//server/src/main/java/net/thisptr/phlegethon/service",
        "@maven//:javax_xml_bind_jaxb_api",
        "@maven//:org_springframework_boot_spring_boot_starter_web",
    ],
    deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob",
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage",
        "//server/src/main/java/net/thisptr/phlegethon/blob/types/jfr",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_joda",
        "@maven//:com_zaxxer_HikariCP",
        "@maven//:joda_time_joda_time",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
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
        ":server",
        "@maven//:org_springframework_boot_spring_boot",
    ],
)

java_binary(
    name = "server-main",
    srcs = glob(["Main.java"]),
    main_class = "net.thisptr.phlegethon.server.Main",
    runtime_deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage/s3",
        "@maven//:mysql_mysql_connector_java",
    ],
    deps = [
        ":server",
        "@maven//:org_springframework_boot_spring_boot",
    ],
)
