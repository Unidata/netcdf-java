package ucar.nc2.grid;

import java.io.Closeable;
import java.util.Formatter;

public interface GridDataset extends Closeable {

  String getName();

  String getLocation();

  Iterable<GridCoordinateSystem> getCoordSystems();

  Iterable<GridAxis> getCoordAxes();

  Iterable<Grid> getGrids();

  Grid findGrid(String name);

  void toString(Formatter f);

}
