/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.Station;
import java.io.IOException;
import java.util.*;

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
public class WriterCFStationCollection extends CFPointWriter {

  //////////////////////////////////////////////////////////

  private List<StationFeature> stnList;
  protected Structure stationStruct; // used for netcdf4 extended
  private HashMap<String, Integer> stationIndexMap;

  private boolean useDesc;
  private boolean useAlt;
  private boolean useWmoId;

  private int desc_strlen = 1, wmo_strlen = 1;
  private Map<String, Variable> featureVarMap = new HashMap<>();

  public WriterCFStationCollection(String fileOut, List<Attribute> atts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, atts, dataVars, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeries.name()));
    writer.addGroupAttribute(null, new Attribute(CF.DSG_REPRESENTATION,
        "Timeseries of station data in the indexed ragged array representation, H.2.5"));
  }

  public void writeHeader(List<StationFeature> stns, StationPointFeature spf) throws IOException {
    this.stnList = stns;

    // see if there's altitude, wmoId for any stations
    for (Station stn : stnList) {
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
    }

    llbb = CFPointWriterUtils.getBoundingBox(stnList); // gets written in super.finish();

    StationFeature sf = spf.getStation();
    StructureData stnData = sf.getFeatureData();
    StructureData obsData = spf.getFeatureData();

    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleBuilder.makeScalar(timeName, "time of measurement", timeUnit.getUdUnit(), DataType.DOUBLE)
        .addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());

    coords.add(
        VariableSimpleBuilder.makeScalar(stationIndexName, "station index for this observation record", null, DataType.INT)
            .addAttribute(CF.INSTANCE_DIMENSION, stationDimName).build());

    Formatter coordNames = new Formatter().format("%s %s %s", timeName, latName, lonName);
    if (useAlt)
      coordNames.format(" %s", stationAltName);

    super.writeHeader(coords, stnData, obsData, coordNames.toString());

    int count = 0;
    stationIndexMap = new HashMap<>(2 * stns.size());
    for (StationFeature stn : stnList) {
      writeStationData(stn);
      stationIndexMap.put(stn.getName(), count);
      count++;
    }

  }

  protected void makeFeatureVariables(StructureData featureData, boolean isExtended) {

    // add the dimensions : extendded model can use an unlimited dimension
    // Dimension stationDim = isExtended ? writer.addDimension(null, stationDimName, 0, true, true, false) :
    // writer.addDimension(null, stationDimName, nstns);
    Dimension stationDim = writer.addDimension(null, stationDimName, stnList.size());

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
      if (getDataVar(m.getName()) != null)
        stnVars.add(VariableSimpleBuilder.fromMember(m).build());
    }

    if (isExtended) {
      stationStruct = (Structure) writer.addVariable(null, stationStructName, DataType.STRUCTURE, stationDimName);
      addCoordinatesExtended(stationStruct, stnVars);
    } else {
      addCoordinatesClassic(stationDim, stnVars, featureVarMap);
    }

  }

  private int stnRecno;

  private void writeStationData(StationFeature stn) throws IOException {

    StructureDataScalar stnCoords = new StructureDataScalar("Coords");
    stnCoords.addMember(latName, null, null, DataType.DOUBLE, stn.getLatLon().getLatitude());
    stnCoords.addMember(lonName, null, null, DataType.DOUBLE, stn.getLatLon().getLongitude());
    stnCoords.addMember(stationAltName, null, null, DataType.DOUBLE, stn.getAltitude());
    stnCoords.addMemberString(stationIdName, null, null, stn.getName().trim(), id_strlen);
    if (useDesc)
      stnCoords.addMemberString(descName, null, null, stn.getDescription().trim(), desc_strlen);
    if (useWmoId)
      stnCoords.addMemberString(wmoName, null, null, stn.getWmoId().trim(), wmo_strlen);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(stnCoords); // coords first so it takes precedence
    sdall.add(stn.getFeatureData());

    stnRecno = super.writeStructureData(stnRecno, stationStruct, sdall, featureVarMap);
  }

  public void writeRecord(Station s, PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(s.getName(), sobs.getObservationTime(), sobs.getObservationTimeAsCalendarDate(), sdata);
  }

  private int obsRecno;

  public void writeRecord(String stnName, double timeCoordValue, CalendarDate obsDate, StructureData sdata)
      throws IOException {
    trackBB(null, obsDate);

    Integer parentIndex = stationIndexMap.get(stnName);
    if (parentIndex == null)
      throw new RuntimeException("Cant find station " + stnName);

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMember(timeName, null, null, DataType.DOUBLE, timeCoordValue);
    coords.addMember(stationIndexName, null, null, DataType.INT, parentIndex);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(sdata);

    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
