package ucar.nc2.ogc.waterml;

import java.io.IOException;
import java.util.Arrays;
import net.opengis.waterml.x20.MeasureType;
import ucar.ma2.Array;
import ucar.ma2.StructureMembers;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeature;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NcMeasureType {
  // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/
  // wml2:MeasurementTVP/wml2:value
  public static MeasureType initValue(MeasureType value, PointFeature pointFeat, VariableSimpleIF dataVar)
      throws IOException {
    // TEXT
    StructureMembers.Member firstDataMember = pointFeat.getDataAll().findMember(dataVar.getShortName());
    assert firstDataMember != null : String
        .format("%s appeared in the list of data variables but not in the StructureData.", dataVar.getShortName());

    Array dataArray = pointFeat.getDataAll().getArray(firstDataMember);
    assert dataArray.getSize() == 1 : String.format("Expected array to be scalar, but its shape was %s.",
        Arrays.toString(dataArray.getShape()));

    double dataVal = dataArray.getDouble(0);
    value.setDoubleValue(dataVal);

    return value;
  }

  private NcMeasureType() {}
}
