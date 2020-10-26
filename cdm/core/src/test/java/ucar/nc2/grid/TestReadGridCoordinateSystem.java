package ucar.nc2.grid;

import org.junit.Test;
import ucar.array.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

public class TestReadGridCoordinateSystem {

  @Test
  public void readGridIrregularTime() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "ncml/nc/cldc.mean.nc";
    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      System.out.println("readGridDataset: " + ncd.getLocation());

      Grid grid = ncd.findGrid("cldc").orElse(null);
      assertThat(grid).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.isLatLon()).isTrue();
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

      GridSubset subset = new GridSubset();
      CalendarDate wantDate = CalendarDate.parseISOformat(null, "1960-01-01T00:00:00Z");
      subset.setTime(wantDate);
      GridReferencedArray geoArray = grid.readData(subset);
      Array<Number> data = geoArray.data();
      assertThat(data.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(data.getRank()).isEqualTo(3);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 21, 360});

      GridCoordinateSystem csSubset = geoArray.csSubset();
      assertThat(csSubset).isNotNull();
      assertThat(csSubset.isLatLon()).isTrue();
      assertThat(csSubset.getXHorizAxis()).isNotNull();
      assertThat(csSubset.getYHorizAxis()).isNotNull();
      assertThat(csSubset.getTimeAxis()).isNotNull();
      assertThat(csSubset.getGridAxes()).hasSize(3);
      for (GridAxis axis : csSubset.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(csSubset.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(csSubset.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1DTime time = csSubset.getTimeAxis();
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
      assertThat(gcs.isLatLon()).isFalse();
      assertThat(gcs.getProjection()).isNotNull();
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

      GridSubset subset = new GridSubset();
      CalendarDate wantDate = CalendarDate.parseISOformat(null, "2009-08-02T12:00:00Z");
      subset.setTime(wantDate);
      subset.setVertCoord(700.0);
      GridReferencedArray geoArray = grid.readData(subset);
      Array<Number> data = geoArray.data();
      assertThat(data.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(data.getRank()).isEqualTo(4);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 1, 39, 45});

      GridCoordinateSystem csSubset = geoArray.csSubset();
      assertThat(csSubset).isNotNull();
      assertThat(csSubset.isLatLon()).isFalse();
      assertThat(csSubset.getXHorizAxis()).isNotNull();
      assertThat(csSubset.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(csSubset.getTimeAxis()).isNotNull();
      assertThat(csSubset.getGridAxes()).hasSize(4);
      for (GridAxis axis : csSubset.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(csSubset.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(csSubset.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1D vert = csSubset.getVerticalAxis();
      assertThat(vert.getNcoords()).isEqualTo(1);
      assertThat(vert.getCoordMidpoint(0)).isEqualTo(700.0);

      GridAxis1DTime time = csSubset.getTimeAxis();
      assertThat(time.getNcoords()).isEqualTo(1);
      CalendarDate cd = time.getCalendarDate(0);
      // gregorian != proleptic_gregorian
      assertThat(cd.toString()).isEqualTo(wantDate.toString());
    }
  }

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
      assertThat(gcs.isLatLon()).isTrue();
      assertThat(gcs.getProjection()).isNotNull();
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

      GridSubset subset = new GridSubset();
      CalendarDate wantDate = CalendarDate.parseISOformat(null, "2009-08-02T12:00:00Z");
      subset.setTime(wantDate);
      subset.setVertCoord(725.0);
      GridReferencedArray geoArray = grid.readData(subset);
      Array<Number> data = geoArray.data();
      assertThat(data.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(data.getRank()).isEqualTo(3);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 50, 50});

      GridCoordinateSystem csSubset = geoArray.csSubset();
      assertThat(csSubset).isNotNull();
      assertThat(csSubset.isLatLon()).isTrue();
      assertThat(csSubset.getXHorizAxis()).isNotNull();
      assertThat(csSubset.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(csSubset.getTimeAxis()).isNull();
      assertThat(csSubset.getGridAxes()).hasSize(3);
      for (GridAxis axis : csSubset.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(csSubset.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(csSubset.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1D vert = csSubset.getVerticalAxis();
      assertThat(vert.getNcoords()).isEqualTo(1);
      assertThat(vert.getCoordMidpoint(0)).isEqualTo(725);
    }
  }

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
      assertThat(gcs.isLatLon()).isTrue();
      assertThat(gcs.getProjection()).isNotNull();
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

      GridSubset subset = new GridSubset();
      CalendarDate wantDate = CalendarDate.parseISOformat(null, "2003-02-14T06:00:00Z");
      subset.setTime(wantDate);
      subset.setVertCoord(775.);
      GridReferencedArray geoArray = grid.readData(subset);
      Array<Number> data = geoArray.data();
      assertThat(data.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(data.getRank()).isEqualTo(4);
      assertThat(data.getShape()).isEqualTo(new int[] {1, 1, 73, 73});

      GridCoordinateSystem csSubset = geoArray.csSubset();
      assertThat(csSubset).isNotNull();
      assertThat(csSubset.isLatLon()).isEqualTo(gcs.isLatLon());
      assertThat(csSubset.getXHorizAxis()).isNotNull();
      assertThat(csSubset.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(csSubset.getTimeAxis()).isNotNull();
      assertThat(csSubset.getGridAxes()).hasSize(4);
      for (GridAxis axis : csSubset.getGridAxes()) {
        assertThat(axis).isInstanceOf(GridAxis1D.class);
        if (axis.getAxisType().isTime()) {
          assertThat(axis).isInstanceOf(GridAxis1DTime.class);
        }
      }

      assertThat(csSubset.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat(csSubset.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis1D vert = csSubset.getVerticalAxis();
      assertThat(vert.getNcoords()).isEqualTo(1);
      assertThat(vert.getCoordMidpoint(0)).isEqualTo(700.);
    }
  }


}
