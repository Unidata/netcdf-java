package ucar.nc2.filter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Misc;

public class ConvertMissing {

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

    // check if validData values are stored packed or unpacked
    if (hasValidMin || hasValidMax) {
      if (FilterHelpers.rank(validType) == FilterHelpers.rank(var.getScaledOffsetType())
          && FilterHelpers.rank(validType) > FilterHelpers.rank(var.getOriginalDataType())) {
        // If valid_range is the same type as the wider of scale_factor and add_offset, PLUS
        // it is wider than the (packed) data, we know that the valid_range values were stored as unpacked.
        // We already assumed that this was the case when we first read the attribute values, so there's
        // nothing for us to do here.
      } else {
        // Otherwise, the valid_range values were stored as packed. So now we must unpack them.
        if (hasValidMin) {
          validMin = var.applyScaleOffset(validMin);
        }
        if (hasValidMax) {
          validMax = var.applyScaleOffset(validMax);
        }
      }
      // During the scaling process, it is possible that the valid minimum and maximum values have effectively been
      // swapped (for example, when the scale value is negative). Go ahead and check to make sure the valid min is
      // actually less than the valid max, and if not, fix it. See https://github.com/Unidata/netcdf-java/issues/572.
      if (validMin > validMax) {
        double tmp = validMin;
        validMin = validMax;
        validMax = tmp;
      }
    }

    /// fill_value
    boolean hasFillValue = var.hasFillValue();
    double fillValue = var.getFillValue();

    /// missing_value
    double[] missingValue = null;
    boolean hasMissingValue = false;
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
        hasMissingValue = true;
      } else { // not a string
        missingValue = new double[missingValueAtt.getLength()];
        DataType missingType = FilterHelpers.getAttributeDataType(missingValueAtt, signedness);
        for (int i = 0; i < missingValue.length; i++) {
          missingValue[i] = var.convertUnsigned(missingValueAtt.getNumericValue(i), missingType).doubleValue();
          missingValue[i] = var.applyScaleOffset(missingValue[i]);
        }

        for (double mv : missingValue) {
          if (!Double.isNaN(mv)) {
            hasMissingValue = true; // dont need to do anything if it's already a NaN
            break;
          }
        }
      }
    }
    return new ConvertMissing(var.fillValueIsMissing(), var.invalidDataIsMissing(), var.missingDataIsMissing(),
        hasValidMin, hasValidMax, validMin, validMax, hasFillValue, fillValue, hasMissingValue, missingValue);
  }


  public ConvertMissing(boolean fillValueIsMissing, boolean invalidDataIsMissing, boolean missingDataIsMissing,
      boolean hasValidMin, boolean hasValidMax, double validMin, double validMax, boolean hasFillValue,
      double fillValue, boolean hasMissingValue, double[] missingValue) {
    this.fillValueIsMissing = fillValueIsMissing;
    this.invalidDataIsMissing = invalidDataIsMissing;
    this.missingDataIsMissing = missingDataIsMissing;
    this.hasValidMin = hasValidMin;
    this.hasValidMax = hasValidMax;
    this.validMin = validMin;
    this.validMax = validMax;
    this.hasFillValue = hasFillValue;
    this.fillValue = fillValue;
    this.hasMissingValue = hasMissingValue;
    this.missingValue = missingValue;
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
    // valid_min and valid_max may have been multiplied by scale_factor, which could be a float, not a double.
    // That potential loss of precision means that we cannot do the nearlyEquals() comparison with
    // Misc.defaultMaxRelativeDiffDouble.
    boolean greaterThanOrEqualToValidMin =
        Misc.nearlyEquals(val, validMin, Misc.defaultMaxRelativeDiffFloat) || val > validMin;
    boolean lessThanOrEqualToValidMax =
        Misc.nearlyEquals(val, validMax, Misc.defaultMaxRelativeDiffFloat) || val < validMax;

    return (hasValidMin && !greaterThanOrEqualToValidMin) || (hasValidMax && !lessThanOrEqualToValidMax);
  }

  public boolean hasFillValue() {
    return hasFillValue;
  }

  public boolean isFillValue(double val) {
    return hasFillValue && Misc.nearlyEquals(val, fillValue, Misc.defaultMaxRelativeDiffFloat);
  }

  public double getFillValue() {
    return fillValue;
  }

  public boolean isMissingValue(double val) {
    if (!hasMissingValue) {
      return false;
    }
    for (double aMissingValue : missingValue) {
      if (Misc.nearlyEquals(val, aMissingValue, Misc.defaultMaxRelativeDiffFloat)) {
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
    } else {
      return (missingDataIsMissing && isMissingValue(val)) || (fillValueIsMissing && isFillValue(val))
          || (invalidDataIsMissing && isInvalidData(val));
    }
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

  public Number convertMissing(Number value) {
    return isMissing(value.doubleValue()) ? Double.NaN : value;
  }

  public Array convertMissing(Array in) {
    DataType type = in.getDataType();
    if (!type.isNumeric()) {
      return in;
    }

    Array out = Array.factory(type, in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();

    // iterate and convert elements
    while (iterIn.hasNext()) {
      Number value = (Number) iterIn.getObjectNext();
      value = convertMissing(value);
      iterOut.setObjectNext(value);
    }

    return out;
  }
}
