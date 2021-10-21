/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset;

import java.util.Formatter;
import java.util.Set;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.util.Misc;
import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;

import static ucar.array.ArrayType.DOUBLE;
import static ucar.array.ArrayType.INT;
import static ucar.array.ArrayType.SHORT;
import static ucar.array.ArrayType.UINT;
import static ucar.array.ArrayType.USHORT;
import static ucar.array.ArrayType.LONG;
import static ucar.array.ArrayType.ULONG;

/**
 * Implementation of EnhanceScaleMissingUnsigned for unsigned data, scale/offset packed data, and missing data.
 *
 * A Variable decorator that handles unsigned data, scale/offset packed data, and missing data.
 * Specifically, it handles:
 * <ul>
 * <li>unsigned data using {@code _Unsigned}</li>
 * <li>packed data using {@code scale_factor} and {@code add_offset}</li>
 * <li>invalid/missing data using {@code valid_min}, {@code valid_max}, {@code valid_range}, {@code missing_value},
 * or {@code _FillValue}</li>
 * </ul>
 * if those "standard attributes" are present.
 *
 * <h2>Standard Use</h2>
 *
 * <h3>Implementation rules for unsigned data</h3>
 *
 * <ol>
 * <li>A variable is considered unsigned if it has an {@link ArrayType#isUnsigned() unsigned data type} or an
 * {@code _Unsigned} attribute with value {@code true}.</li>
 * <li>Values will be {@link ArrayType#widenNumber widened}, which effectively reinterprets signed data as unsigned
 * data.</li>
 * <li>To accommodate the unsigned conversion, the variable's data type will be changed to the
 * {@link #nextLarger(ArrayType)} next larger type.</li>
 * </ol>
 *
 * <h3>Implementation rules for scale/offset</h3>
 *
 * <ol>
 * <li>If scale_factor and/or add_offset variable attributes are present, then this is a "packed" Variable.</li>
 * <li>The data type of the variable will be set to the {@link #largestOf(ArrayType...)} largest of:
 * <ul>
 * <li>the original data type</li>
 * <li>the unsigned conversion type, if applicable</li>
 * <li>the {@code scale_factor} attribute type</li>
 * <li>the {@code add_offset} attribute type</li>
 * </ul>
 * The signedness of the variable's data type will be preserved. For example, if the variable was originally
 * unsigned, then {@link #getScaledOffsetType()} will be unsigned as well.
 * </li>
 * <li>External (packed) data is converted to internal (unpacked) data transparently during the
 * {@link #applyScaleOffset(Array)} call.</li>
 * </ol>
 *
 * <h3>Implementation rules for missing data</h3>
 *
 * Here "missing data" is a general name for invalid/never-written/missing values. Use this interface when you don't
 * need to distinguish these variants. See below for a lower-level interface if you do need to distinguish between them.
 *
 * <ol>
 * <li>By default, hasMissing() is true if any of hasValidData(), hasFillValue() or hasMissingValue() are true
 * (see below). You can modify this behavior by calling setInvalidDataIsMissing(), setFillValueIsMissing(), or
 * setMissingDataIsMissing().</li>
 * <li>Test specific values through isMissing(double). Note that the data is converted and compared as a double.</li>
 * <li>Data values of float or double NaN are considered missing data and will return true if called with
 * isMissing(). (However isMissing() will not detect if you are setting NaNs yourself).</li>
 * </ol>
 *
 * <h3>Implementation rules for missing data with scale/offset</h3>
 *
 * <ol>
 * <li>_FillValue and missing_value values are always in the units of the external (packed) data.</li>
 * <li>If valid_range is the same type as scale_factor (actually the wider of scale_factor and add_offset) and this
 * is wider than the external data, then it will be interpreted as being in the units of the internal (unpacked)
 * data. Otherwise it is in the units of the external (packed) data.</li>
 * </ol>
 *
 * <h2>Low Level Access</h2>
 *
 * The following provide more direct access to missing/invalid data. These are mostly convenience routines for
 * checking the standard attributes. If you set useNaNs = true in the constructor, these routines cannot be used when
 * the data has type float or double.
 *
 * <h3>Implementation rules for valid_range</h3>
 *
 * <ol>
 * <li>If valid_range is present, valid_min and valid_max attributes are ignored. Otherwise, the valid_min and/or
 * valid_max is used to construct a valid range. If any of these exist, hasValidData() is true.</li>
 * <li>To test a specific value, call isInvalidData(). Note that the data is converted and compared as a double. Or
 * get the range through getValidMin() and getValidMax().</li>
 * </ol>
 *
 * <h3>Implementation rules for _FillData</h3>
 *
 * <ol>
 * <li>If the _FillData attribute is present, it must have a scalar value of the same type as the data. In this
 * case, hasFillValue() returns true.</li>
 * <li>Test specific values through isFillValue(). Note that the data is converted and compared as a double.</li>
 * </ol>
 *
 * <h3>Implementation rules for missing_value</h3>
 *
 * <ol>
 * <li>If the missing_value attribute is present, it must have a scalar or vector value of the same type as the
 * data. In this case, hasMissingValue() returns true.</li>
 * <li>Test specific values through isMissingValue(). Note that the data is converted and compared as a double.</li>
 * </ol>
 *
 * <h3>Strategies for using EnhanceScaleMissingUnsigned</h3>
 *
 * <ol>
 * <li>Low-level: use the is/has InvalidData/FillValue/MissingValue routines to "roll-your own" tests for the
 * various kinds of missing/invalid data.</li>
 * <li>Standard: use is/hasMissing() to test for missing data when you don't need to distinguish between the
 * variants. Use the setXXXisMissing() to customize the behavior if needed.</li>
 * </ol>
 */
public class EnhanceScaleMissingUnsigned {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ArrayType origArrayType;
  private ArrayType unsignedConversionType;
  private ArrayType scaledOffsetType;

  // defaults from NetcdfDataset modes
  private boolean invalidDataIsMissing;
  private boolean fillValueIsMissing;
  private boolean missingDataIsMissing;

  private boolean useScaleOffset;
  private double scale = 1.0, offset;

  private boolean hasValidRange, hasValidMin, hasValidMax;
  private double validMin = -Double.MAX_VALUE, validMax = Double.MAX_VALUE;

  private boolean hasFillValue;
  private double fillValue; // LOOK: making it double not really correct. What about CHAR?

  private boolean hasMissingValue;
  private double[] missingValue; // LOOK: also wrong to make double, for the same reason.

  private ArrayType.Signedness signedness;

  /**
   * Constructor, default values.
   *
   * @param forVar the Variable to decorate.
   */
  public EnhanceScaleMissingUnsigned(VariableSimpleIF forVar, Set<Enhance> enhancements) {
    this(forVar, enhancements, NetcdfDataset.fillValueIsMissing, NetcdfDataset.invalidDataIsMissing,
        NetcdfDataset.missingDataIsMissing);
  }

  /**
   * Constructor.
   * If scale/offset attributes are found, remove them from the decorated variable.
   *
   * @param forVar the Variable to decorate.
   * @param fillValueIsMissing use _FillValue for isMissing()
   * @param invalidDataIsMissing use valid_range for isMissing()
   * @param missingDataIsMissing use missing_value for isMissing()
   */
  public EnhanceScaleMissingUnsigned(VariableSimpleIF forVar, Set<Enhance> enhancements, boolean fillValueIsMissing,
      boolean invalidDataIsMissing, boolean missingDataIsMissing) {
    this.fillValueIsMissing = fillValueIsMissing;
    this.invalidDataIsMissing = invalidDataIsMissing;
    this.missingDataIsMissing = missingDataIsMissing;

    this.origArrayType = forVar.getArrayType();
    this.unsignedConversionType = origArrayType;

    // unsignedConversionType is initialized to origArrayType, and origArrayType may be a non-integral type that doesn't
    // have an "unsigned flavor" (such as FLOAT and DOUBLE). Furthermore, unsignedConversionType may start out as
    // integral, but then be widened to non-integral (i.e. LONG -> DOUBLE). For these reasons, we cannot rely upon
    // unsignedConversionType to store the signedness of the variable. We need a separate field.
    this.signedness = origArrayType.getSignedness();

    // In the event of conflict, "unsigned" wins. Potential conflicts include:
    // 1. origArrayType is unsigned, but variable has "_Unsigned == false" attribute.
    // 2. origArrayType is signed, but variable has "_Unsigned == true" attribute.
    if (signedness == ArrayType.Signedness.SIGNED) {
      String unsignedAtt = forVar.attributes().findAttributeString(CDM.UNSIGNED, null);
      if (unsignedAtt != null && unsignedAtt.equalsIgnoreCase("true")) {
        this.signedness = ArrayType.Signedness.UNSIGNED;
      }
    }

    if (signedness == ArrayType.Signedness.UNSIGNED) {
      // We may need a larger data type to hold the results of the unsigned conversion.
      this.unsignedConversionType = nextLarger(origArrayType).withSignedness(ArrayType.Signedness.UNSIGNED);
      logger.debug("assign unsignedConversionType = {}", unsignedConversionType);
    }

    ArrayType scaleType = null, offsetType = null, validType = null;
    logger.debug("{} for Variable = {}", getClass().getSimpleName(), forVar.getShortName());

    Attribute scaleAtt = forVar.attributes().findAttribute(CDM.SCALE_FACTOR);
    if (scaleAtt != null && !scaleAtt.isString()) {
      scaleType = getAttributeArrayType(scaleAtt);
      scale = convertUnsigned(scaleAtt.getNumericValue(), scaleType).doubleValue();
      useScaleOffset = enhancements.contains(Enhance.ApplyScaleOffset);
      logger.debug("scale = {}  type = {}", scale, scaleType);
    }

    Attribute offsetAtt = forVar.attributes().findAttribute(CDM.ADD_OFFSET);
    if (offsetAtt != null && !offsetAtt.isString()) {
      offsetType = getAttributeArrayType(offsetAtt);
      offset = convertUnsigned(offsetAtt.getNumericValue(), offsetType).doubleValue();
      useScaleOffset = enhancements.contains(Enhance.ApplyScaleOffset);
      logger.debug("offset = {}", offset);
    }

    ////// missing data : valid_range. assume here its in units of unpacked data. correct this below
    Attribute validRangeAtt = forVar.attributes().findAttribute(CDM.VALID_RANGE);
    if (validRangeAtt != null && !validRangeAtt.isString() && validRangeAtt.getLength() > 1) {
      validType = getAttributeArrayType(validRangeAtt);
      validMin = convertUnsigned(validRangeAtt.getNumericValue(0), validType).doubleValue();
      validMax = convertUnsigned(validRangeAtt.getNumericValue(1), validType).doubleValue();
      hasValidRange = true;
      logger.debug("valid_range = {}  {}", validMin, validMax);
    }

    Attribute validMinAtt = forVar.attributes().findAttribute(CDM.VALID_MIN);
    Attribute validMaxAtt = forVar.attributes().findAttribute(CDM.VALID_MAX);

    // Only process the valid_min and valid_max attributes if valid_range isn't present.
    if (!hasValidRange) {
      if (validMinAtt != null && !validMinAtt.isString()) {
        validType = getAttributeArrayType(validMinAtt);
        validMin = convertUnsigned(validMinAtt.getNumericValue(), validType).doubleValue();
        hasValidMin = true;
        logger.debug("valid_min = {}", validMin);
      }

      if (validMaxAtt != null && !validMaxAtt.isString()) {
        validType = largestOf(validType, getAttributeArrayType(validMaxAtt));
        validMax = convertUnsigned(validMaxAtt.getNumericValue(), validType).doubleValue();
        hasValidMax = true;
        logger.debug("valid_min = {}", validMax);
      }

      if (hasValidMin && hasValidMax) {
        hasValidRange = true;
      }
    }

    /// _FillValue
    Attribute fillValueAtt = forVar.attributes().findAttribute(CDM.FILL_VALUE);
    if (fillValueAtt != null && !fillValueAtt.isString()) {
      ArrayType fillType = getAttributeArrayType(fillValueAtt);
      if (fillType.isNumeric()) {
        fillValue = convertUnsigned(fillValueAtt.getNumericValue(), fillType).doubleValue();
        fillValue = applyScaleOffset(fillValue); // This will fail when _FillValue is CHAR.
        hasFillValue = true;
      }
    } else {
      // No _FillValue attribute found. Instead, use NetCDF default fill value.
      if (unsignedConversionType.isNumeric()) {
        Number fill = NetcdfFormatUtils.getFillValueDefault(unsignedConversionType);
        if (fill != null) {
          fillValue = applyScaleOffset(fill);
          hasFillValue = true;
        }
      }
    }

    /// missing_value
    Attribute missingValueAtt = forVar.attributes().findAttribute(CDM.MISSING_VALUE);
    if (missingValueAtt != null) {
      if (missingValueAtt.isString()) {
        String svalue = missingValueAtt.getStringValue();
        if (origArrayType == ArrayType.CHAR) {
          missingValue = new double[1];
          if (Strings.isNullOrEmpty(svalue)) {
            missingValue[0] = 0;
          } else {
            missingValue[0] = svalue.charAt(0);
          }

          hasMissingValue = true;
          // not a CHAR - try to fix problem where they use a numeric value as a String attribute
        } else if (!Strings.isNullOrEmpty(svalue)) {
          try {
            missingValue = new double[1];
            missingValue[0] = Double.parseDouble(svalue);
            hasMissingValue = true;
          } catch (NumberFormatException ex) {
            logger.debug("String missing_value not parseable as double = {}", missingValueAtt.getStringValue());
          }
        }
      } else { // not a string
        ArrayType missType = getAttributeArrayType(missingValueAtt);

        missingValue = new double[missingValueAtt.getLength()];
        for (int i = 0; i < missingValue.length; i++) {
          missingValue[i] = convertUnsigned(missingValueAtt.getNumericValue(i), missType).doubleValue();
          missingValue[i] = applyScaleOffset(missingValue[i]);
        }
        logger.debug("missing_data: {}", java.util.Arrays.toString(missingValue));

        for (double mv : missingValue) {
          if (!Double.isNaN(mv)) {
            hasMissingValue = true; // dont need to do anything if its already a NaN
            break;
          }
        }
      }
    }

    /// assign convertedArrayType if needed
    if (useScaleOffset) {
      scaledOffsetType = largestOf(unsignedConversionType, scaleType, offsetType).withSignedness(signedness);
      logger.debug("assign scaledOffsetType = {}", scaledOffsetType);

      // validData may be packed or unpacked
      if (hasValidData()) {
        if (rank(validType) == rank(largestOf(scaleType, offsetType))
            && rank(validType) > rank(unsignedConversionType)) {
          // If valid_range is the same type as the wider of scale_factor and add_offset, PLUS
          // it is wider than the (packed) data, we know that the valid_range values were stored as unpacked.
          // We already assumed that this was the case when we first read the attribute values, so there's
          // nothing for us to do here.
        } else {
          // Otherwise, the valid_range values were stored as packed. So now we must unpack them.
          if (hasValidRange || hasValidMin) {
            validMin = applyScaleOffset(validMin);
          }
          if (hasValidRange || hasValidMax) {
            validMax = applyScaleOffset(validMax);
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
    }
  }

  // Get the data type of an attribute. Make it unsigned if the variable is unsigned.
  private ArrayType getAttributeArrayType(Attribute attribute) {
    ArrayType dataType = attribute.getArrayType();
    if (signedness == ArrayType.Signedness.UNSIGNED) {
      // If variable is unsigned, make its integral attributes unsigned too.
      dataType = dataType.withSignedness(signedness);
    }
    return dataType;
  }

  /**
   * Returns a distinct integer for each of the {@link ArrayType#isNumeric() numeric} data types that can be used to
   * (roughly) order them by the range of the ArrayType. {@code BYTE < UBYTE < SHORT < USHORT < INT < UINT <
   * LONG < ULONG < FLOAT < DOUBLE}. {@code -1} will be returned for all non-numeric data types.
   *
   * @param dataType a numeric data type.
   * @return a distinct integer for each of the numeric data types that can be used to (roughly) order them by size.
   */
  public static int rank(ArrayType dataType) {
    if (dataType == null) {
      return -1;
    }

    switch (dataType) {
      case BYTE:
        return 0;
      case UBYTE:
        return 1;
      case SHORT:
        return 2;
      case USHORT:
        return 3;
      case INT:
        return 4;
      case UINT:
        return 5;
      case LONG:
        return 6;
      case ULONG:
        return 7;
      case FLOAT:
        return 8;
      case DOUBLE:
        return 9;
      default:
        return -1;
    }
  }

  /**
   * Returns the data type that is the largest among the arguments. Relative sizes of data types are determined via
   * {@link #rank(ArrayType)}.
   *
   * @param dataTypes an array of numeric data types.
   * @return the data type that is the largest among the arguments.
   */
  public static ArrayType largestOf(ArrayType... dataTypes) {
    ArrayType widest = null;
    for (ArrayType dataType : dataTypes) {
      if (widest == null) {
        widest = dataType;
      } else if (rank(dataType) > rank(widest)) {
        widest = dataType;
      }
    }
    return widest;
  }

  /**
   * Returns the smallest numeric data type that:
   * <ol>
   * <li>can hold a larger integer than {@code dataType} can</li>
   * <li>if integral, has the same signedness as {@code dataType}</li>
   * </ol>
   * The relative sizes of data types are determined in a manner consistent with {@link #rank(ArrayType)}.
   * <p/>
   * <table border="1">
   * <tr>
   * <th>Argument</th>
   * <th>Result</th>
   * </tr>
   * <tr>
   * <td>BYTE</td>
   * <td>SHORT</td>
   * </tr>
   * <tr>
   * <td>UBYTE</td>
   * <td>USHORT</td>
   * </tr>
   * <tr>
   * <td>SHORT</td>
   * <td>INT</td>
   * </tr>
   * <tr>
   * <td>USHORT</td>
   * <td>UINT</td>
   * </tr>
   * <tr>
   * <td>INT</td>
   * <td>LONG</td>
   * </tr>
   * <tr>
   * <td>UINT</td>
   * <td>ULONG</td>
   * </tr>
   * <tr>
   * <td>LONG</td>
   * <td>DOUBLE</td>
   * </tr>
   * <tr>
   * <td>ULONG</td>
   * <td>DOUBLE</td>
   * </tr>
   * <tr>
   * <td>Any other data type</td>
   * <td>Just return argument</td>
   * </tr>
   * </table>
   * <p/>
   * The returned type is intended to be just big enough to hold the result of performing an unsigned conversion of a
   * value of the smaller type. For example, the {@code byte} value {@code -106} equals {@code 150} when interpreted
   * as unsigned. That won't fit in a (signed) {@code byte}, but it will fit in a {@code short}.
   *
   * @param dataType an integral data type.
   * @return the next larger type.
   */
  public static ArrayType nextLarger(ArrayType dataType) {
    switch (dataType) {
      case BYTE:
        return SHORT;
      case UBYTE:
        return USHORT;
      case SHORT:
        return INT;
      case USHORT:
        return UINT;
      case INT:
        return LONG;
      case UINT:
        return ULONG;
      case LONG:
      case ULONG:
        return DOUBLE;
      default:
        return dataType;
    }
  }

  public double getScaleFactor() {
    return scale;
  }

  public double getOffset() {
    return offset;
  }

  public ArrayType.Signedness getSignedness() {
    return signedness;
  }

  public ArrayType getScaledOffsetType() {
    return scaledOffsetType;
  }

  @Nonnull
  public ArrayType getUnsignedConversionType() {
    return unsignedConversionType;
  }

  public boolean hasValidData() {
    return hasValidRange || hasValidMin || hasValidMax;
  }

  public double getValidMin() {
    return validMin;
  }

  public double getValidMax() {
    return validMax;
  }

  // TODO using valid_min, valid_max to indicate "missing". Seems that these are often not used correctly,
  // and we should not support. CF or NUWG?
  public boolean isInvalidData(double val) {
    // valid_min and valid_max may have been multiplied by scale_factor, which could be a float, not a double.
    // That potential loss of precision means that we cannot do the nearlyEquals() comparison with
    // Misc.defaultMaxRelativeDiffDouble.
    boolean greaterThanOrEqualToValidMin =
        Misc.nearlyEquals(val, validMin, Misc.defaultMaxRelativeDiffFloat) || val > validMin;
    boolean lessThanOrEqualToValidMax =
        Misc.nearlyEquals(val, validMax, Misc.defaultMaxRelativeDiffFloat) || val < validMax;

    return (hasValidRange && !(greaterThanOrEqualToValidMin && lessThanOrEqualToValidMax))
        || (hasValidMin && !greaterThanOrEqualToValidMin) || (hasValidMax && !lessThanOrEqualToValidMax);
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

  public boolean hasScaleOffset() {
    return useScaleOffset;
  }

  public boolean hasMissingValue() {
    return hasMissingValue;
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

  public void setFillValueIsMissing(boolean b) {
    this.fillValueIsMissing = b;
  }

  public void setInvalidDataIsMissing(boolean b) {
    this.invalidDataIsMissing = b;
  }

  public void setMissingDataIsMissing(boolean b) {
    this.missingDataIsMissing = b;
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

  public Number convertUnsigned(Number value) {
    return convertUnsigned(value, signedness);
  }

  private static Number convertUnsigned(Number value, ArrayType dataType) {
    return convertUnsigned(value, dataType.getSignedness());
  }

  private static Number convertUnsigned(Number value, ArrayType.Signedness signedness) {
    if (signedness == ArrayType.Signedness.UNSIGNED) {
      // Handle integral types that should be treated as unsigned by widening them if necessary.
      return ArrayType.widenNumberIfNegative(value);
    } else {
      return value;
    }
  }

  public double applyScaleOffset(Number value) {
    double convertedValue = value.doubleValue();
    return useScaleOffset ? scale * convertedValue + offset : convertedValue;
  }

  public Array<?> applyScaleOffset(Array<?> in) {
    return convert(in, false, true, false);
  }

  public Number convertMissing(Number value) {
    return isMissing(value.doubleValue()) ? Double.NaN : value;
  }

  public Array<?> convertMissing(Array<?> in) {
    return convert(in, false, false, true);
  }

  public Array<?> convert(Array<?> in, boolean convertUnsigned, boolean applyScaleOffset, boolean convertMissing) {
    if (!in.getArrayType().isNumeric() || (!convertUnsigned && !applyScaleOffset && !convertMissing)) {
      return in; // Nothing to do!
    }

    if (getSignedness() == ArrayType.Signedness.SIGNED) {
      convertUnsigned = false;
    }
    if (!hasScaleOffset()) {
      applyScaleOffset = false;
    }

    ArrayType outType = origArrayType;
    if (convertUnsigned) {
      outType = getUnsignedConversionType();
    }
    if (applyScaleOffset) {
      outType = getScaledOffsetType();
    }

    if (outType != ArrayType.FLOAT && outType != ArrayType.DOUBLE) {
      convertMissing = false;
    }

    return convertArray(outType, (Array<Number>) in, new ConvertFn(convertUnsigned, applyScaleOffset, convertMissing));
  }

  private Array<?> convertArray(ArrayType type, Array<Number> org, ConvertFn convertFn) {
    int npts = (int) org.getSize();

    Object pvals;
    switch (type) {
      case UBYTE:
      case BYTE: {
        byte[] bvals = new byte[npts];
        int count = 0;
        for (Number val : org) {
          bvals[count++] = convertFn.convert(val).byteValue();
        }
        pvals = bvals;
        break;
      }
      case DOUBLE: {
        double[] dvals = new double[npts];
        int count = 0;
        for (Number val : org) {
          dvals[count++] = convertFn.convert(val).doubleValue();
        }
        pvals = dvals;
        break;
      }
      case FLOAT: {
        float[] fvals = new float[npts];
        int count = 0;
        for (Number val : org) {
          fvals[count++] = convertFn.convert(val).floatValue();
        }
        pvals = fvals;
        break;
      }
      case UINT:
      case INT: {
        int[] ivals = new int[npts];
        int count = 0;
        for (Number val : org) {
          ivals[count++] = convertFn.convert(val).intValue();
        }
        pvals = ivals;
        break;
      }
      case USHORT:
      case SHORT: {
        short[] svals = new short[npts];
        int count = 0;
        for (Number val : org) {
          svals[count++] = convertFn.convert(val).shortValue();
        }
        pvals = svals;
        break;
      }
      case ULONG:
      case LONG: {
        long[] lvals = new long[npts];
        int count = 0;
        for (Number val : org) {
          lvals[count++] = convertFn.convert(val).longValue();
        }
        pvals = lvals;
        break;
      }
      default:
        throw new IllegalArgumentException("makeArray od type " + type);
    }
    return Arrays.factory(type, org.getShape(), pvals);
  }

  private class ConvertFn {
    final boolean convertUnsigned;
    final boolean applyScaleOffset;
    final boolean convertMissing;

    public ConvertFn(boolean convertUnsigned, boolean applyScaleOffset, boolean convertMissing) {
      this.convertUnsigned = convertUnsigned;
      this.applyScaleOffset = applyScaleOffset;
      this.convertMissing = convertMissing;
    }

    Number convert(Number value) {
      if (convertUnsigned) {
        value = convertUnsigned(value);
      }
      if (applyScaleOffset) {
        value = applyScaleOffset(value);
      }
      if (convertMissing) {
        value = convertMissing(value);
      }
      return value;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** public for debugging */
  public void showInfo(Formatter f) {
    f.format("has missing = %s%n", hasMissing());
    if (hasMissing()) {
      if (hasMissingValue()) {
        f.format("   missing value(s) = ");
        for (double d : getMissingValues())
          f.format(" %f", d);
        f.format("%n");
      }
      if (hasFillValue())
        f.format("   fillValue = %f%n", getFillValue());
      if (hasValidData())
        f.format("   valid min/max = [%f,%f]%n", getValidMin(), getValidMax());
    }
    f.format("FillValue or default = %s%n", getFillValue());

    f.format("%nhas scale/offset = %s%n", hasScaleOffset());
    if (hasScaleOffset()) {
      double offset = applyScaleOffset(0.0);
      double scale = applyScaleOffset(1.0) - offset;
      f.format("   scale_factor = %f add_offset = %f%n", scale, offset);
    }
    f.format("original data type = %s%n", origArrayType);
    f.format("ScaledOffsetType data type = %s%n", getScaledOffsetType());
    f.format("UnsignedConversionType data type = %s%n", getUnsignedConversionType());
  }
}
