/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.write.NcdumpArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Test {@link CoordinateAxis.Builder} */
public class TestCoordinateAxisBuilder {

  @Test
  public void testFromVariableDS() {
    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT)
        .setUnits("units").setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.GeoX);
    CoordinateAxis axis = builder.build(makeDummyGroup());

    assertThat(axis.getShortName()).isEqualTo("name");
    assertThat(axis.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(axis.getUnitsString()).isEqualTo("units");
    assertThat(axis.getDescription()).isEqualTo("desc");
    assertThat(axis.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(axis.findAttributeString(CDM.UNITS, "")).isEqualTo("units");
    assertThat(axis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");

    CoordinateAxis copy = axis.toBuilder().build(makeDummyGroup());
    assertThat(copy).isEqualTo(axis);
    assertThat(copy.hashCode()).isEqualTo(axis.hashCode());
  }

}
