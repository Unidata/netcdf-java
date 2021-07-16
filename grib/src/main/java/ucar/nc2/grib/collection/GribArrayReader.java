/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.RangeIterator;
import ucar.array.SectionIterable;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Parameter;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.grid2.GridSubset;
import ucar.unidata.io.RandomAccessFile;

/**
 * Grib Data Reader.
 * Split from GribIosp, so can be used by GribCoverage.
 *
 * @author caron
 * @since 4/6/11
 */
@Immutable
public abstract class GribArrayReader {
  private static final Logger logger = LoggerFactory.getLogger(GribArrayReader.class);

  public static GribArrayReader factory(GribCollectionImmutable gribCollection,
      GribCollectionImmutable.VariableIndex vindex) {
    if (gribCollection.isGrib1)
      return new Grib1ArrayReader(gribCollection, vindex);
    else
      return new Grib2ArrayReader(gribCollection, vindex);
  }

  protected abstract float[] readData(RandomAccessFile rafData, GribReaderRecord dr) throws IOException;

  protected abstract void show(RandomAccessFile rafData, long dataPos) throws IOException;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static GribCollectionImmutable.Record currentDataRecord;
  public static GribDataValidator validator;
  public static String currentDataRafFilename;
  private static final boolean show = false; // debug

  protected final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.VariableIndex vindex;
  private final List<GribReaderRecord> records = new ArrayList<>();

  protected GribArrayReader(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
    this.gribCollection = gribCollection;
    this.vindex = vindex;
  }

  /**
   * Read the section of data described by want
   * 
   * @param want which data do you want?
   * @return data as an Array<?>
   */
  public Array<?> readData(ucar.array.SectionIterable want) throws IOException, InvalidRangeException {
    if (vindex instanceof PartitionCollectionImmutable.VariableIndexPartitioned)
      return readDataFromPartition((PartitionCollectionImmutable.VariableIndexPartitioned) vindex, want);
    else
      return readDataFromCollection(vindex, want);
  }

  /*
   * SectionIterable iterates over the source indexes, corresponding to vindex's SparseArray.
   * IOSP: works because variable coordinate corresponds 1-1 to Grib Coordinate.
   * GribCoverage: must translate coordinates to Grib Coordinate index.
   * LOOK want.getShape() indicates the result Array shape, may not be rectangular? How does Array represent that case?
   * old way uses NaN. Could be Vlen ?? COuld restrict subset to only swuare, eg 1 runtime, 1 time.
   * could specialize FmrcTimeCoordinateSystem
   * SectionIterable.next(int[] index) is not used here.
   */
  private Array<?> readDataFromCollection(GribCollectionImmutable.VariableIndex vindex, SectionIterable want)
      throws IOException {
    // first time, read records and keep in memory
    vindex.readRecords();

    int rank = want.getRank();
    int sectionLen = rank - 2; // all but x, y
    SectionIterable sectionWanted = want.subSection(0, sectionLen);
    // assert sectionLen == vindex.getRank(); LOOK true or false ??

    // collect all the records that need to be read
    int resultIndex = 0;
    for (int sourceIndex : sectionWanted) {
      // addRecord(sourceIndex, count++);
      GribCollectionImmutable.Record record = vindex.getRecordAt(sourceIndex);
      if (Grib.debugRead)
        logger.debug("GribIosp debugRead sourceIndex={} resultIndex={} record is null={}", sourceIndex, resultIndex,
            record == null);
      if (record != null)
        records.add(new GribReaderRecord(resultIndex, record, vindex.group.getGdsHorizCoordSys()));
      resultIndex++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = new DataReceiver(want.getShape(), want.getRange(rank - 2), want.getRange(rank - 1));
    read(dataReceiver);
    return dataReceiver.getArray();
  }

  /*
   * Iterates using SectionIterable.next(int[] index).
   * The work of translating that down into partition heirarchy and finally to a GC is all in
   * VariableIndexPartitioned.getDataRecord(int[] index)
   * want.getShape() indicates the result Array shape.
   */
  private Array<?> readDataFromPartition(PartitionCollectionImmutable.VariableIndexPartitioned vindexP,
      ucar.array.SectionIterable section) throws IOException {

    int rank = section.getRank();
    ucar.array.SectionIterable sectionWanted = section.subSection(0, rank - 2); // all but x, y
    ucar.array.SectionIterable.SectionIterator iterWanted = sectionWanted.getIterator(); // iterator over wanted indices
                                                                                         // in vindexP
    int[] indexWanted = new int[rank - 2]; // place to put the iterator result
    int[] useIndex = indexWanted;

    // collect all the records that need to be read
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted); // returns the vindexP index in indexWanted array

      // for MRUTP, must munge the index here (not in vindexP.getDataRecord, because its recursive
      if (vindexP.getType() == GribCollectionImmutable.Type.MRUTP) {
        // find the partition from getRuntimeIdxFromMrutpTimeIndex
        CoordinateTime2D time2D = (CoordinateTime2D) vindexP.getCoordinateTime();
        assert time2D != null;
        int[] timeIndices = time2D.getTimeIndicesFromMrutp(indexWanted[0]);

        int[] indexReallyWanted = new int[indexWanted.length + 1];
        indexReallyWanted[0] = timeIndices[0];
        indexReallyWanted[1] = timeIndices[1];
        System.arraycopy(indexWanted, 1, indexReallyWanted, 2, indexWanted.length - 1);
        useIndex = indexReallyWanted;
      }

      PartitionCollectionImmutable.DataRecord record = vindexP.getDataRecord(useIndex);
      if (record == null) {
        if (Grib.debugRead)
          logger.debug("readDataFromPartition missing data%n");
        resultPos++; // can just skip, since result is prefilled with NaNs
        continue;
      }
      record.resultIndex = resultPos;
      records.add(record);
      resultPos++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver =
        new DataReceiver(section.getShape(), section.getRange(rank - 2), section.getRange(rank - 1));
    readPartitioned(dataReceiver);

    return dataReceiver.getArray();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read all of the data records that have been added.
   * The full (x,y) record is read, the reciever will subset the (x, y) as needed.
   * 
   * @param dataReceiver send data here.
   */
  private void read(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    int currFile = -1;
    RandomAccessFile rafData = null;
    try {
      for (GribReaderRecord dr : records) {
        if (Grib.debugIndexOnly || Grib.debugGbxIndexOnly) {
          GribIosp.debugIndexOnlyCount++;
          currentDataRecord = dr.record;
          currentDataRafFilename = gribCollection.getDataRafFilename(dr.record.fileno);
          if (Grib.debugIndexOnlyShow)
            dr.show(gribCollection);
          dataReceiver.setDataToZero();
          continue;
        }

        if (dr.record.fileno != currFile) {
          if (rafData != null)
            rafData.close();
          rafData = gribCollection.getDataRaf(dr.record.fileno);
          currFile = dr.record.fileno;
        }

        if (dr.record.pos == GribCollectionMutable.MISSING_RECORD)
          continue;

        if (GribArrayReader.validator != null && dr.validation != null && rafData != null) {
          GribArrayReader.validator.validate(gribCollection.cust, rafData, dr.record.pos + dr.record.drsOffset,
              dr.validation);

        } else if (show && rafData != null) { // for validation
          show(dr.validation);
          show(rafData, dr.record.pos + dr.record.drsOffset);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = vindex.group.getGdsHorizCoordSys();
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null)
        rafData.close(); // make sure its closed even on exception
    }
  }

  private void show(GridSubset validation) {
    if (validation == null)
      return;
    System.out.printf("Coords wanted%n %s", validation);
  }

  private void readPartitioned(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    PartitionCollectionImmutable.DataRecord lastRecord = null;
    RandomAccessFile rafData = null;
    try {

      for (GribReaderRecord dr : records) {
        PartitionCollectionImmutable.DataRecord drp = (PartitionCollectionImmutable.DataRecord) dr;
        if (Grib.debugIndexOnly || Grib.debugGbxIndexOnly) {
          GribIosp.debugIndexOnlyCount++;
          if (Grib.debugIndexOnlyShow)
            drp.show();
          dataReceiver.setDataToZero();
          continue;
        }

        if ((rafData == null) || !drp.usesSameFile(lastRecord)) {
          if (rafData != null)
            rafData.close();
          rafData = drp.usePartition.getRaf(drp.partno, dr.record.fileno);
        }
        lastRecord = drp;

        if (dr.record.pos == GribCollectionMutable.MISSING_RECORD)
          continue;

        if (GribArrayReader.validator != null && dr.validation != null) {
          GribArrayReader.validator.validate(gribCollection.cust, rafData, dr.record.pos + dr.record.drsOffset,
              dr.validation);
        } else if (show) { // for validation
          show(dr.validation);
          show(rafData, dr.record.pos + dr.record.drsOffset);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = dr.hcs;
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null)
        rafData.close(); // make sure its closed even on exception
    }
  }

  public interface DataReceiverIF {
    void addData(float[] data, int resultIndex, int nx);

    void setDataToZero(); // only used when debugging with gbx/ncx only, to fake the data

    Array<?> getArray();
  }

  public static class DataReceiver implements DataReceiverIF {
    private final RangeIterator yRange;
    private final RangeIterator xRange;
    private final int horizSize;
    private final float[] dataArray;
    private final int[] shape;

    DataReceiver(int[] shape, RangeIterator yRange, RangeIterator xRange) {
      this.shape = shape;
      this.yRange = yRange;
      this.xRange = xRange;
      this.horizSize = yRange.length() * xRange.length();

      long len = Arrays.computeSize(shape);
      if (len > 100 * 1000 * 1000 * 4) { // LOOK make configurable
        logger.debug("Len greater that 100MB shape={}%n{}", java.util.Arrays.toString(shape),
            Throwables.getStackTraceAsString(new Throwable()));
        throw new IllegalArgumentException("RequestTooLarge: Len greater that 100M ");
      }
      this.dataArray = new float[(int) len];
      java.util.Arrays.fill(this.dataArray, Float.NaN); // prefill primitive array
    }

    @Override
    public void addData(float[] data, int resultIndex, int nx) {
      int start = resultIndex * horizSize;
      int count = 0;
      for (int y : yRange) {
        for (int x : xRange) {
          int dataIdx = y * nx + x;
          // dataArray.setFloat(start + count, data[dataIdx]);
          this.dataArray[start + count] = data[dataIdx];
          count++;
        }
      }
    }

    // optimization
    @Override
    public void setDataToZero() {
      java.util.Arrays.fill(this.dataArray, 0.0f);
    }

    @Override
    public Array<?> getArray() {
      return Arrays.factory(ArrayType.FLOAT, shape, dataArray);
    }
  }

  /////////////////////////////////////////////////////////

  private static class Grib2ArrayReader extends GribArrayReader {
    private final Grib2Tables cust;

    Grib2ArrayReader(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib2Tables) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, GribReaderRecord dr) throws IOException {
      GdsHorizCoordSys hcs = dr.hcs;
      long dataPos = dr.record.pos + dr.record.drsOffset;
      long bmsPos = (dr.record.bmsOffset > 0) ? dr.record.pos + dr.record.bmsOffset : 0;
      return Grib2Record.readData(rafData, dataPos, bmsPos, hcs.gdsNumberPoints, hcs.getScanMode(), hcs.nxRaw,
          hcs.nyRaw, hcs.nptsInLine);
    }

    @Override
    protected void show(RandomAccessFile rafData, long pos) throws IOException {
      Grib2Record gr = Grib2RecordScanner.findRecordByDrspos(rafData, pos);
      if (gr != null) {
        Formatter f = new Formatter();
        f.format("File=%s%n", rafData.getLocation());
        f.format("  Parameter=%s%n", cust.getVariableName(gr));
        f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
        f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
        TimeCoordIntvDateValue tinv = cust.getForecastTimeInterval(gr);
        if (tinv != null)
          f.format("  TimeInterval=%s%n", tinv);
        f.format("  ");
        gr.getPDS().show(f);
        System.out.printf("%nGrib2Record.readData at drsPos %d = %s%n", pos, f.toString());
      }
    }
  }

  private static class Grib1ArrayReader extends GribArrayReader {
    private final Grib1Customizer cust;

    Grib1ArrayReader(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib1Customizer) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, GribReaderRecord dr) throws IOException {
      return Grib1Record.readData(rafData, dr.record.pos);
    }

    @Override
    protected void show(RandomAccessFile rafData, long dataPos) throws IOException {
      rafData.seek(dataPos);
      Grib1Record gr = new Grib1Record(rafData);
      Formatter f = new Formatter();
      f.format("File=%s%n", rafData.getLocation());
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      Grib1Parameter param =
          cust.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
      f.format("  Parameter=%s%n", param);
      f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
      Grib1ParamTime ptime = gr.getParamTime(cust);
      f.format("  ForecastTime=%d%n", ptime.getForecastTime());
      if (ptime.isInterval()) {
        int[] tinv = ptime.getInterval();
        f.format("  TimeInterval=(%d,%d)%n", tinv[0], tinv[1]);
      }
      f.format("%n");
      gr.getPDSsection().showPds(cust, f);
      System.out.printf("%nGrib1Record.readData at drsPos %d = %s%n", dataPos, f.toString());
    }
  }

}
