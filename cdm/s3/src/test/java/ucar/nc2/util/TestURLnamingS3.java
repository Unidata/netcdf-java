/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Checks dataset reference resolution with the S3 file provider available. */
public class TestURLnamingS3 {
  private static final String BASE = "cdms3://profile@localhost/bucket?site/wrapper.ncml#delimiter=/";

  /** Preserve absolute dataset URIs for both NcML wrappers and aggregation members. */
  @Test
  public void testAbsoluteReferences() {
    for (String base : new String[] {BASE, "cdms3:bucket?wrapper.ncml"}) {
      for (String reference : new String[] {"http://localhost/data.nc",
          "https://localhost/data.nc?selection=time#fragment", "dods://localhost/thredds/dodsC/data.nc",
          "dap4://localhost/thredds/dap4/data.nc", "cdms3://profile@localhost/other-bucket?data.nc#delimiter=/",
          "file:/tmp/data.nc"}) {
        assertThat(URLnaming.resolve(base, reference)).isEqualTo(reference);
        assertThat(URLnaming.resolveFile(base, reference)).isEqualTo(reference);
      }
    }
  }

  /** Resolve relative object names within the containing object prefix. */
  @Test
  public void testRelativeReferences() {
    for (String reference : new String[] {"data.nc", "nested/data.nc", "data%20file.nc"}) {
      String expected = "cdms3://profile@localhost/bucket?site/" + reference + "#delimiter=/";
      assertThat(URLnaming.resolve(BASE, reference)).isEqualTo(expected);
      assertThat(URLnaming.resolveFile(BASE, reference)).isEqualTo(expected);
    }
  }

  /** Return references with invalid URI syntax when an object location cannot be constructed. */
  @Test
  public void testInvalidReference() {
    String reference = "data file.nc";
    assertThat(URLnaming.resolve(BASE, reference)).isEqualTo(reference);
    assertThat(URLnaming.resolveFile(BASE, reference)).isEqualTo(reference);
  }

  /** Preserve the null-base and null-reference return values. */
  @Test
  public void testNullReferences() {
    assertThat(URLnaming.resolve(BASE, null)).isNull();
    assertThat(URLnaming.resolveFile(BASE, null)).isNull();
    assertThat(URLnaming.resolve(null, "data.nc")).isEqualTo("data.nc");
    assertThat(URLnaming.resolveFile(null, "data.nc")).isEqualTo("data.nc");
  }
}
