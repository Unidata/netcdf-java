/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset;

import java.util.Set;
import ucar.array.ArraysConvert;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.dataset.EnhanceScaleMissingUnsignedImpl;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;

/** Does enhancemnets to a VariableDS' data. */
public class DataEnhancer {
  private final VariableDS variableDS;
  private final DataType dataType;
  private final DataType orgDataType;
  private final EnhanceScaleMissingUnsignedImpl scaleMissingUnsignedProxy;

  public DataEnhancer(VariableDS variableDS, EnhanceScaleMissingUnsignedImpl scaleMissingUnsignedProxy) {
    this.variableDS = variableDS;
    this.dataType = variableDS.getDataType();
    this.orgDataType = variableDS.getOriginalDataType();
    this.scaleMissingUnsignedProxy = scaleMissingUnsignedProxy;
  }

  public ucar.ma2.Array convert(ucar.ma2.Array data, Set<Enhance> enhancements) {
    if (enhancements.contains(Enhance.ConvertEnums)
        && (dataType.isEnum() || (orgDataType != null && orgDataType.isEnum()))) {
      // Creates STRING data. As a result, we can return here, because the other conversions don't apply to STRING.
      return convertEnums(data);
    } else {
      // TODO: make work for isVariableLength; i thought BUFR worked?
      if (variableDS.isVariableLength()) {
        return data;
      }
      return scaleMissingUnsignedProxy.convert(data, enhancements.contains(Enhance.ConvertUnsigned),
          enhancements.contains(Enhance.ApplyScaleOffset), enhancements.contains(Enhance.ConvertMissing));
    }
  }

  private ucar.ma2.Array convertEnums(ucar.ma2.Array values) {
    if (!values.getDataType().isIntegral()) {
      return values; // Nothing to do!
    }

    ucar.ma2.Array result = ucar.ma2.Array.factory(DataType.STRING, values.getShape());
    IndexIterator ii = result.getIndexIterator();
    values.resetLocalIterator();
    while (values.hasNext()) {
      String sval = variableDS.lookupEnumString(values.nextInt());
      ii.setObjectNext(sval);
    }
    return result;
  }

  // TODO
  public ucar.array.Array<?> convertArray(ucar.array.Array<?> data, Set<Enhance> enhancements) {
    ucar.ma2.Array ma2 = ArraysConvert.convertFromArray(data);
    return ArraysConvert.convertToArray(convert(ma2, enhancements));
  }


}
