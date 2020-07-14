# buildSrc

Contains custom [Task](https://docs.gradle.org/current/dsl/org.gradle.api.Task.html)s
for publishing to the Unidata nexus server. For reference, see
[this](https://docs.gradle.org/current/userguide/organizing_build_logic.html#sec:build_sources).

## Debugging

Setup a Remote task (which is a debug task) in Intelli (default settings are fine).
Then, from the command line, run:

~~~
./gradlew help -Dorg.gradle.debug=true --no-daemon
~~~

The `help` task is just to trigger the compile and tests run of buildSrc.
I suppose it could be any task.
Once gradle is listening for a remote debugger, return to intellij and run (debug) the Remote task.
See https://docs.gradle.org/current/userguide/troubleshooting.html#_attaching_a_debugger_to_your_build