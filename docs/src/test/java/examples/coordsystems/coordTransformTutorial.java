package examples.coordsystems;

import ucar.ma2.ArrayDouble;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.transform.AbstractTransformBuilder;
import ucar.nc2.dataset.transform.HorizTransformBuilderIF;
import ucar.unidata.geoloc.vertical.OceanSigma;
import ucar.unidata.geoloc.vertical.VerticalTransformImpl;

import java.util.Formatter;

public class coordTransformTutorial {

  class YourTransformClass {
  }; // only used for call in transformRegister()

  public static void registerTransform() {
    CoordTransBuilder.registerTransform("YourTransformName", YourTransformClass.class);
  }

  public static void projectionEx() {
    class LambertConformalConic extends AbstractTransformBuilder
        implements HorizTransformBuilderIF {

      public ProjectionCT makeCoordinateTransform(AttributeContainer ctv,
          String geoCoordinateUnits) {
        // IMPLEMENTED BELOW
        return null; /* DOCS-IGNORE */
      }

      public String getTransformName() {
        // The name of the transformation. This is referenced in your dataset
        return "lambert_conformal_conic";
      }
    }
  }

  public static void vertTransEx() {
    /* INSERT public */class CFOceanSigma extends AbstractTransformBuilder {
      private String sigma, ps, ptop;
      private Formatter errBuffer; /* DOCS-IGNORE */

      protected String getFormula(NetcdfDataset ds, Variable ctv) {
        String formula = ctv.findAttributeString("formula_terms", null);
        if (null == formula) {
          if (null != errBuffer)
            errBuffer.format(
                "CoordTransBuilder %s: needs attribute 'formula_terms' on Variable %s%n",
                getTransformName(), ctv.attributes().getName());
          return null;
        }
        return formula;
      }

      public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
        // IMPLEMENTED BELOW
        return null; /* DOCS-IGNORE */
      }

      public String getTransformName() {
        // The name of the transformation, this is referenced in your dataset
        return "atmosphere_sigma_coordinate";
      }

      public OceanSigma makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
        // VerticalTransform is created when method called by the VerticalCT
        return new OceanSigma(ds, timeDim, vCT.getParameters());
      }
    }
  }

  public static void vertTransClass() {
    class AtmosSigma extends VerticalTransformImpl {
      // The parameter names as public constant Strings
      public static final String PTOP = "Pressure at top";
      public static final String PS = "surfacePressure variable name";
      public static final String SIGMA = "sigma variable name";
      private Variable psVar; // surface pressure
      private double[] sigma; // The sigma array, function of z
      private double ptop; // Top of the model

      public AtmosSigma(Dimension timeDim) {
        // IMPLEMENTED BELOW
        super(timeDim);/* DOCS-IGNORE */
      }

      public ArrayDouble.D3 getCoordinateArray(int timeIndex) {
        // IMPLEMENTED BELOW
        return null;/* DOCS-IGNORE */
      }

      public ArrayDouble.D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) {
        // TO BE IMPLEMENTED
        return null;/* DOCS-IGNORE */
      }
    }
  }
}
