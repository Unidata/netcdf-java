/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.VTfromExistingData;

/**
 * Create a Vertical Transform from an "explicit_field", where the vertical coordinate is explcitly specified as a
 * variable.
 *
 * @author caron
 */
public class VExplicitField extends AbstractVerticalCTBuilder implements VerticalTransformBuilder {
  public String getTransformName() {
    return VerticalCT.Type.Existing3DField.name();
  }

  public VerticalCT.Builder<?> makeVerticalCT(NetcdfFile ds, AttributeContainer ctv) {
    VerticalCT.Builder<?> rs = VerticalCT.builder().setName(ctv.getName()).setAuthority(getTransformName())
        .setVerticalType(VerticalCT.Type.Existing3DField).setTransformBuilder(this);
    String fieldName = ctv.findAttributeString(VTfromExistingData.existingDataField, null);
    if (null == fieldName)
      throw new IllegalArgumentException(
          "ExplicitField Vertical Transform must have attribute " + VTfromExistingData.existingDataField);
    rs.addParameter(new Attribute("standard_name", getTransformName()));
    rs.addParameter(new Attribute(VTfromExistingData.existingDataField, fieldName));
    return rs;
  }

  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return VTfromExistingData.create(ds, timeDim, vCT.getCtvAttributes());
  }

}
