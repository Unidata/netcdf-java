package examples.featuretypes;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import ucar.ma2.Range;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.NoFactoryFoundException;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.test.TestLogger;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

public class GridDatasetsTutorial {
  // logs error/info message in memory and can be accessed from test functions
  public static TestLogger logger = TestLogger.TestLoggerFactory.getLogger();

  public static FeatureDataset gridDatasetFormat(FeatureType wantFeatureType,
      String yourLocationAsString, CancelTask task) throws NoFactoryFoundException, IOException {
    Formatter errlog = new Formatter();
    FeatureDataset fd =
        FeatureDatasetFactoryManager.open(wantFeatureType, yourLocationAsString, task, errlog);

    if (fd == null) {
      throw new NoFactoryFoundException(errlog.toString());
    } else {
      return fd;
    }
  }

  public static void gridFormat(String yourLocationAsString) throws java.io.IOException {
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(yourLocationAsString);
  }

  public static void usingGridDataset(String gridNameAsString, GridDataset yourGridDataset) {
    GridDatatype grid = yourGridDataset.findGridDatatype(gridNameAsString);
    GridCoordSystem yourGridCoordSystem = grid.getCoordinateSystem();
    CoordinateAxis xAxis = yourGridCoordSystem.getXHorizAxis();
    CoordinateAxis yAxis = yourGridCoordSystem.getYHorizAxis();
    CoordinateAxis1D zAxis = yourGridCoordSystem.getVerticalAxis(); // may be null

    if (yourGridCoordSystem.hasTimeAxis1D()) {
      CoordinateAxis1DTime tAxis1D = yourGridCoordSystem.getTimeAxis1D();
      List<CalendarDate> dates = tAxis1D.getCalendarDates();

    } else if (yourGridCoordSystem.hasTimeAxis()) {
      CoordinateAxis tAxis = yourGridCoordSystem.getTimeAxis();
    }
  }


  public static void findLatLonVal(String yourLocationAsString, Formatter errlog,
      double xPointAsDouble, double yPointAsDouble) throws IOException {
    // open the dataset, find the variable and its coordinate system
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(yourLocationAsString);
    GridDatatype grid = gds.findGridDatatype("myVariableName");
    GridCoordSystem gcs = grid.getCoordinateSystem();

    double lat = 8.0;
    double lon = 21.0;

    // find the x,y index for a specific lat/lon position
    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null); // xy[0] = x, xy[1] = y

    // read the data at that lat, lon and the first time and z level (if any)
    Array data = grid.readDataSlice(0, 0, xy[1], xy[0]); // note order is t, z, y, x
    double val = data.getDouble(0); // we know its a scalar
    System.out.printf("Value at %f %f == %f%n", lat, lon, val);
  }

  public static void readingData(GridDatatype yourGridDatatype, int time_index, int z_index,
      int x_index, int y_index) throws IOException {
    // get 2D data at timeIndex, levelIndex
    Array data = yourGridDatatype.readDataSlice(time_index, z_index, x_index, y_index);
  }

  public static void CallMakeSubset(GridDatatype yourGridDatatype, Range rt_range, Range ens_range,
      Range t_range, Range z_range, Range y_range, Range x_range) throws InvalidRangeException {
    GridDatatype subset =
        yourGridDatatype.makeSubset(rt_range, ens_range, t_range, z_range, y_range, x_range);

  }
}
