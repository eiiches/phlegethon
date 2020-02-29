load("@io_bazel_rules_docker//java:image.bzl", "java_image")
load("@io_bazel_rules_docker//container:container.bzl", "container_bundle", "container_image", "container_layer")

java_library(
    name = "server",
    srcs = glob(["Application.java"]),
    resources = [
        "//server/src/main/resources:application.yaml",
    ],
    runtime_deps = [
        "//server/src/main/java/net/thisptr/phlegethon/blob/storage/s3",
        "//server/src/main/java/net/thisptr/phlegethon/server/controller",
        "//server/src/main/java/net/thisptr/phlegethon/service",
        "@maven//:javax_xml_bind_jaxb_api",
        "@maven//:mysql_mysql_connector_java",
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

container_image(
    name = "openjdk-image",
    base = "@openjdk-image//image",
    layers = [
        ":openjdk-image-symlinks",
    ],
)

container_layer(
    name = "openjdk-image-symlinks",
    symlinks = {
        "/usr/bin/java": "/usr/local/openjdk-11/bin/java",
    },
)

java_image(
    name = "image",
    srcs = glob(["Main.java"]),
    base = ":openjdk-image",
    main_class = "net.thisptr.phlegethon.server.Main",
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

container_bundle(
    name = "image-bundle",
    images = {
        "phlegethon/server:latest": ":image",
    },
)
