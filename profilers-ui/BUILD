load("//tools/base/bazel:bazel.bzl", "iml_module")

iml_module(
    name = "profilers-ui",
    srcs = ["src"],
    resources = ["resources"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/adt/idea/profilers[module]",
        "//tools/idea/platform/annotations[module]",
        "//tools/idea/.idea/libraries:studio-profiler-grpc-1.0-jarjar",
        "//tools/idea/platform/core-api[module]",
        "//tools/idea/.idea/libraries:Guava",
        "//tools/adt/idea/adt-ui[module]",
        "//tools/adt/idea/adt-ui-model[module]",
        "//tools/idea/platform/platform-api[module]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/base/perflib:studio.perflib[module]",
    ],
)
