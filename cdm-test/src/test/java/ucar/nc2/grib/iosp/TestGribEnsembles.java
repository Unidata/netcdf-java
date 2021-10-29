/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.iosp;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGribEnsembles {

  // from jitka
  @Test
  public void testWMOgrib2() throws Exception {

    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib";
    System.out.printf("Open %s%n", filename);
    Formatter errlog = new Formatter();
    try (NetcdfDataset datafile = NetcdfDatasets.openDataset(filename)) {
      GridDataset gridDataset = GridDatasetFactory.wrapGridDataset(datafile, errlog).orElseThrow();

      String variableName = "VAR_10-3-192_L1";
      Grid grid = gridDataset.findGridByAttribute(Grib.VARIABLE_ID_ATTNAME, variableName).orElseThrow();
      assertThat(grid.getCoordinateSystem().getNominalShape()).isEqualTo(ImmutableList.of(1, 477, 1, 207, 198));
    }

  }

  @Test
  public void testEcmwfEns() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB";
    System.out.printf("Open %s%n", filename);

    try (NetcdfDataset datafile = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      try (GridDataset gridDataset = GridDatasetFactory.wrapGridDataset(datafile, errlog).orElseThrow()) {
        String requiredName = "Total_precipitation_surface";
        Grid grid = gridDataset.findGrid(requiredName).orElseThrow();
        assertThat(grid.getName()).isEqualTo(requiredName);
        assertThat(grid.getCoordinateSystem().getNominalShape()).isEqualTo(ImmutableList.of(1, 1, 51, 21, 31));
      }
    }
  }

}
