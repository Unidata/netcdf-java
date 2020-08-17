/* Copyright Unidata */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import thredds.inventory.MFile;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.internal.ncml.Aggregation.Type;
import ucar.nc2.internal.ncml.AggregationOuter.CacheVar;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateFromString;
import ucar.nc2.util.CancelTask;

/**
 * Encapsulates a NetcdfFile that is a component of the aggregation.
 */
class AggDatasetOuter extends AggDataset {
  private final AggregationOuter aggregationOuter;
  @Nullable
  final String coordValue; // if theres a coordValue on the netcdf element - may be multiple, blank seperated
  final Date coordValueDate; // if its a date
  final boolean isStringValued; // if coordinat is a String

  // not final because of deffered read
  int ncoord; // number of coordinates in outer dimension
  int aggStart, aggEnd; // index in aggregated dataset; aggStart <= i < aggEnd

  /**
   * Dataset constructor.
   * With this constructor, the actual opening of the dataset is deferred, and done by the reader.
   * Used with explicit netcdf elements, and scanned files.
   *
   * @param cacheName a unique name to use for caching
   * @param location attribute "location" on the netcdf element
   * @param id attribute "id" on the netcdf element
   * @param ncoordS attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param enhance open dataset in enhance mode NOT USED
   * @param reader factory for reading this netcdf dataset; if null, use NetcdfDatasets.open( location)
   */
  AggDatasetOuter(AggregationOuter aggregationOuter, String cacheName, String location, String id, String ncoordS,
      String coordValueS, EnumSet<Enhance> enhance, ucar.nc2.internal.cache.FileFactory reader) {

    super(cacheName, location, id, enhance, reader, aggregationOuter.spiObject, aggregationOuter.ncmlElem);
    this.aggregationOuter = aggregationOuter;

    if ((aggregationOuter.type == Type.joinNew) || (aggregationOuter.type == Type.joinExistingOne)) {
      this.ncoord = 1;
    }
    if (ncoordS != null) {
      try {
        this.ncoord = Integer.parseInt(ncoordS);
      } catch (NumberFormatException e) {
        Aggregation.logger.error("bad ncoord attribute on dataset=" + location);
      }
    }

    boolean isString = false;
    if ((aggregationOuter.type == Type.joinNew) || (aggregationOuter.type == Type.joinExistingOne)
        || (aggregationOuter.type == Type.forecastModelRunCollection)) {
      if (coordValueS == null) {
        coordValueS = extractCoordNameFromFilename(this.getLocation());
        isString = true;
      } else {
        // we just need to know if its string valued
        try {
          Double.parseDouble(coordValueS);
        } catch (NumberFormatException e) {
          isString = true;
        }
      }
    }

    // allow coordValue attribute on JOIN_EXISTING, may be multiple values seperated by blanks or commas
    if ((aggregationOuter.type == Type.joinExisting) && (coordValueS != null)) {
      StringTokenizer stoker = new StringTokenizer(coordValueS, " ,");
      this.ncoord = stoker.countTokens();
    }

    this.isStringValued = isString; // LOOK ??
    this.coordValue = coordValueS;
    this.coordValueDate = null; // LOOK why isnt this set?
  }

  private String extractCoordNameFromFilename(String loc) {
    int pos = loc.lastIndexOf('/');
    String result = (pos < 0) ? loc : loc.substring(pos + 1);
    pos = result.lastIndexOf('#');
    if (pos > 0)
      result = result.substring(0, pos);
    return result;
  }

  AggDatasetOuter(AggregationOuter aggregationOuter, MFile cd) {
    super(cd, aggregationOuter.spiObject, aggregationOuter.ncmlElem);
    this.aggregationOuter = aggregationOuter;

    if ((aggregationOuter.type == Type.joinNew) || (aggregationOuter.type == Type.joinExistingOne)) {
      this.ncoord = 1;
    }
    String coordValueS = null;
    // default is that the coordinates are just the filenames
    // this can be overriden by an explicit declaration, which will replace the variable after ther agg is processed in
    // NcMLReader
    if ((aggregationOuter.type == Type.joinNew) || (aggregationOuter.type == Type.joinExistingOne)
        || (aggregationOuter.type == Type.forecastModelRunCollection)) {
      coordValueS = extractCoordNameFromFilename(this.getLocation());
      this.isStringValued = true;
    } else {
      this.isStringValued = false; // LOOK ??
    }

    if (null != aggregationOuter.dateFormatMark) {
      String filename = cd.getName(); // LOOK operates on name, not path
      coordValueDate = DateFromString.getDateUsingDemarkatedCount(filename, aggregationOuter.dateFormatMark, '#');
      coordValueS = CalendarDateFormatter.toDateTimeStringISO(coordValueDate);
      if (Aggregation.debugDateParse)
        System.out.println("  adding " + cd.getPath() + " date= " + coordValueS);

    } else {
      coordValueDate = null;
      if (Aggregation.debugDateParse)
        System.out.println("  adding " + cd.getPath());
    }

    if ((coordValueS == null) && (aggregationOuter.type == Type.joinNew)) // use filename as coord value
      coordValueS = cd.getName();

    this.coordValue = coordValueS;
  }

  /**
   * Get the coordinate value(s) as a String for this Dataset
   *
   * @return the coordinate value(s) as a String
   */
  public String getCoordValueString() {
    return coordValue;
  }

  /**
   * Get the coordinate value as a Date for this Dataset; may be null
   *
   * @return the coordinate value as a Date, or null
   */
  public Date getCoordValueDate() {
    return coordValueDate;
  }

  public void show(Formatter f) {
    f.format("   %s", getLocation());
    if (coordValue != null)
      f.format(" coordValue='%s'", coordValue);
    if (coordValueDate != null)
      f.format(" coordValueDate='%s'", CalendarDateFormatter.toDateTimeString(coordValueDate));
    f.format(" range=[%d:%d) (%d)%n", aggStart, aggEnd, ncoord);
  }

  /**
   * Get number of coordinates in this Dataset.
   * If not already set, open the file and get it from the aggregation dimension.
   *
   * @param cancelTask allow cancellation
   * @return number of coordinates in this Dataset.
   * @throws IOException if io error
   */
  public int getNcoords(CancelTask cancelTask) throws IOException {
    if (ncoord <= 0) {
      try (NetcdfFile ncd = acquireFile(cancelTask)) {
        if ((cancelTask != null) && cancelTask.isCancel())
          return 0;

        Dimension d = ncd.findDimension(aggregationOuter.dimName); // long name of dimension
        if (d != null)
          ncoord = d.getLength();
        else
          throw new IllegalArgumentException("Dimension not found= " + aggregationOuter.dimName);
      }
    }
    return ncoord;
  }

  /**
   * Set the starting and ending index into the aggregation dimension
   *
   * @param aggStart starting index
   * @param cancelTask allow to bail out
   * @return number of coordinates in this dataset
   * @throws IOException if io error
   */
  protected int setStartEnd(int aggStart, CancelTask cancelTask) throws IOException {
    this.aggStart = aggStart;
    this.aggEnd = aggStart + getNcoords(cancelTask);
    return ncoord;
  }

  /**
   * Get the desired Range, reletive to this Dataset, if no overlap, return null.
   * <p>
   * wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd.
   * if this overlaps, set the Range required for the nested dataset.
   * note this should handle strides ok.
   *
   * @param totalRange desired range, reletive to aggregated dimension.
   * @return desired Range or null if theres nothing wanted from this datase.
   * @throws InvalidRangeException if invalid range request
   */
  protected Range getNestedJoinRange(Range totalRange) throws InvalidRangeException {
    int wantStart = totalRange.first();
    int wantStop = totalRange.last() + 1; // Range has last inclusive, we use last exclusive

    // see if this dataset is needed
    if (!isNeeded(wantStart, wantStop))
      return null;

    int firstInInterval = totalRange.getFirstInInterval(aggStart);
    if ((firstInInterval < 0) || (firstInInterval >= aggEnd))
      return null;

    int start = Math.max(firstInInterval, wantStart) - aggStart;
    int stop = Math.min(aggEnd, wantStop) - aggStart;

    return new Range(start, stop - 1, totalRange.stride()); // Range has last inclusive
  }

  protected boolean isNeeded(Range totalRange) {
    int wantStart = totalRange.first();
    int wantStop = totalRange.last() + 1; // Range has last inclusive, we use last exclusive
    return isNeeded(wantStart, wantStop);
  }

  // wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd
  // find out if this overlaps this nested Dataset indices
  private boolean isNeeded(int wantStart, int wantStop) {
    if (wantStart >= wantStop)
      return false;
    return (wantStart < aggEnd) && (wantStop > aggStart);

  }

  /*
   * @Override
   * protected void cacheCoordValues(NetcdfFile ncfile) throws IOException {
   * if (coordValue != null) return;
   *
   * Variable coordVar = ncfile.findVariable(dimName);
   * if (coordVar != null) {
   * Array data = coordVar.read();
   * coordValue = data.toString();
   * }
   *
   * }
   */

  // read any cached variables that need it

  @Override
  protected void cacheVariables(NetcdfFile ncfile) throws IOException {
    for (CacheVar pv : aggregationOuter.cacheList) {
      pv.read(this, ncfile);
    }
  }

  @Override
  protected Array read(Variable mainv, CancelTask cancelTask, List<Range> section)
      throws IOException, InvalidRangeException {
    NetcdfFile ncd = null;
    try {
      ncd = acquireFile(cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Variable v = findVariable(ncd, mainv);
      if (v == null) {
        Aggregation.logger.error("AggOuterDimension cant find " + mainv.getFullName() + " in " + ncd.getLocation()
            + "; return all zeroes!!!");
        return Array.factory(mainv.getDataType(), new Section(section).getShape()); // all zeros LOOK need missing
                                                                                    // value
      }

      if (Aggregation.debugRead) {
        Section want = new Section(section);
        System.out.printf("AggOuter.read(%s) %s from %s in %s%n", want, mainv.getNameAndDimensions(),
            v.getNameAndDimensions(), getLocation());
      }

      // its possible that we are asking for more of the time coordinate than actually exists (fmrc ragged time)
      // so we need to read only what is there
      Range fullRange = v.getRanges().get(0);
      Range want = section.get(0);
      if (fullRange.last() < want.last()) {
        Range limitRange = new Range(want.first(), fullRange.last(), want.stride());
        section = new ArrayList<>(section); // make a copy
        section.set(0, limitRange);
      }

      return v.read(section);

    } finally {
      close(ncd);
    }
  }

  @Override
  public int compareTo(AggDataset o) {
    if (o instanceof AggDatasetOuter && coordValueDate != null) {
      return coordValueDate.compareTo(((AggDatasetOuter) o).coordValueDate);
    } else {
      return super.compareTo(o);
    }
  }
}
