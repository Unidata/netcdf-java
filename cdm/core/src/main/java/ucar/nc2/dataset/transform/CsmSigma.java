/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.transform;

import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VerticalCT;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.AtmosSigma;
import ucar.unidata.geoloc.vertical.HybridSigmaPressure;
import ucar.unidata.util.Parameter;

public class CsmSigma extends AbstractTransformBuilder implements VertTransformBuilderIF {

  public String getTransformName() {
    return "csm_sigma_level";
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    VerticalCT rs = new VerticalCT("sigma-" + ctv.getName(), getTransformName(), VerticalCT.Type.Sigma, this);
    rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

    if (!addParameter2(rs, AtmosSigma.PS, ds, ctv, "PS_var", false)) {
      return null;
    }
    if (!addParameter2(rs, AtmosSigma.SIGMA, ds, ctv, "B_var", false)) {
      return null;
    }
    if (!addParameter2(rs, AtmosSigma.PTOP, ds, ctv, "P0_var", false)) {
      return null;
    }
    return rs;
  }

  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim,
      VerticalCT vCT) {
    return new AtmosSigma(ds, timeDim, vCT.getParameters());
  }

  /**
   * Add a Parameter to a CoordinateTransform. The variable attribute points to a another variable that has the data in
   * it. Make sure that
   * attribute and variable exist. If readData is true, read the data and use it as the value of the parameter,
   * otherwise use the name as
   * the value of the parameter.
   *
   * @param rs the CoordinateTransform
   * @param paramName the parameter name
   * @param ds dataset
   * @param v variable
   * @param attName variable attribute name
   * @param readData if true, read data and use a s parameter value
   * @return true if success, false is failed
   */
  private static boolean addParameter2(CoordinateTransform rs, String paramName, NetcdfFile ds, AttributeContainer v,
      String attName, boolean readData) {
    String varName;
    if (null == (varName = v.findAttributeString(attName, null))) {
      return false;
    }
    varName = varName.trim();

    Variable dataVar;
    if (null == (dataVar = ds.findVariable(varName))) {
      return false;
    }

    if (readData) {
      Array data;
      try {
        data = dataVar.read();
      } catch (IOException e) {
        return false;
      }
      double[] vals = (double[]) data.get1DJavaArray(DataType.DOUBLE);
      rs.addParameter(new Parameter(paramName, vals));

    } else {
      rs.addParameter(new Parameter(paramName, varName));
    }

    return true;
  }

  public static class HybridSigmaPressureBuilder extends AbstractTransformBuilder implements VertTransformBuilderIF {

    public String getTransformName() {
      return "csm_hybrid_sigma_pressure";
    }

    public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
      VerticalCT rs = new VerticalCT(ctv.getName(), getTransformName(), VerticalCT.Type.HybridSigmaPressure, this);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)"));

      if (!addParameter2(rs, HybridSigmaPressure.PS, ds, ctv, "PS_var", false)) {
        return null;
      }
      if (!addParameter2(rs, HybridSigmaPressure.A, ds, ctv, "A_var", false)) {
        return null;
      }
      if (!addParameter2(rs, HybridSigmaPressure.B, ds, ctv, "B_var", false)) {
        return null;
      }
      if (!addParameter2(rs, HybridSigmaPressure.P0, ds, ctv, "P0_var", false)) {
        return null;
      }
      return rs;
    }

    public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim,
        VerticalCT vCT) {
      return new HybridSigmaPressure(ds, timeDim, vCT.getParameters());
    }
  }

}
