package ucar.nc2.filter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.iosp.netcdf3.N3iosp;

import java.util.*;

public class ConvertMissing implements Enhancement {

  private boolean hasValidMin, hasValidMax;
  private double validMin, validMax;

  private boolean hasFillValue;
  private double fillValue; // LOOK: making it double not really correct. What about CHAR?

  private boolean hasMissingValue;
  private double[] missingValue; // LOOK: also wrong to make double, for the same reason.

  // defaults from NetcdfDataset modes
  private boolean invalidDataIsMissing;
  private boolean fillValueIsMissing;
  private boolean missingDataIsMissing;

  public static ConvertMissing createFromVariable(VariableDS var) {
    // valid min and max
    DataType.Signedness signedness = var.getSignedness();
    double validMin = -Double.MAX_VALUE, validMax = Double.MAX_VALUE;
    boolean hasValidMin = false, hasValidMax = false;
    // assume here its in units of unpacked data. correct this below
    Attribute validRangeAtt = var.findAttribute(CDM.VALID_RANGE);
    DataType validType = null;
    if (validRangeAtt != null && !validRangeAtt.isString() && validRangeAtt.getLength() > 1) {
      validType = FilterHelpers.getAttributeDataType(validRangeAtt, signedness);
      validMin = var.convertUnsigned(validRangeAtt.getNumericValue(0), validType).doubleValue();
      validMax = var.convertUnsigned(validRangeAtt.getNumericValue(1), validType).doubleValue();
      hasValidMin = true;
      hasValidMax = true;
    }

    Attribute validMinAtt = var.findAttribute(CDM.VALID_MIN);
    Attribute validMaxAtt = var.findAttribute(CDM.VALID_MAX);

    // Only process the valid_min and valid_max attributes if valid_range isn't present.
    if (!hasValidMin) {
      if (validMinAtt != null && !validMinAtt.isString()) {
        validType = FilterHelpers.getAttributeDataType(validMinAtt, signedness);
        validMin = var.convertUnsigned(validMinAtt.getNumericValue(), validType).doubleValue();
        hasValidMin = true;
      }

      if (validMaxAtt != null && !validMaxAtt.isString()) {
        validType = FilterHelpers.largestOf(validType, FilterHelpers.getAttributeDataType(validMaxAtt, signedness));
        validMax = var.convertUnsigned(validMaxAtt.getNumericValue(), validType).doubleValue();
        hasValidMax = true;
      }
    }

    if (validMin > validMax) {
      double temp = validMin;
      validMin = validMax;
      validMax = temp;
    }

    /// fill_value
    double fillValue = getFillValueOrDefault(var);
    boolean hasFillValue = !Double.isNaN(fillValue);

    /// missing_value
    double[] missingValue = null;
    Attribute missingValueAtt = var.findAttribute(CDM.MISSING_VALUE);
    if (missingValueAtt != null) {
      if (missingValueAtt.isString()) {
        String svalue = missingValueAtt.getStringValue();
        if (var.getOriginalDataType() == DataType.CHAR) {
          missingValue = new double[1];
          if (svalue.isEmpty()) {
            missingValue[0] = 0;
          } else {
            missingValue[0] = svalue.charAt(0);
          }
        } else { // not a CHAR - try to fix problem where they use a numeric value as a String attribute
          try {
            missingValue = new double[1];
            missingValue[0] = Double.parseDouble(svalue);
          } catch (NumberFormatException ex) {
            // TODO add logger
          }
        }
      } else { // not a string
        missingValue = new double[missingValueAtt.getLength()];
        DataType missingType = FilterHelpers.getAttributeDataType(missingValueAtt, signedness);
        for (int i = 0; i < missingValue.length; i++) {
          missingValue[i] = var.convertUnsigned(missingValueAtt.getNumericValue(i), missingType).doubleValue();
        }
      }
    }
    return new ConvertMissing(var.fillValueIsMissing(), var.invalidDataIsMissing(), var.missingDataIsMissing(),
        hasValidMin, hasValidMax, validMin, validMax, hasFillValue, fillValue, missingValue);
  }


  public ConvertMissing(boolean fillValueIsMissing, boolean invalidDataIsMissing, boolean missingDataIsMissing,
      boolean hasValidMin, boolean hasValidMax, double validMin, double validMax, boolean hasFillValue,
      double fillValue, double[] missingValue) {
    this.fillValueIsMissing = fillValueIsMissing;
    this.invalidDataIsMissing = invalidDataIsMissing;
    this.missingDataIsMissing = missingDataIsMissing;
    this.hasValidMin = hasValidMin;
    this.hasValidMax = hasValidMax;
    this.validMin = validMin;
    this.validMax = validMax;
    this.hasFillValue = hasFillValue;
    this.fillValue = fillValue;
    this.missingValue = missingValue;
    this.hasMissingValue = false;
    // clean up missing values: remove NaNs, fill values, and values outside valid range
    if (this.missingDataIsMissing && this.missingValue != null) {
      List<Double> missing = new ArrayList();
      for (double mv : this.missingValue) {
        if (Double.isNaN(mv)) {
          continue;
        }
        if (fillValueIsMissing && hasFillValue && mv == fillValue) {
          continue;
        }
        if (invalidDataIsMissing && hasValidMin && mv < validMin) {
          continue;
        }
        if (invalidDataIsMissing && hasValidMax && mv > validMax) {
          continue;
        }
        missing.add(mv);
      }
      int nMissing = missing.size();
      this.missingValue = new double[nMissing];
      for (int i = 0; i < nMissing; i++) {
        this.missingValue[i] = missing.get(i);
      }
      this.hasMissingValue = this.missingValue.length > 0;
    }
  }

  public boolean hasValidData() {
    return hasValidMin || hasValidMax;
  }

  public double getValidMin() {
    return validMin;
  }

  public double getValidMax() {
    return validMax;
  }

  public boolean isInvalidData(double val) {
    if (Double.isNaN(val)) {
      return true;
    }
    if (val > validMax) {
      return true;
    }
    if (val < validMin) {
      return true;
    }
    return false;
  }

  public boolean hasFillValue() {
    return hasFillValue;
  }

  public boolean isFillValue(double val) {
    return hasFillValue && val == fillValue;
  }

  public double getFillValue() {
    return fillValue;
  }

  public boolean isMissingValue(double val) {
    for (double aMissingValue : missingValue) {
      if (val == aMissingValue) {
        return true;
      }
    }
    return false;
  }

  public double[] getMissingValues() {
    return missingValue;
  }

  public boolean hasMissingValue() {
    return hasMissingValue;
  }

  public boolean hasMissing() {
    return (invalidDataIsMissing && hasValidData()) || (fillValueIsMissing && hasFillValue())
        || (missingDataIsMissing && hasMissingValue());
  }

  public boolean isMissing(double val) {
    if (Double.isNaN(val)) {
      return true;
    }
    return (missingDataIsMissing && hasMissingValue && isMissingValue(val))
        || (fillValueIsMissing && hasFillValue && isFillValue(val))
        || (invalidDataIsMissing && hasValidData() && isInvalidData(val));
  }

  @Deprecated
  public void setFillValueIsMissing(boolean b) {
    this.fillValueIsMissing = b;
  }

  @Deprecated
  public void setInvalidDataIsMissing(boolean b) {
    this.invalidDataIsMissing = b;
  }

  @Deprecated
  public void setMissingDataIsMissing(boolean b) {
    this.missingDataIsMissing = b;
  }

  public double convert(double value) {
    return isMissing(value) ? Double.NaN : value;
  }

  public Array convertMissing(Array in) {
    DataType type = in.getDataType();
    if (!type.isNumeric()) {
      return in;
    }
    if (!hasMissing()) {
      return in;
    }

    Array out = Array.factory(type, in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();

    // iterate and convert elements
    while (iterIn.hasNext()) {
      Number value = (Number) iterIn.getObjectNext();
      value = convert(value.doubleValue());
      iterOut.setObjectNext(value);
    }

    return out;
  }

  public static double getFillValueOrDefault(VariableDS var) {
    // need fill value info before convertMissing
    Attribute fillValueAtt = var.findAttribute(CDM.FILL_VALUE);
    if (fillValueAtt != null && !fillValueAtt.isString()) {
      DataType fillType = FilterHelpers.getAttributeDataType(fillValueAtt, var.getSignedness());
      return var.convertUnsigned(fillValueAtt.getNumericValue(), fillType).doubleValue();
    }
    // No _FillValue attribute found. Instead, if file is NetCDF and variable is numeric, use the default fill value.
    String ncfileId = var.getOriginalVariable() == null ? null : var.getOriginalVariable().getFileTypeId();
    if (DataFormatType.NETCDF.getDescription().equals(ncfileId)
        || DataFormatType.NETCDF4.getDescription().equals(ncfileId)) {
      DataType fillType = var.getDataType();
      if (fillType.isNumeric()) {
        return var.convertUnsigned(N3iosp.getFillValueDefault(fillType), fillType).doubleValue();
      }
    }
    return Double.NaN;
  }
}
