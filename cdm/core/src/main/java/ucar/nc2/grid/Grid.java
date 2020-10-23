package ucar.nc2.grid;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.IsMissingEvaluator;

import java.io.IOException;

public interface Grid extends IsMissingEvaluator {

  String getName();

  String getUnitsString();

  String getDescription();

  String getCoordSysName();

  DataType getDataType();

  GridCoordinateSystem getCoordinateSystem();

  GridReferencedArray readData(GridSubset subset) throws IOException, InvalidRangeException;
}
