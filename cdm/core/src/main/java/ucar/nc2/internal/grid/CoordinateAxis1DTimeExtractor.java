package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.ArrayChar;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

class CoordinateAxis1DTimeExtractor {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis1DTimeExtractor.class);

  final TimeHelper timeHelper;
  final List<CalendarDate> cdates;

  CoordinateAxis1DTimeExtractor(CoordinateAxis coordAxis, double[] values) {
    Preconditions.checkArgument(coordAxis.getRank() < 2);
    this.timeHelper = TimeHelper.factory(coordAxis.getUnitsString(), coordAxis.attributes());

    try {
      Formatter errMessages = new Formatter();
      if (coordAxis.getDataType() == DataType.CHAR) {
        cdates = makeTimesFromChar(coordAxis, errMessages);
      } else if (coordAxis.getDataType() == DataType.STRING) {
        cdates = makeTimesFromStrings(coordAxis, errMessages);
      } else {
        cdates = makeCalendarDateFromValues(values);
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
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
    CalendarDate cd = timeHelper.makeCalendarDateFromOffset(coordValue);
    if (cd == null) {
      if (errMessages != null) {
        errMessages.format("String time coordinate must be ISO formatted= %s%n", coordValue);
        log.info("Char time coordinate must be ISO formatted= {} file = {}", coordValue, org.getDatasetLocation());
      }
      throw new IllegalArgumentException();
    }
    return cd;
  }

  private List<CalendarDate> makeCalendarDateFromValues(double[] values) {
    int ncoords = values.length;
    ArrayList<CalendarDate> result = new ArrayList<>(ncoords);
    for (double val : values) {
      result.add(timeHelper.makeCalendarDateFromOffset(val));
    }
    return result;
  }
}
