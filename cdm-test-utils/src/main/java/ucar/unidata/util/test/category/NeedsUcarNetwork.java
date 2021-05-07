package ucar.unidata.util.test.category;


/**
 * A marker to be used with JUnit categories to indicate that a test method or test class requires access to a
 * resource that is only accessible on the UCAR network in order to complete successfully.
 *
 * To enable these tests, set the ucarNetworkAvailable system property, for example
 *
 * ./gradlew -DucarNetworkAvailable=true :cdm-s3:test --tests ucar.unidata.io.s3.TestS3Read
 *
 * See gradle/root/testing.gradle
 *
 */
public interface NeedsUcarNetwork {

}
