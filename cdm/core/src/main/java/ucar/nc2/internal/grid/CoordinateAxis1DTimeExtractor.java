package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.ArrayChar;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

class CoordinateAxis1DTimeExtractor {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis1DTimeExtractor.class);

  final TimeHelper timeHelper;
  @Nullable
  final List<CalendarDate> cdates; // non null only if its a string or char coordinate

  CoordinateAxis1DTimeExtractor(CoordinateAxis coordAxis) {
    Preconditions.checkArgument(coordAxis.getDataType() == DataType.CHAR || coordAxis.getRank() < 2);
    this.timeHelper = TimeHelper.factory(coordAxis.getUnitsString(), coordAxis.attributes());

    Formatter errMessages = new Formatter();
    try {
      if (coordAxis.getDataType() == DataType.CHAR) {
        cdates = makeTimesFromChar(coordAxis, errMessages);
      } else if (coordAxis.getDataType() == DataType.STRING) {
        cdates = makeTimesFromStrings(coordAxis, errMessages);
      } else {
        cdates = null;
      }
    } catch (IOException ioe) {
      throw new RuntimeException(errMessages.toString(), ioe);
    }
  }

  private List<CalendarDate> makeTimesFromChar(Variable org, Formatter errMessages) throws IOException {
    Preconditions.checkArgument(org.getDataType() == DataType.CHAR);
    List<CalendarDate> result = new ArrayList<>();

    ArrayChar data = (ArrayChar) org.readArray();
    ucar.array.Array<String> dateStrings = data.makeStringsFromChar();
    for (String coordValue : dateStrings) {
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      result.add(cd);
    }
    return result;
  }

  private List<CalendarDate> makeTimesFromStrings(Variable org, Formatter errMessages) throws IOException {
    Preconditions.checkArgument(org.getDataType() == DataType.STRING);
    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);

    ucar.array.Array<String> data = (ucar.array.Array<String>) org.readArray();
    for (String coordValue : data) {
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      result.add(cd);
    }
    return result;
  }

  private CalendarDate makeCalendarDateFromStringCoord(String coordValue, Variable org, Formatter errMessages) {
    try {
      return timeHelper.makeCalendarDateFromOffset(coordValue);
    } catch (IllegalArgumentException e) {
      errMessages.format("Bad time coordinate '%s' in dataset '%s'%n", coordValue, org.getDatasetLocation());
      log.info("Bad time coordinate '{}' in dataset '{}'", coordValue, org.getDatasetLocation());
      throw new RuntimeException(errMessages.toString(), e);
    }
  }
}
