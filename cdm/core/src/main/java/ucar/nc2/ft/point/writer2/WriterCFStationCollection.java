/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataComposite;
import ucar.ma2.StructureDataFromMember;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.VariableSimpleBuilder;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationTimeSeriesCollectionImpl;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.Station;

/**
 * Write a CF "Discrete Sample" station file.
 * Example H.7. Timeseries of station data in the indexed ragged array representation.
 *
 * <p/>
 * 
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8340320"
 * @author caron
 * @since Aug 19, 2009
 */
class WriterCFStationCollection extends WriterCFPointAbstract {

  //////////////////////////////////////////////////////////

  private List<StationFeature> stnList;
  private Structure stationStruct; // used for netcdf4 extended
  private HashMap<String, Integer> stationIndexMap;

  private boolean useDesc;
  private boolean useAlt;
  private boolean useWmoId;

  private int desc_strlen = 1, wmo_strlen = 1;
  private HashSet<String> featureVarMap = new HashSet<>();

  WriterCFStationCollection(String fileOut, AttributeContainer atts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, atts, dataVars, timeUnit, altUnits, config);
    writerb.addAttribute(new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeries.name()));
    writerb.addAttribute(new Attribute(CF.DSG_REPRESENTATION,
        "Timeseries of station data in the indexed ragged array representation, H.2.5"));
  }

  @Override
  void finishBuilding() throws IOException {
    super.finishBuilding();
    stationStruct = findStructure(stationStructName);
  }

  protected void writeHeader(StationTimeSeriesFeatureCollection stations) throws IOException {
    this.stnList = stations.getStationFeatures().stream().distinct().collect(Collectors.toList());
    List<VariableSimpleIF> coords = new ArrayList<>();

    // see if there's altitude, wmoId for any stations
    for (Station stn : stations) {
      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (!stn.getWmoId().trim().isEmpty()))
        useWmoId = true;
      if ((stn.getDescription() != null) && (!stn.getDescription().trim().isEmpty()))
        useDesc = true;

      // find string lengths
      id_strlen = Math.max(id_strlen, stn.getName().length());
      if (stn.getDescription() != null)
        desc_strlen = Math.max(desc_strlen, stn.getDescription().length());
      if (stn.getWmoId() != null)
        wmo_strlen = Math.max(wmo_strlen, stn.getWmoId().length());

      if (stn instanceof DsgFeatureCollection) {
        DsgFeatureCollection dsgStation = (DsgFeatureCollection) stn;
        if (coords.stream().noneMatch(x -> x.getShortName().equals(dsgStation.getTimeName()))) {
          coords.add(VariableSimpleBuilder
              .makeScalar(dsgStation.getTimeName(), "time of measurement", dsgStation.getTimeUnit().getUdUnit(),
                  DataType.DOUBLE)
              .addAttribute(CF.CALENDAR, dsgStation.getTimeUnit().getCalendar().toString()).build());
        }
      } else {
        coords.add(
            VariableSimpleBuilder.makeScalar(timeName, "time of measurement", timeUnit.getUdUnit(), DataType.DOUBLE)
                .addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());
      }
    }

    llbb = ucar.nc2.ft.point.writer.CFPointWriterUtils.getBoundingBox(stnList); // gets written in super.finish();
    coords.add(VariableSimpleBuilder
        .makeScalar(stationIndexName, "station index for this observation record", null, DataType.INT)
        .addAttribute(CF.INSTANCE_DIMENSION, stationDimName).build());

    super.writeHeader(coords, (StationTimeSeriesCollectionImpl) stations, null);
    int count = 0;
    stationIndexMap = new HashMap<>(2 * stnList.size());
    for (StationFeature stn : stnList) {
      writeStationData(stn);
      stationIndexMap.put(stn.getName(), count);
      count++;
    }
  }

  @Override
  void makeFeatureVariables(StructureData featureData, boolean isExtended) {
    // add the dimensions : extendded model can use an unlimited dimension
    // Dimension stationDim = isExtended ? writer.addDimension(null, stationDimName, 0, true, true, false) :
    // writer.addDimension(null, stationDimName, nstns);
    Dimension stationDim = writerb.addDimension(stationDimName, stnList.size());

    List<VariableSimpleIF> stnVars = new ArrayList<>();
    stnVars.add(VariableSimpleBuilder.makeScalar(latName, "station latitude", CDM.LAT_UNITS, DataType.DOUBLE).build());
    stnVars.add(VariableSimpleBuilder.makeScalar(lonName, "station longitude", CDM.LON_UNITS, DataType.DOUBLE).build());

    if (useAlt) {
      stnVars.add(VariableSimpleBuilder.makeScalar(stationAltName, "station altitude", altUnits, DataType.DOUBLE)
          .addAttribute(CF.STANDARD_NAME, CF.SURFACE_ALTITUDE)
          .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits)).build());
    }

    stnVars.add(VariableSimpleBuilder.makeString(stationIdName, "station identifier", null, id_strlen)
        .addAttribute(CF.CF_ROLE, CF.TIMESERIES_ID).build()); // station_id:cf_role = "timeseries_id";

    if (useDesc)
      stnVars.add(VariableSimpleBuilder.makeString(descName, "station description", null, desc_strlen)
          .addAttribute(CF.STANDARD_NAME, CF.PLATFORM_NAME).build());

    if (useWmoId)
      stnVars.add(VariableSimpleBuilder.makeString(wmoName, "station WMO id", null, wmo_strlen)
          .addAttribute(CF.STANDARD_NAME, CF.PLATFORM_ID).build());

    for (StructureMembers.Member m : featureData.getMembers()) {
      if (findDataVar(m.getName()) != null) {
        stnVars.add(VariableSimpleBuilder.fromMember(m).build());
      }
    }

    if (isExtended) {
      Structure.Builder structb = writerb.addStructure(stationStructName, stationDimName);
      addCoordinatesExtended(structb, stnVars);
    } else {
      addCoordinatesClassic(stationDim, stnVars, featureVarMap);
    }
  }

  private int stnRecno;

  private void writeStationData(StationFeature stn) throws IOException {
    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(latName, null, null, DataType.DOUBLE, stn.getLatLon().getLatitude());
    smb.addMemberScalar(lonName, null, null, DataType.DOUBLE, stn.getLatLon().getLongitude());
    smb.addMemberScalar(stationAltName, null, null, DataType.DOUBLE, stn.getAltitude());
    smb.addMemberString(stationIdName, null, null, stn.getName().trim(), id_strlen);
    if (useDesc)
      smb.addMemberString(descName, null, null, stn.getDescription().trim(), desc_strlen);
    if (useWmoId)
      smb.addMemberString(wmoName, null, null, stn.getWmoId().trim(), wmo_strlen);
    StructureData stnCoords = new StructureDataFromMember(smb.build());

    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(stnCoords, stn.getFeatureData()));
    stnRecno = super.writeStructureData(stnRecno, stationStruct, sdall, featureVarMap);
  }

  private int obsRecno;

  protected void writeRecord(String stnName, double timeCoordValue, CalendarDate obsDate, double altCoordValue,
      StructureData sdata) throws IOException {
    trackBB(null, obsDate);

    Integer parentIndex = stationIndexMap.get(stnName);
    if (parentIndex == null)
      throw new RuntimeException("Cant find station " + stnName);

    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(timeName, null, null, DataType.DOUBLE, timeCoordValue);
    if (!Double.isNaN(altCoordValue))
      smb.addMemberScalar(altitudeCoordinateName, null, null, DataType.DOUBLE, altCoordValue);
    smb.addMemberScalar(stationIndexName, null, null, DataType.INT, parentIndex);
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, sdata));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
