package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.ReferenceType;
import ucar.nc2.VariableSimpleIF;

/**
 * Created by cwardgar on 3/7/14.
 */
public abstract class NcReferenceType {
  // wml2:Collection/wml2:observationMember/om:OM_Observation/om:observedProperty
  public static ReferenceType initObservedProperty(ReferenceType observedProperty, VariableSimpleIF dataVar) {
    // @xlink:title
    observedProperty.setTitle(dataVar.getShortName());

    return observedProperty;
  }

  // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/
  // wml2:defaultPointMetadata/wml2:DefaultTVPMeasurementMetadata/wml2:interpolationType
  public static ReferenceType initInterpolationType(ReferenceType interpolationType) {
    // FIXME: I may be able to do better if the NetCDF file has an interpolation_type variable.
    // See OGC 12-031r2, p. 35.

    // @xlink:title
    interpolationType.setTitle("Unknown");

    // @xlink:href
    interpolationType.setHref("http://www.opengis.net/def/nil/OGC/0/unknown");

    return interpolationType;
  }

  private NcReferenceType() {}
}
