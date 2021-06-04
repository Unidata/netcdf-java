package examples.featuretypes;

import com.google.common.collect.ImmutableList;
import ucar.array.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.test.TestLogger;

import ucar.nc2.grid.*;

import java.io.IOException;
import java.util.Formatter;
import java.util.Map;
import java.util.Optional;

public class GridDatasetsTutorial {
    // logs error/info message in memory and can be accessed from test functions
    public static TestLogger logger = TestLogger.TestLoggerFactory.getLogger();

    public static FeatureDataset gridDatasetFormat( FeatureType wantFeatureType, String yourLocationAsString, CancelTask task) throws NoFactoryFoundException, IOException {
        Formatter errlog = new Formatter();
        FeatureDataset fd = FeatureDatasetFactoryManager.open( wantFeatureType, yourLocationAsString, task, errlog);

        if (fd == null) {
            throw new NoFactoryFoundException( errlog.toString());
        } else {
            return fd;
        }
    }

    public static void gridFormat(String yourLocationAsString, Formatter errlog) throws java.io.IOException {
        GridDataset gds = ucar.nc2.grid.GridDatasetFactory.openGridDataset(yourLocationAsString, errlog);
    }

    public static void usingGridDataset(String yourLocationAsString, Formatter errlog) throws java.io.IOException {
        GridDataset gds = ucar.nc2.grid.GridDatasetFactory.openGridDataset(yourLocationAsString, errlog);
        GridCoordinateSystem gcs = (GridCoordinateSystem) gds.getGridCoordinateSystems();

        GridAxis xAxis = gcs.getXHorizAxis();
        GridAxis yAxis = gcs.getYHorizAxis();
        GridAxis1D zAxis = gcs.getVerticalAxis(); // may be null

        GridAxis1DTime tAxis1D = gcs.getTimeAxis(); // may be null
        ImmutableList<CalendarDate> dates = tAxis1D.getCalendarDates();
    }


    public static void findLatLonVal(String yourLocationAsString, Formatter errlog, double xPointAsDouble, double yPointAsDouble) throws IOException {
        // open the dataset, find the variable and its coordinate system
        GridDataset gds = ucar.nc2.grid.GridDatasetFactory.openGridDataset( yourLocationAsString, errlog);
        GridHorizCoordinateSystem gcs = (GridHorizCoordinateSystem) gds.getGridCoordinateSystems();

        // find the find the indices and coordinates of the horizontal 2D grid from the (x,y) projection point
        Optional<GridHorizCoordinateSystem.CoordReturn> xy = gcs.findXYindexFromCoord( xPointAsDouble, yPointAsDouble); // xy[0] = x, xy[1] = y
        System.out.println(xy.toString()); // prints x and y data indices and x and y grid coordinates

    }
    public static void readingData(GridSubset subsetAsGridSubset, Grid yourGrid) throws java.io.IOException, InvalidRangeException {
        GridReferencedArray data = yourGrid.readData( subsetAsGridSubset);
    }
    public static void CallMakeSubset(Map<String, String> stringMap) throws java.io.IOException {
        // puts entries from stringMap into a HashMap
        GridSubset subset = new GridSubset( stringMap);
    }
    }
