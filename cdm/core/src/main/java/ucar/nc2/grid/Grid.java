package ucar.nc2.grid;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.IsMissingEvaluator;
import ucar.nc2.ft2.coverage.SubsetParams;

import java.io.IOException;

public interface Grid extends IsMissingEvaluator {
  GridCoordinateSystem getCoordinateSystem();

  String getName();

  String getUnitsString();

  String getDescription();

  String getCoordSysName();

  DataType getDataType();

  GridReferencedArray readData(SubsetParams subset) throws IOException, InvalidRangeException;
}
