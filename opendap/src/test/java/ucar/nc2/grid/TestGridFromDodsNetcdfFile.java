/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test making a GridDataset from an opendap dataset. */
public class TestGridFromDodsNetcdfFile {

  @Test
  @Category(NeedsExternalResource.class)
  public void testDodsSubset() throws Exception {
    String filename = "dods://thredds.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/best";
    String gribId = "Pressure_surface";
    System.out.printf("open %s%n", filename);
    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gribId).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem cs = coverage.getCoordinateSystem();
      assertThat(cs).isNotNull();
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      LatLonRect llbb = hcs.getLatLonBoundingBox();
      LatLonRect bbox = new LatLonRect(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2);

      checkLatLonSubset(coverage, hcs, bbox, new int[] {35, 45});
    }
  }

  private void checkLatLonSubset(Grid coverage, GridHorizCoordinateSystem hcs, LatLonRect bbox, int[] expectedShape)
      throws Exception {
    System.out.printf(" coverage llbb = %s width=%f%n", hcs.getLatLonBoundingBox().toString2(),
        hcs.getLatLonBoundingBox().getWidth());
    System.out.printf(" constrain bbox= %s width=%f%n", bbox.toString2(), bbox.getWidth());

    GridReferencedArray garray = coverage.getReader().setLatLonBoundingBox(bbox).setTimePresent().read();
    MaterializedCoordinateSystem mcs = garray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();
    GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
    assertThat(hcs2).isNotNull();
    System.out.printf(" data cs shape=%s%n", hcs2.getShape());
    assertThat(hcs2.getShape()).isEqualTo(expectedShape);
  }

}
