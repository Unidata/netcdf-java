`root/`: Contains script plugins that should be applied only to the root project.

`any/`: Contains script plugins that can be applied to any project, including the root. 
Most of what we need in the `any/` directory can be found in these two script plugins:

  1. `java-library.gradle`: Provides support for building library projects.
  For most of our stuff, this is what we want.
  Uses the following plugins:
     * [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html)
     * `gradle/any/javadoc.gradle`
     * `gradle/any/testing.gradle`
     * `gradle/any/coverage.gradle`
     * `gradle/any/archiving.gradle`
     * `gradle/any/publishing.gradle`

  2. `test-only-projects.gradle`: Provides support for building test only projects. 
  Uses the following plugins:
     * [java](https://docs.gradle.org/current/userguide/java_plugin.html)
     * `gradle/any/testing.gradle`
     * `gradle/any/coverage.gradle`

     Currently, two gradle subprojects use this - `dap4:d4tests` and `cdm-test`.

`gretty/`: Contains configuration files related to the `gretty` plugin (logging, cert for testing, etc.)

**TODO:** There are still a few Gradle things left to do, but at least we're fully functional at this point.

1. Address any issues in our plugin scripts identified by gradle in terms of Gradle 7 compatibility.

   ~~~
   Deprecated Gradle features were used in this build, making it incompatible with Gradle 7.0.
   Use '--warning-mode all' to show the individual deprecation warnings.
   See https://docs.gradle.org/6.5.1/userguide/command_line_interface.html#sec:command_line_warnings
   ~~~

   This is more about doing proactive maintenance with our gradle infrastructure.
   I do not want to be in the position of trying to jump three major versions again :-)

2. Find new dependency license checker
   The license plugin we used in the past does not seem to work with the java-library and java-platform plugins.
   Sad times.
