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
  public void readGridDataset() throws IOException, InvalidRangeException {
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
}
