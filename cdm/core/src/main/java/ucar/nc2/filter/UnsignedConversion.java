package ucar.nc2.filter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.VariableDS;

public class UnsignedConversion {

  private DataType outType;

  public static UnsignedConversion createFromVar(VariableDS var) {
    DataType origDataType = var.getDataType();
    DataType unsignedConversionType = origDataType;

    // unsignedConversionType is initialized to origDataType, and origDataType may be a non-integral type that doesn't
    // have an "unsigned flavor" (such as FLOAT and DOUBLE). Furthermore, unsignedConversionType may start out as
    // integral, but then be widened to non-integral (i.e. LONG -> DOUBLE). For these reasons, we cannot rely upon
    // unsignedConversionType to store the signedness of the variable. We need a separate field.
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
    return new UnsignedConversion(unsignedConversionType);
  }

  public UnsignedConversion(DataType outType) {
    this.outType = outType;
  }

  public DataType getOutType() {
    return this.outType;
  }

  public Number convertUnsigned(Number value) {
    return DataType.widenNumberIfNegative(value);
  }


  public Array convertUnsigned(Array in) {
    Array out = Array.factory(outType, in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();

    // iterate and convert elements
    while (iterIn.hasNext()) {
      Number value = (Number) iterIn.getObjectNext();
      value = convertUnsigned(value);
      iterOut.setObjectNext(value);
    }

    return out;
  }
}
