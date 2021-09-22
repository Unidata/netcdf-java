/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation. Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.grib;

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
