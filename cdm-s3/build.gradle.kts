plugins {
    id("cdm.library-conventions")
}

// Matches Maven's "project.description"
description = "The Common Data Model (CDM) AWS S3 support."

dependencies {
    implementation(platform(project(":netcdf-java-platform")))
    testImplementation(platform(project(":netcdf-java-testing-platform")))

    api(project(":cdm-core"))

    implementation(libs.awsApacheClient)
    implementation(libs.awsS3Sdk) {
        // exclude netty nio client due to open CVEs. See
        // https://github.com/aws/aws-sdk-java-v2/issues/1632
        // we don't use the nio http client in our S3 related code,
        // so we should be ok here (others may need to add it specifically to
        // their code if they are using our S3 stuff, but then it's their
        // explicit decision to run it).
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }
    implementation(libs.guava)
    implementation(libs.jsr305)
    implementation(libs.slf4j)

    testImplementation(project(":cdm-test-utils"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)

    testRuntimeOnly(libs.logbackClassic)
}
