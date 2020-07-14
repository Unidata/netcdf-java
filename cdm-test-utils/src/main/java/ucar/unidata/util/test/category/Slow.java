package ucar.unidata.util.test.category;

/**
 * A marker to be used with JUnit categories to indicate that a test method or test class takes a long time to run.
 * We'll want to avoid running such tests after every commit; instead just run them once-a-night or so.
 *
 * To enable these tests, set the runSlowTests system property, for example
 *
 * ./gradlew -DrunSlowTests=true :subproject:test
 *
 * See gradle/root/testing.gradle
 *
 */
public interface Slow {
}
