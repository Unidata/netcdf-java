package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Variable;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

class ExtractTimeCoordinateValues {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExtractTimeCoordinateValues.class);

  @Nullable
  final List<CalendarDate> cdates; // non null only if its a string or char coordinate
  @Nullable
  final CoordinateAxis convertedAxis;

  ExtractTimeCoordinateValues(CoordinateAxis coordAxis) {
    Preconditions.checkArgument(coordAxis.getArrayType() == ArrayType.CHAR || coordAxis.getRank() < 2);

    Formatter errMessages = new Formatter();
    try {
      if (coordAxis.getArrayType() == ArrayType.CHAR) {
        cdates = makeTimesFromChar(coordAxis, errMessages);
      } else if (coordAxis.getArrayType() == ArrayType.STRING) {
        cdates = makeTimesFromStrings(coordAxis, errMessages);
      } else {
        cdates = null;
        this.convertedAxis = null;
        return;
      }
      this.convertedAxis = makeConvertedAxis(coordAxis);
    } catch (IOException ioe) {
      throw new RuntimeException(errMessages.toString(), ioe);
    }
  }

  CoordinateAxis getConvertedAxis() {
    return convertedAxis;
  }

  private CoordinateAxis makeConvertedAxis(CoordinateAxis coordAxis) {
    CoordinateAxis.Builder<?> builder = coordAxis.toBuilder();
    builder.removeAttribute(CDM.UNITS);
    builder.removeAttribute(CDM.MISSING_VALUE);
    builder.removeAttribute(CDM.SCALE_FACTOR);
    builder.removeAttribute(CDM.ADD_OFFSET);
    builder.removeAttribute(CDM.VALID_RANGE);
    builder.removeAttribute(CDM.VALID_MIN);
    builder.removeAttribute(CDM.VALID_MAX);

    CalendarDateUnit cdu = CalendarDateUnit.of(CalendarPeriod.Hour, false, cdates.get(0));
    int n = cdates.size();
    double[] parray = new double[n];
    int count = 0;
    for (CalendarDate cdate : cdates) {
      parray[count++] = cdu.makeFractionalOffsetFromRefDate(cdate);
    }

    builder.setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {n}, parray));
    builder.setArrayType(ArrayType.DOUBLE);
    builder.setUnits(cdu.toString());

    return builder.build(coordAxis.getParentGroup());
  }

  private List<CalendarDate> makeTimesFromChar(Variable org, Formatter errMessages) throws IOException {
    Preconditions.checkArgument(org.getArrayType() == ArrayType.CHAR);
    List<CalendarDate> result = new ArrayList<>();

    Array<?> data = org.readArray();
    Array<String> dateStrings = Arrays.makeStringsFromChar((Array<Byte>) data);
    for (String coordValue : dateStrings) {
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      result.add(cd);
    }
    return result;
  }

  private List<CalendarDate> makeTimesFromStrings(Variable org, Formatter errMessages) throws IOException {
    Preconditions.checkArgument(org.getArrayType() == ArrayType.STRING);
    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);

    Array<String> data = (Array<String>) org.readArray();
    for (String coordValue : data) {
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      result.add(cd);
    }
    return result;
  }

  private CalendarDate makeCalendarDateFromStringCoord(String coordValue, Variable org, Formatter errMessages) {
    try {
      return CalendarDate.fromUdunitIsoDate(null, coordValue).orElseThrow();
    } catch (Exception e) {
      errMessages.format("Bad time coordinate '%s' in dataset '%s'%n", coordValue, org.getDatasetLocation());
      log.info("Bad time coordinate '{}' in dataset '{}'", coordValue, org.getDatasetLocation());
      throw new IllegalArgumentException(errMessages.toString(), e);
    }
  }
}
