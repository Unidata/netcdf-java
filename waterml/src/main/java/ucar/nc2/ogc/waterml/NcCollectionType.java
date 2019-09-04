package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.CollectionType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.ogc.om.NcOMObservationPropertyType;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NcCollectionType {
  // wml2:Collection
  public static CollectionType initCollection(CollectionType collection, FeatureDatasetPoint fdPoint,
      List<VariableSimpleIF> dataVars) throws IOException {
    // @gml:id
    String id = MarshallingUtil.createIdForType(CollectionType.class);
    collection.setId(id);

    // wml2:metadata
    NcDocumentMetadataPropertyType.initMetadata(collection.addNewMetadata());

    // wml2:observationMember[0..*]
    StationTimeSeriesFeatureCollection stationFeatColl = getStationFeatures(fdPoint);
    try {
      while (stationFeatColl.hasNext()) {
        StationTimeSeriesFeature stationFeat = stationFeatColl.next();

        for (VariableSimpleIF dataVar : dataVars) {
          if (dataVar.getDataType().isNumeric()) {
            // wml2:observationMember
            NcOMObservationPropertyType.initObservationMember(collection.addNewObservationMember(), stationFeat,
                dataVar);
          }
        }
      }
    } finally {
      stationFeatColl.finish();
    }

    return collection;
  }

  public static StationTimeSeriesFeatureCollection getStationFeatures(FeatureDatasetPoint fdPoint) {
    String datasetFileName = new File(fdPoint.getNetcdfFile().getLocation()).getName();

    if (fdPoint.getFeatureType() != FeatureType.STATION) {
      throw new IllegalArgumentException(String.format("In %s, expected feature type to be STATION, not %s.",
          datasetFileName, fdPoint.getFeatureType()));
    }

    List<DsgFeatureCollection> featCollList = fdPoint.getPointFeatureCollectionList();

    if (featCollList.size() != 1) {
      throw new IllegalArgumentException(
          String.format("Expected %s to contain 1 FeatureCollection, not %s.", datasetFileName, featCollList.size()));
    } else if (!(featCollList.get(0) instanceof StationTimeSeriesFeatureCollection)) {
      String expectedClassName = StationTimeSeriesFeatureCollection.class.getName();
      String actualClassName = featCollList.get(0).getClass().getName();

      throw new IllegalArgumentException(String.format("Expected %s's FeatureCollection to be a %s, not a %s.",
          datasetFileName, expectedClassName, actualClassName));
    }

    return (StationTimeSeriesFeatureCollection) featCollList.get(0);
  }

  private NcCollectionType() {}
}
