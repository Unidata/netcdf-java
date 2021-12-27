// java projects are tested, checked for codestyle
plugins {
    java
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.jar {
    // Fails the build when an attempt is made to add a duplicate entry to an archive.
    duplicatesStrategy = DuplicatesStrategy.FAIL

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Implementation-Vendor-Id" to project.group,
            "Implementation-Vendor" to "UCAR/Unidata",
            "Implementation-URL" to "https://www.unidata.ucar.edu/software/netcdf-java/",
            "Build-Jdk" to System.getProperty("java.version"),
            "Built-By" to System.getProperty("user.name")
        )
    }
}

tasks.withType<Javadoc> {
    group = "documentation"

    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).docEncoding = "UTF-8"
    (options as StandardJavadocDocletOptions).charSet("UTF-8")

    // When instances of JDK classes appear in our Javadoc (e.g. "java.lang.String"), create links out of them to
    // Oracle's JavaSE 11 Javadoc.
    (options as StandardJavadocDocletOptions).links("https://docs.oracle.com/en/java/javase/11/docs/api/")

    // doclint="all" (the default) will identify 100s of errors in our docs and cause no Javadoc to be generated.
    // So, turn it off.
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")

    // TODO: Actually use doclint and fix the errors it finds. Below are the options that the Gradle project uses.
    // At the very least, try 'reference'.
    // options.addStringOption 'Xdoclint:syntax,html,reference', '-quiet'
}

tasks.test {
    useJUnit {
        excludeCategories(
            "ucar.unidata.util.test.category.NeedsCdmUnitTest",
            "ucar.unidata.util.test.category.NeedsContentRoot",
            "ucar.unidata.util.test.category.NeedsExternalResource",
            "ucar.unidata.util.test.category.NeedsRdaData",
            "ucar.unidata.util.test.category.NeedsUcarNetwork",
            "ucar.unidata.util.test.category.NotPullRequest",
            "ucar.unidata.util.test.category.Slow"
        )
    }
}

val extendedTestsTask = task<Test>("extendedTests") {
    description = "Runs the extended tests."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter("test")

    useJUnit {
        includeCategories(
            "ucar.unidata.util.test.category.NeedsCdmUnitTest",
            "ucar.unidata.util.test.category.NeedsContentRoot",
            "ucar.unidata.util.test.category.NeedsExternalResource",
            "ucar.unidata.util.test.category.NotPullRequest",
            "ucar.unidata.util.test.category.Slow"
        )
    }
}

val specialTestsTask = task<Test>("specialTests") {
    description = "Runs the special tests."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter("extendedTests")

    useJUnit {
        includeCategories(
            "ucar.unidata.util.test.category.NeedsRdaData",
            "ucar.unidata.util.test.category.NeedsUcarNetwork"
        )
    }
}

tasks.check {
    dependsOn(extendedTestsTask)
}