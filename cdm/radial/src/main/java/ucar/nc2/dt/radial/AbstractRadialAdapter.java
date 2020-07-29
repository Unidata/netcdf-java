/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.radial;

import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.*;
import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.DataType;
import java.util.*;
import java.io.IOException;

/**
 * Make a NetcdfDataset into a RadialDatasetSweep.
 */
public abstract class AbstractRadialAdapter implements RadialDatasetSweep, FeatureDatasetFactory {
  protected NetcdfDataset netcdfDataset;
  protected String title, desc, location;
  protected Date startDate, endDate;
  protected LatLonRect boundingBox;
  protected List<VariableSimpleIF> dataVariables = new ArrayList<>();
  protected StringBuffer parseInfo = new StringBuffer();

  protected ucar.unidata.geoloc.EarthLocation origin;
  protected HashMap csHash = new HashMap();
  protected ucar.nc2.units.DateUnit dateUnits;
  protected ucar.nc2.time.CalendarDateUnit calDateUnits;
  protected FileCacheIF fileCache;

  public AbstractRadialAdapter() {}

  public AbstractRadialAdapter(NetcdfDataset ds) {
    this.netcdfDataset = ds;
    this.location = netcdfDataset.getLocation();

    this.title = netcdfDataset.getTitle();
    if (title == null)
      title = netcdfDataset.getRootGroup().findAttributeString("title", null);
    if (desc == null)
      desc = netcdfDataset.getRootGroup().findAttributeString("description", null);

    // look for radial data variables
    parseInfo.append("RadialDatasetAdapter look for RadialVariables\n");
    for (Variable var : ds.getVariables()) {
      addRadialVariable(ds, var);
    }
  }

  protected abstract void addRadialVariable(NetcdfDataset ds, Variable var);

  protected abstract RadialVariable makeRadialVariable(NetcdfDataset nds, Variable v0);

  protected abstract void setTimeUnits() throws Exception; // reminder for subclasses to set this

  protected abstract void setEarthLocation(); // reminder for subclasses to set this

  protected abstract void setStartDate(); // reminder for subclasses to set this

  protected abstract void setEndDate(); // reminder for subclasses to set this

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String desc) {
    this.desc = desc;
  }

  public void setLocationURI(String location) {
    this.location = location;
  }

  protected void removeDataVariable(String varName) {
    Iterator iter = dataVariables.iterator();
    while (iter.hasNext()) {
      VariableSimpleIF v = (VariableSimpleIF) iter.next();
      if (v.getShortName().equals(varName))
        iter.remove();
    }
  }

  // you must set EarthLocation before you call this.
  protected void setBoundingBox() {
    LatLonRect.Builder largestBB = null;
    // look through all the coord systems
    for (Object o : csHash.values()) {
      RadialCoordSys sys = (RadialCoordSys) o;
      sys.setOrigin(origin);
      LatLonRect bb = sys.getBoundingBox();
      if (largestBB == null) {
        largestBB = bb.toBuilder();
      } else {
        largestBB = largestBB.extend(bb);
      }
    }

    boundingBox = largestBB == null ? null : largestBB.build();
  }


  public void calcBounds() throws java.io.IOException {
    setBoundingBox();
    try {
      setTimeUnits();
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
    setStartDate();
    setEndDate();
  }

  //////////////////////////////////////////////////////////////////////////

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public LatLonRect getBoundingBox() {
    return boundingBox;
  }

  public List<VariableSimpleIF> getDataVariables() {
    return dataVariables;
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    for (VariableSimpleIF s : dataVariables) {
      String ss = s.getShortName();
      if (shortName.equals(ss))
        return s;
    }
    return null;
  }


  public RadialDatasetSweep.Type getCommonType() {
    return null;
  }

  public ucar.nc2.units.DateUnit getTimeUnits() {
    return dateUnits;
  }

  public ucar.nc2.time.CalendarDateUnit getCalendarDateUnit() {
    return calDateUnits;
  }

  public ucar.unidata.geoloc.EarthLocation getEarthLocation() {
    return origin;
  }


  /////////////////////////////////////////////
  // FeatureDatasetFactory

  public FeatureType[] getFeatureTypes() {
    return new FeatureType[] {FeatureType.RADIAL};
  }

  // FeatureDataset
  public FeatureType getFeatureType() {
    return FeatureType.RADIAL;
  }

  public DateRange getDateRange() {
    return new DateRange(getStartDate(), getEndDate());
  }

  public CalendarDateRange getCalendarDateRange() {
    return CalendarDateRange.of(getStartDate(), getEndDate());
  }

  public CalendarDate getCalendarDateStart() {
    return CalendarDate.of(getStartDate());
  }

  public CalendarDate getCalendarDateEnd() {
    return CalendarDate.of(getEndDate());
  }

  public void getDetailInfo(Formatter sf) {
    sf.format("%s", getDetailInfo());
  }

  public String getImplementationName() {
    return getClass().getName();
  }

  //////////////////////////////////////////////////
  // FileCacheable

  /** @deprecated do not use */
  @Deprecated
  @Override
  public synchronized void setFileCache(FileCacheIF fileCache) {
    this.fileCache = fileCache;
  }

  @Override
  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      if (fileCache.release(this))
        return;
    }

    try {
      if (netcdfDataset != null)
        netcdfDataset.close();
    } finally {
      netcdfDataset = null;
    }
  }

  /** @deprecated do not use */
  @Deprecated
  public void release() throws IOException {
    if (netcdfDataset != null)
      netcdfDataset.release();
  }

  /** @deprecated do not use */
  @Deprecated
  public void reacquire() throws IOException {
    if (netcdfDataset != null)
      netcdfDataset.reacquire();
  }

  @Override
  public long getLastModified() {
    return (netcdfDataset != null) ? netcdfDataset.getLastModified() : 0;
  }

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() {
    return netcdfDataset;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return desc;
  }

  public String getLocationURI() {
    return location;
  }

  public String getLocation() {
    return location;
  }

  @Override
  public AttributeContainer attributes() {
    return netcdfDataset != null ? netcdfDataset.getRootGroup().attributes()
        : new AttributeContainerMutable(getRadarName()).toImmutable();
  }

  /** @deprecated use attributes() */
  @Deprecated
  public List<Attribute> getGlobalAttributes() {
    if (netcdfDataset == null)
      return new ArrayList<>();
    return netcdfDataset.getGlobalAttributes();
  }

  /** @deprecated use attributes() */
  @Deprecated
  public Attribute findGlobalAttributeIgnoreCase(String name) {
    if (netcdfDataset == null)
      return null;
    return netcdfDataset.findGlobalAttributeIgnoreCase(name);
  }

  public String getDetailInfo() {
    StringBuilder sbuff = new StringBuilder();

    sbuff.append(" Radar ID = " + getRadarID() + "\n");
    sbuff.append(" Radar Name = " + getRadarName() + "\n");
    sbuff.append(" Data Format Name= " + getDataFormat() + "\n");
    sbuff.append(" Common Type = " + getCommonType() + "\n");
    sbuff.append(" Common Origin = " + getCommonOrigin() + "\n");
    CalendarDateUnit dt = getCalendarDateUnit();
    if (dt != null)
      sbuff.append(" Date Unit = " + dt + "\n");
    sbuff.append(" isStationary = " + isStationary() + "\n");
    // sbuff.append(" isRadial = "+isRadial()+"\n");
    sbuff.append(" isVolume = " + isVolume() + "\n");
    sbuff.append("\n");

    sbuff.append("  location= ").append(getLocation()).append("\n");
    sbuff.append("  title= ").append(getTitle()).append("\n");
    sbuff.append("  desc= ").append(getDescription()).append("\n");
    sbuff.append("  start= ").append(CalendarDateFormatter.toDateTimeString(getStartDate())).append("\n");
    sbuff.append("  end  = ").append(CalendarDateFormatter.toDateTimeString(getEndDate())).append("\n");
    sbuff.append("  bb   = ").append(getBoundingBox()).append("\n");
    if (getBoundingBox() != null)
      sbuff.append("  bb   = ").append(getBoundingBox().toString2()).append("\n");

    sbuff.append("  has netcdf = ").append(getNetcdfFile() != null).append("\n");
    if (!attributes().isEmpty()) {
      sbuff.append("  Attributes\n");
      for (Attribute a : attributes()) {
        sbuff.append("    ").append(a).append("\n");
      }
    }

    List<VariableSimpleIF> vars = getDataVariables();
    sbuff.append("  Variables (").append(vars.size()).append(")\n");
    for (VariableSimpleIF v : vars) {
      sbuff.append("    name='").append(v.getShortName()).append("' desc='").append(v.getDescription())
          .append("' units='").append(v.getUnitsString()).append("' type=").append(v.getDataType()).append("\n");
    }

    sbuff.append("\nparseInfo=\n");
    sbuff.append(parseInfo);
    sbuff.append("\n");

    return sbuff.toString();
  }

  ///////////////////////////////////////////////////////////////////////
  public static class MyRadialVariableAdapter implements VariableSimpleIF {
    private int rank;
    private int[] shape;
    protected String name;
    private String desc;
    private String units;
    private AttributeContainer attributes;

    public MyRadialVariableAdapter(String vName, Variable v) {
      rank = 1;
      shape = new int[] {1};
      name = vName;
      desc = v.getDescription();
      units = v.getUnitsString();
      attributes = v.attributes();
    }

    public String toString() {
      return name;
    }

    /** Sort by full name */
    public int compareTo(VariableSimpleIF o) {
      return getFullName().compareTo(o.getFullName());
    }

    public String getName() {
      return this.name;
    }

    public String getFullName() {
      return this.name;
    }

    public String getShortName() {
      return this.name;
    }

    public DataType getDataType() {
      return DataType.FLOAT;
    }

    public String getDescription() {
      return this.desc;
    }

    public String getInfo() {
      return this.desc;
    }

    public String getUnitsString() {
      return units;
    }

    public int getRank() {
      return this.rank;
    }

    public int[] getShape() {
      return this.shape;
    }

    public List<Dimension> getDimensions() {
      return null;
    }

    public ucar.nc2.Attribute findAttributeIgnoreCase(String attName) {
      return attributes.findAttributeIgnoreCase(attName);
    }

    @Override
    public AttributeContainer attributes() {
      return new AttributeContainerMutable(name, attributes).toImmutable();
    }
  }


}
