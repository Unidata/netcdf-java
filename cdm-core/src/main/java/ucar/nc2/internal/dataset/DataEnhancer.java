/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset;

import java.util.Set;

import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.ArraysConvert;
import ucar.array.Array;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;

/** Does enhancements to a VariableDS' data. */
public class DataEnhancer {
  private final VariableDS variableDS;
  private final ArrayType dataType;
  private final ArrayType orgDataType;
  private final EnhanceScaleMissingUnsigned scaleMissingUnsignedProxy;

  public DataEnhancer(VariableDS variableDS, EnhanceScaleMissingUnsigned scaleMissingUnsignedProxy) {
    this.variableDS = variableDS;
    this.dataType = variableDS.getArrayType();
    this.orgDataType = variableDS.getOriginalArrayType();
    this.scaleMissingUnsignedProxy = scaleMissingUnsignedProxy;
  }

  /** @deprecated use convertArray */
  @Deprecated
  public ucar.ma2.Array convert(ucar.ma2.Array data, Set<Enhance> enhancements) {
    Array<?> array = ArraysConvert.convertToArray(data);
    Array<?> converted = convertArray(array, enhancements);
    return ArraysConvert.convertFromArray(converted);
  }

  public ucar.array.Array<?> convertArray(ucar.array.Array<?> data, Set<Enhance> enhancements) {
    if (enhancements.contains(Enhance.ConvertEnums)
        && (dataType.isEnum() || (orgDataType != null && orgDataType.isEnum()))) {
      // Creates STRING data. As a result, we can return here, because the other conversions don't apply to STRING.
      return convertEnums(data);
    } else {
      // TODO: make this work for isVariableLength; i thought BUFR worked?
      if (variableDS.isVariableLength()) {
        return data;
      }
      return scaleMissingUnsignedProxy.convert(data, enhancements.contains(Enhance.ConvertUnsigned),
          enhancements.contains(Enhance.ApplyScaleOffset), enhancements.contains(Enhance.ConvertMissing));
    }
  }

  private Array<?> convertEnums(Array<?> values) {
    if (values.getArrayType() == ArrayType.STRING) {
      return values; // Nothing to do!
    }

    String[] sdata = new String[(int) values.getSize()];
    int count = 0;
    for (Number val : (Array<Number>) values) {
      String sval = variableDS.lookupEnumString(val.intValue());
      sdata[count++] = sval;
    }
    return Arrays.factory(ArrayType.STRING, values.getShape(), sdata);
  }

}
