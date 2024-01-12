package ucar.nc2.filter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.VariableDS;

public class UnsignedConversion implements Enhancement {

  private DataType outType;
  private DataType.Signedness signedness;

  public static UnsignedConversion createFromVar(VariableDS var) {
    DataType origDataType = var.getDataType();
    // won't need to convert non-integral types
    if (!origDataType.isIntegral()) {
      return new UnsignedConversion(origDataType, DataType.Signedness.SIGNED);
    }
    DataType unsignedConversionType = origDataType;
    // unsignedConversionType may start out as integral, but then be widened to non-integral (i.e. LONG -> DOUBLE)
    // so we cannot rely upon unsignedConversionType to store the signedness of the variable. We need a separate field.
    DataType.Signedness signedness = origDataType.getSignedness();

    // In the event of conflict, "unsigned" wins. Potential conflicts include:
    // 1. origDataType is unsigned, but variable has "_Unsigned == false" attribute.
    // 2. origDataType is signed, but variable has "_Unsigned == true" attribute.
    if (signedness == DataType.Signedness.SIGNED) {
      String unsignedAtt = var.attributes().findAttributeString(CDM.UNSIGNED, null);
      if (unsignedAtt != null && unsignedAtt.equalsIgnoreCase("true")) {
        signedness = DataType.Signedness.UNSIGNED;
      }
    }

    if (signedness == DataType.Signedness.UNSIGNED) {
      // We may need a larger data type to hold the results of the unsigned conversion.
      unsignedConversionType = FilterHelpers.nextLarger(origDataType).withSignedness(DataType.Signedness.UNSIGNED);
    }
    return new UnsignedConversion(unsignedConversionType, signedness);
  }

  public UnsignedConversion(DataType outType, DataType.Signedness signedness) {
    this.outType = outType;
    this.signedness = signedness;
  }

  public DataType getOutType() {
    return this.outType;
  }

  public DataType.Signedness getSignedness() {
    return this.signedness;
  }

  public double convert(double value) {
    Number val;
    switch (outType) {
      case UBYTE:
        val = new Byte((byte) value);
        break;
      case USHORT:
        val = new Byte((byte) value);
        break;
      case UINT:
        val = new Short((short) value);
        break;
      case ULONG:
        val = new Integer((int) value);
        break;
      default:
        val = new Double(value);
    }
    return this.signedness == DataType.Signedness.UNSIGNED ? DataType.widenNumberIfNegative(val).doubleValue() : value;
  }

  public Array convertUnsigned(Array in) {
    if (signedness == DataType.Signedness.SIGNED) {
      return in;
    }
    Array out = Array.factory(outType, in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();

    try {
      // iterate and convert elements
      while (iterIn.hasNext()) {
        Number value = (Number) iterIn.getObjectNext();
        value = DataType.widenNumberIfNegative(value);
        iterOut.setObjectNext(value);
      }
    } catch (ClassCastException ex) {
      return in;
    }

    return out;
  }

  public static Number convertUnsigned(Number value, DataType dataType) {
    return dataType.isUnsigned() ? DataType.widenNumberIfNegative(value) : value;
  }
}
