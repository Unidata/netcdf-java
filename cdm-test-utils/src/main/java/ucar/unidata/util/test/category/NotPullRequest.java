package ucar.unidata.util.test.category;

/**
 * A marker to be used with JUnit categories to indicate that a test method or test class cannot succeed when run
 * within the CI environment (currently GitHub Actions) used for checking pull requests.
 *
 * @author cwardgar
 * @since 2015/03/19
 */
public interface NotPullRequest {
}
