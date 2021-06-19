package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public class TestReadGridDataset {

  @Test
  public void readGridIrregularTime() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "ncml/nc/cldc.mean.nc";
    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      System.out.println("readGridDataset: " + ncd.getLocation());

      Grid grid = ncd.findGrid("cldc").orElse(null);
      assertThat(grid).isNotNull();
      assertThat(grid.getUnits()).isEqualTo("okta");
      assertThat(grid.getDescription()).isEqualTo("Cloudiness Monthly Mean at Surface");
      assertThat(grid.toString()).contains("float cldc(time=456, lat=21, lon=360)");
      assertThat(grid.hasMissing()).isEqualTo(true);
      assertThat(grid.isMissing(0)).isEqualTo(false);
      // :valid_range = 0.0f, 8.0f; // float
      // :actual_range = 0.0f, 8.0f; // float
      assertThat(grid.isMissing(32766)).isEqualTo(true); // outside valid range TODO correct?
      assertThat(grid.isMissing(6553.100049)).isEqualTo(true); // with scale and offset

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.getName()).isEqualTo("time lat lon");
      assertThat(gcs.getHorizCoordSystem().isLatLon()).isTrue();
      assertThat(gcs.getXHorizAxis()).isNotNull();
      assertThat(gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getTimeAxis()).isNotNull();
      assertThat(gcs.getGridAxes()).hasSize(3);
      for (GridAxis axis : gcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }
      assertThat(gcs.showFnSummary()).isEqualTo("GRID(T,Y,X)");
      Formatter f = new Formatter();
      gcs.show(f, true);
      assertThat(f.toString()).contains("time (GridAxis1DTime) 715511");

      // LOOK this was "1960-01-01T00:00:00Z" in mixed gregorian
      CalendarDate wantDate = CalendarDate.fromUdunitIsoDate(null, "1960-01-03T00:00:00Z").orElseThrow();
      GridReferencedArray geoArray = grid.getReader().setTime(wantDate).read();
      Array<Number> data = geoArray.data();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(data.getRank()).isEqualTo(3);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 21, 360});

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordSystem().isLatLon()).isTrue();
      assertThat(mcs.getXHorizAxis()).isNotNull();
      assertThat(mcs.getYHorizAxis()).isNotNull();
      assertThat(mcs.getTimeAxis()).isNotNull();
      assertThat(mcs.getGridAxes()).hasSize(3);
      for (GridAxis axis : mcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(mcs.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(mcs.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1DTime time = mcs.getTimeAxis();
      assertThat(time.getNcoords()).isEqualTo(1);
      CalendarDate cd = time.getCalendarDate(0);
      // gregorian != proleptic_gregorian
      assertThat(cd.toString()).isEqualTo(wantDate.toString());
    }
  }

  @Test
  public void readGridRegularTime() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc";
    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      System.out.println("readGridDataset: " + ncd.getLocation());

      Grid grid = ncd.findGrid("Temperature_isobaric").orElse(null);
      assertThat(grid).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.getHorizCoordSystem().isLatLon()).isFalse();
      assertThat(gcs.getHorizCoordSystem().getProjection()).isNotNull();
      assertThat(gcs.getXHorizAxis()).isNotNull();
      assertThat(gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(gcs.getTimeAxis()).isNotNull();
      assertThat(gcs.getGridAxes()).hasSize(4);
      for (GridAxis axis : gcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      CalendarDate wantDate = CalendarDate.fromUdunitIsoDate(null, "2009-08-02T12:00:00Z").orElseThrow();
      GridReferencedArray geoArray = grid.getReader().setTime(wantDate).setVertCoord(700.0).read();
      Array<Number> data = geoArray.data();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(data.getRank()).isEqualTo(4);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 1, 39, 45});

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordSystem().isLatLon()).isFalse();
      assertThat(mcs.getXHorizAxis()).isNotNull();
      assertThat(mcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(mcs.getTimeAxis()).isNotNull();
      assertThat(mcs.getGridAxes()).hasSize(4);
      for (GridAxis axis : mcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(mcs.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(mcs.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1D vert = mcs.getVerticalAxis();
      assertThat(vert.getNcoords()).isEqualTo(1);
      assertThat(vert.getCoordMidpoint(0)).isEqualTo(700.0);

      GridAxis1DTime time = mcs.getTimeAxis();
      assertThat(time.getNcoords()).isEqualTo(1);
      CalendarDate cd = time.getCalendarDate(0);
      // gregorian != proleptic_gregorian
      assertThat(cd.toString()).isEqualTo(wantDate.toString());
    }
  }

  @Category(NeedsCdmUnitTest.class)
  @Test
  public void testNoTimeAxis() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/coards/inittest24.QRIDV07200.ncml";
    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      System.out.println("readGridDataset: " + ncd.getLocation());

      Grid grid = ncd.findGrid("QR").orElse(null);
      assertThat(grid).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.getHorizCoordSystem().isLatLon()).isTrue();
      assertThat(gcs.getHorizCoordSystem().getProjection()).isNotNull();
      assertThat(gcs.getXHorizAxis()).isNotNull();
      assertThat(gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(gcs.getTimeAxis()).isNull();
      assertThat(gcs.getGridAxes()).hasSize(3);
      for (GridAxis axis : gcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      CalendarDate wantDate = CalendarDate.fromUdunitIsoDate(null, "2009-08-02T12:00:00Z").orElseThrow();
      GridReferencedArray geoArray = grid.getReader().setTime(wantDate).setVertCoord(725.0).read();
      Array<Number> data = geoArray.data();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(data.getRank()).isEqualTo(3);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 50, 50});

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordSystem().isLatLon()).isTrue();
      assertThat(mcs.getXHorizAxis()).isNotNull();
      assertThat(mcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(mcs.getTimeAxis()).isNull();
      assertThat(mcs.getGridAxes()).hasSize(3);
      for (GridAxis axis : mcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(mcs.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(mcs.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1D vert = mcs.getVerticalAxis();
      assertThat(vert.getNcoords()).isEqualTo(1);
      assertThat(vert.getCoordMidpoint(0)).isEqualTo(725);
    }
  }

  @Category(NeedsCdmUnitTest.class)
  @Test
  public void testProblem() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc";
    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      System.out.println("readGridDataset: " + ncd.getLocation());

      Grid grid = ncd.findGrid("T").orElse(null);
      assertThat(grid).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.getHorizCoordSystem().isLatLon()).isTrue();
      assertThat(gcs.getHorizCoordSystem().getProjection()).isNotNull();
      assertThat(gcs.getXHorizAxis()).isNotNull();
      assertThat(gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(gcs.getTimeAxis()).isNotNull();
      assertThat(gcs.getGridAxes()).hasSize(4);
      for (GridAxis axis : gcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      CalendarDate wantDate = CalendarDate.fromUdunitIsoDate(null, "2003-02-14T06:00:00Z").orElseThrow();
      GridReferencedArray geoArray = grid.getReader().setTime(wantDate).setVertCoord(725.0).read();
      Array<Number> data = geoArray.data();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(data.getRank()).isEqualTo(4);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 1, 73, 73});

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordSystem().isLatLon()).isEqualTo(gcs.getHorizCoordSystem().isLatLon());
      assertThat(mcs.getXHorizAxis()).isNotNull();
      assertThat(mcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(mcs.getTimeAxis()).isNotNull();
      assertThat(mcs.getGridAxes()).hasSize(4);
      for (GridAxis axis : mcs.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(mcs.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(mcs.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1D vert = mcs.getVerticalAxis();
      assertThat(vert.getNcoords()).isEqualTo(1);
      assertThat(vert.getCoordMidpoint(0)).isEqualTo(700.);
    }
  }

  @Test
  public void testFileNotFound() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "conventions/fileNot.nc";
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      fail();
    } catch (FileNotFoundException e) {
      assertThat(e.getMessage()).contains("(No such file or directory)");
    }
  }

  @Test
  public void testFileNotGrid() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "point/point.ncml";
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNull();
    }
  }

}