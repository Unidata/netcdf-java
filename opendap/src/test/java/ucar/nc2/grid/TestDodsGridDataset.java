/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.InvalidRangeException;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsExternalResource;

import static com.google.common.truth.Truth.assertThat;

/** Test making a GridDataset from opendap dataset. */
public class TestDodsGridDataset {

  @Test
  @Category(NeedsExternalResource.class)
  public void testDodsSubset() throws IOException, InvalidRangeException {
    String filename = "dods://thredds.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/best";
    System.out.printf("open %s%n", filename);
    String gridName = "Pressure_surface";

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(coverage.getHorizCoordinateSystem().getShape()).isEqualTo(ImmutableList.of(65, 93));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      LatLonRect llbb = hcs.getLatLonBoundingBox();
      LatLonRect llbb_subset = LatLonRect.builder(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2).build();

      checkLatLonSubset(hcs, coverage, llbb_subset, new int[] {1, 35, 45});
    }
  }

  private void checkLatLonSubset(GridHorizCoordinateSystem hcs, Grid coverage, LatLonRect bbox, int[] expectedShape)
      throws IOException, InvalidRangeException {
    System.out.printf(" coverage llbb = %s width=%f%n", hcs.getLatLonBoundingBox().toString2(),
        hcs.getLatLonBoundingBox().getWidth());
    System.out.printf(" constrain bbox= %s width=%f%n", bbox.toString2(), bbox.getWidth());

    GridReferencedArray geoArray = coverage.getReader().setLatLonBoundingBox(bbox).setTimeLatest().read();
    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();
    System.out.printf(" data cs shape=%s%n", mcs.getMaterializedShape());
    System.out.printf(" data shape=%s%n", Arrays.toString(geoArray.data().getShape()));

    assertThat(geoArray.data().getShape()).isEqualTo(expectedShape);
    assertThat(mcs.getMaterializedShape()).isEqualTo(Ints.asList(expectedShape));
  }

}
