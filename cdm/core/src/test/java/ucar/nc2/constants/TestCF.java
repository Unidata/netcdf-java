/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.constants;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

/** Test {@link ucar.nc2.constants.CF} */
public class TestCF {

  @Test
  public void testFeatureConversion() {
    for (ucar.nc2.constants.FeatureType ft : ucar.nc2.constants.FeatureType.values()) {
      CF.FeatureType cff = CF.FeatureType.convert(ft);
      if (cff != null) {
        ucar.nc2.constants.FeatureType ft2 = CF.FeatureType.convert(cff);
        assertThat(ft).isEqualTo(ft2);
      }
    }

    for (CF.FeatureType cff : CF.FeatureType.values()) {
      ucar.nc2.constants.FeatureType ft = CF.FeatureType.convert(cff);
      if (ft != null) {
        CF.FeatureType cff2 = CF.FeatureType.convert(ft);
        assertThat(cff).isEqualTo(cff2);
      }
    }
  }

  @Test
  public void testGetFeatureType() {
    for (CF.FeatureType cff : CF.FeatureType.values()) {
      CF.FeatureType cff2 = CF.FeatureType.getFeatureType(cff.toString());
      assertWithMessage(cff.toString()).that(cff2).isNotNull();
      assertThat(cff).isEqualTo(cff2);
    }

    // case insensitive
    for (CF.FeatureType cff : CF.FeatureType.values()) {
      CF.FeatureType cff2 = CF.FeatureType.getFeatureType(cff.toString().toUpperCase());
      assertThat(cff2).isNotNull();
      assertThat(cff).isEqualTo(cff2);
    }
  }

}
