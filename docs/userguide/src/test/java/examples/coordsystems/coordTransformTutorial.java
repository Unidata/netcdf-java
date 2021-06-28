package examples.coordsystems;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;
import ucar.nc2.internal.dataset.CoordTransformFactory;
import ucar.nc2.internal.dataset.transform.horiz.AbstractProjectionCT;
import ucar.nc2.internal.dataset.transform.horiz.HorizTransformBuilderIF;
import ucar.nc2.internal.dataset.transform.horiz.TransformBuilders;
import ucar.nc2.internal.dataset.transform.vertical.AbstractVerticalCTBuilder;
import ucar.nc2.internal.dataset.transform.vertical.VerticalTransformBuilder;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.AtmosSigma;
import ucar.unidata.geoloc.vertical.OceanSigma;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;


public class coordTransformTutorial {

    private static double EARTH_RADIUS; // used to represent param in LambertConformal
    class YourTransformClass{}; // only used for call in transformRegister()

    public static void registerTransform(){
        CoordTransformFactory.registerTransform( "YourTransformName", YourTransformClass.class);
    }

    public static void projectionEx(){
        /* INSERT public */class LambertConformalConic extends AbstractProjectionCT implements HorizTransformBuilderIF{

            public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, @Nullable String geoCoordinateUnits) {
                return null;
            }

            public String getTransformName() {
                // The name of the transformation, referenced in your dataset
                return "lambert_conformal_conic";
            }

            public TransformType getTransformType() {
                return TransformType.Projection;
            }
        }
    }

    public static ProjectionCT.Builder<?> implementMakeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
        // Various parameters are read from the attributes of the CoordinateTransform variable
        Attribute parAtt = ctv.findAttribute( "standard_parallel");
        if (parAtt == null )
            return null;
        double par1 = Objects.requireNonNull(parAtt.getNumericValue()).doubleValue();
        double par2 = (parAtt.getLength() > 1) ? Objects.requireNonNull(parAtt.getNumericValue(1)).doubleValue() : par1;
        double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
        double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
        double false_easting = ctv.findAttributeDouble(CF.FALSE_EASTING, 0.0);
        double false_northing = ctv.findAttributeDouble(CF.FALSE_NORTHING, 0.0);

        if ((false_easting != 0.0) || (false_northing != 0.0)) {
            double scalef = TransformBuilders.getFalseEastingScaleFactor(geoCoordinateUnits);
            false_easting *= scalef;
            false_northing *= scalef;
        }

        double semi_major_axis = ctv.findAttributeDouble(CF.SEMI_MAJOR_AXIS, Double.NaN);
        double semi_minor_axis = ctv.findAttributeDouble(CF.SEMI_MINOR_AXIS, Double.NaN);
        double inverse_flattening = ctv.findAttributeDouble(CF.INVERSE_FLATTENING, 0.0);

        ucar.unidata.geoloc.Projection proj;

        // check for ellipsoidal earth
        if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
            Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
            proj = new ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse(lat0, lon0, par1, par2,
                    false_easting, false_northing, earth);

        } else {
            // A Projection is created from the parameters
            proj = new ucar.unidata.geoloc.projection.LambertConformal(lat0, lon0, par1, par2, false_easting,
                    false_northing, EARTH_RADIUS);
        }
        // A ProjectionCT wraps the Projection
        return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
    }

    public static void vertTransEx(){
        /* INSERT public */class CFOceanSigma extends AbstractVerticalCTBuilder implements VerticalTransformBuilder {
            private String sigma, ps, ptop;
            private Formatter errBuffer; /* DOCS-IGNORE */

            protected String getFormula(NetcdfDataset ds, Variable ctv) {
                String formula = ctv.findAttributeString("formula_terms", null);
                if (null == formula) {
                    if (null != errBuffer)
                        errBuffer.format(
                                "CoordTransBuilder %s: needs attribute 'formula_terms' on Variable %s%n", getTransformName(),
                                ctv.attributes().getName());
                    return null;
                }
                return formula;
            }

            public TransformType getTransformType() {
                return TransformType.Vertical;
            }

            public String getTransformName() {
                // The name of the transformation, this is referenced in your dataset
                return "atmosphere_sigma_coordinate";
            }

            public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
                 // VerticalTransform is created when method called by the VerticalCT
                 // IMPLEMENTED BELOW
                 return null; /* DOCS-IGNORE */
             }

            public VerticalCT.Builder<?> makeVerticalCT(NetcdfFile ds, AttributeContainer ctv) {
                // IMPLEMENTED BELOW
                return null; /* DOCS-IGNORE */
            }
         }
    }

    public static VerticalCT.Builder<?> implementMakeVerticalCT(NetcdfFile ds, AttributeContainer ctv, String sigma, String ps, String ptop){
        /* The CF vertical transforms rely on formula terms to describe the algorithm
           You may choose to not use this method
        */
        String formula_terms = ctv.findAttributeString("formula_terms", null);
        //String formula_terms = getFormula( ctv.attributes());
        if (null == formula_terms) return null;

        // Parse the formula terms to get the names of variables
        StringTokenizer stoke = new StringTokenizer(formula_terms);
        while (stoke.hasMoreTokens()) {
            String toke = stoke.nextToken();
            if (toke.equalsIgnoreCase("sigma:"))
                sigma = stoke.nextToken();
            else if (toke.equalsIgnoreCase("ps:"))
                ps = stoke.nextToken();
            else if (toke.equalsIgnoreCase("ptop:"))
                ptop = stoke.nextToken();
        }

        /* A VerticalCT is constructed that holds the transform parameters
           Parameters are added to the VerticalCT, where the AtmosSigma class will find them
        */
        VerticalCT rs = VerticalCT.builder().build();
        rs.toBuilder().setName("AtmSigma_Transform_"+ctv.getName());
        rs.toBuilder().setTransformType( TransformType.Vertical);
        rs.toBuilder().setCtvAttributes( ctv);

        /*
         The standard_name and formula_terms attributes are added to the VerticalCT
         The CoordinateTransform can be recreated from the VerticalCT if needed
         The formula is not needed, but makes the dataset metadata more self-contained
        */
        rs.toBuilder().addParameter(new Attribute("standard_name", rs.getName()));
        rs.toBuilder().addParameter(new Attribute("formula_terms", formula_terms));
        rs.toBuilder().addParameter(new Attribute("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

        // Use the builder to add the VerticalCT to the NetcdfDataset
        try(NetcdfDataset ncd = NetcdfDatasets.openDataset(ds.getLocation())){
            NetcdfDataset.Builder<?> dsBuilder = ncd.toBuilder();
            dsBuilder.coords.addCoordinateTransform(rs.toBuilder());
            ncd.close();
            return rs.toBuilder();
        }
        catch(Exception e) {
            // HANDLE EXCEPTION
            return null;
        }
    }

    public static void vertTransClass() {
        /* INSERT public class AtmosSigma extends AbstractVerticalTransform { */
            class AtmosSigma {/* DOCS-IGNORE */
            // The parameter names as public constant Strings
            public static final String PTOP = "Pressure at top";
            public static final String PS = "surfacePressure variable name";
            public static final String SIGMA = "sigma variable name";
            private Variable psVar; // surface pressure
            private double[] sigma; // The sigma array, function of z
            private double ptop;    // Top of the model

            public ucar.unidata.geoloc.vertical.AtmosSigma create(NetcdfFile ds, Dimension timeDim, List<Attribute> params) {
                // IMPLEMENTED BELOW
                return null; /* DOCS-IGNORE */
            }

            public AtmosSigma(Dimension timeDim, String units, Variable psVar, double[] sigma, double ptop) {
                // IMPLEMENTED BELOW
            }

            public ArrayDouble.D3 getCoordinateArray(int timeIndex) {
                // TO BE IMPLEMENTED
                return null;
            }

            public ArrayDouble.D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException {
                // TO BE IMPLEMENTED
                return null;
            }
        }
    }
}
