/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.util.test.category;

/**
 * A marker to be used with JUnit categories to indicate that a test method or test class requires access to
 * RDA Data.
 *
 * To enable these tests, set the rdaDataAvailable system property, for example
 *
 * ./gradlew -DrdaDataAvailable=true :cdm-test:test --tests ucar.nc2.ft.coverage.TestGribCoverageOrth
 *
 * See gradle/root/testing.gradle
 *
 */
public interface NeedsRdaData {
}
