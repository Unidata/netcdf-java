# cdm-test-utils

Contains miscellaneous utility methods for use in the CDM test suite.
Published for internal use by other THREDDS projects.

## Inclusion of third-party software

This project contains source code from [JUnit](https://github.com/junit-team/junit4), version 4.12.
The license for JUnit is available in `third-party-licenses/junit/`.

### Details of use:

Copied several methods from `org.junit.Assert` that are used in the implementations of our
`Assert2.assertNearlyEquals()` family of methods.
