/* Copyright Unidata */
package ucar.nc2.internal.iosp.netcdf3;

import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.Optional;
import ucar.array.Storage;
import ucar.array.StructureData;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructureBB;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.StructureMembers;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.iosp.LayoutRegularSegmented;
import ucar.nc2.internal.iosp.netcdf3.N3header.Vinfo;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import javax.annotation.Nullable;

/** Netcdf 3 version iosp, using Builders for immutability. */
public class N3iosp extends AbstractIOServiceProvider implements IOServiceProvider {
  protected static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3iosp.class);

  /*
   * CLASSIC
   * The maximum size of a record in the classic format in versions 3.5.1 and earlier is 2^32 - 4 bytes.
   * In versions 3.6.0 and later, there is no such restriction on total record size for the classic format
   * or 64-bit offset format.
   *
   * If you don't use the unlimited dimension, only one variable can exceed 2 GiB in size, but it can be as
   * large as the underlying file system permits. It must be the last variable in the dataset, and the offset
   * to the beginning of this variable must be less than about 2 GiB.
   *
   * The limit is really 2^31 - 4. If you were to specify a variable size of 2^31 -3, for example, it would be
   * rounded up to the nearest multiple of 4 bytes, which would be 2^31, which is larger than the largest
   * signed integer, 2^31 - 1.
   *
   * If you use the unlimited dimension, record variables may exceed 2 GiB in size, as long as the offset of the
   * start of each record variable within a record is less than 2 GiB - 4.
   */

  /*
   * LARGE FILE
   * Assuming an operating system with Large File Support, the following restrictions apply to the netCDF 64-bit offset
   * format.
   *
   * No fixed-size variable can require more than 2^32 - 4 bytes of storage for its data, unless it is the last
   * fixed-size variable and there are no record variables. When there are no record variables, the last
   * fixed-size variable can be any size supported by the file system, e.g. terabytes.
   *
   * A 64-bit offset format netCDF file can have up to 2^32 - 1 fixed sized variables, each under 4GiB in size.
   * If there are no record variables in the file the last fixed variable can be any size.
   *
   * No record variable can require more than 2^32 - 4 bytes of storage for each record's worth of data,
   * unless it is the last record variable. A 64-bit offset format netCDF file can have up to 2^32 - 1 records,
   * of up to 2^32 - 1 variables, as long as the size of one record's data for each record variable except the
   * last is less than 4 GiB - 4.
   *
   * Note also that all netCDF variables and records are padded to 4 byte boundaries.
   */

  protected N3header header;
  protected long lastModified; // used by sync
  private final boolean debugRecord = false;
  private Charset valueCharset;

  @Override
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return N3header.isValidFile(raf);
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s", super.getDetailInfo());

    try {
      header.showDetail(f);
    } catch (IOException e) {
      return e.getMessage();
    }

    return f.toString();

  }

  // properties
  boolean useRecordStructure;

  //////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void close() throws java.io.IOException {
    if (raf != null) {
      if (header != null) {
        long size = header.calcFileSize();
        raf.setMinLength(size);
      }
      raf.close();
    }
    raf = null;
  }

  @Override
  public void reacquire() throws IOException {
    super.reacquire();
    header.raf = this.raf;
  }

  @Override
  public String toStringDebug(Object o) {
    return null;
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message instanceof Charset) {
      setValueCharset((Charset) message);
      return Boolean.TRUE;
    }
    if (message == NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE) {
      // LOOK does this work? must be sent before construction????
      this.useRecordStructure = true;
      return Boolean.TRUE;
    }
    if (message.equals(IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT)) {
      return header.useLongOffset ? NetcdfFileFormat.NETCDF3_64BIT_OFFSET : NetcdfFileFormat.NETCDF3;
    }
    return super.sendIospMessage(message);
  }

  /**
   * Return {@link Charset value charset} if it was defined. Definition of charset
   * occurs by sending a charset as a message using the {@link #sendIospMessage}
   * method.
   *
   * @return {@link Charset value charset} if it was defined.
   */
  Optional<Charset> getValueCharset() {
    return Optional.ofNullable(valueCharset);
  }

  /**
   * Define {@link Charset value charset}.
   *
   * @param charset may be null.
   */
  private void setValueCharset(@Nullable Charset charset) {
    this.valueCharset = charset;
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.NETCDF.getDescription();
  }

  @Override
  public String getFileTypeDescription() {
    return "NetCDF-3/CDM";
  }

  @Override
  public String getFileTypeVersion() {
    return "1";
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // open existing file

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, rootGroup.getNcfile(), cancelTask);

    String location = raf.getLocation();
    if (!location.startsWith("http:")) {
      File file = new File(location);
      if (file.exists())
        lastModified = file.lastModified();
    }

    raf.order(RandomAccessFile.BIG_ENDIAN);
    header = createHeader();
    header.read(raf, rootGroup, null);
  }

  /** Create header for reading netcdf file. */
  private N3header createHeader() {
    return new N3header(this);
  }

  /////////////////////////////////////////////////////////////////////////////
  // data reading

  @Override
  public ucar.ma2.Array readData(ucar.nc2.Variable v2, Section section) throws IOException, InvalidRangeException {
    if (v2 instanceof Structure) {
      return readStructureData((Structure) v2, section);
    }

    Object data = readDataObject(v2, section);
    return Array.factory(v2.getDataType(), section.getShape(), data);
  }

  @Override
  public ucar.array.Array<?> readArrayData(Variable v2, Section section)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {
    if (v2 instanceof Structure) {
      return readStructureDataArray((Structure) v2, section);
    }

    Object data = readDataObject(v2, section);
    return ucar.array.Arrays.factory(v2.getDataType(), section.getShape(), data);
  }

  /** Read data subset from file for a variable, create primitive array. */
  private Object readDataObject(Variable v2, Section section) throws java.io.IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section)
        : new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);

    // not possible, anyway wrong returning Array instead of primitive array
    // if (layout.getTotalNelems() == 0) {
    // return Array.factory(dataType, section.getShape());
    // }
    return IospHelper.readDataFill(raf, layout, dataType, null, null);
  }

  /**
   * Read data from record structure. For N3, this is the only possible structure, and there can be no nesting.
   * Read all variables for each record, put in ByteBuffer.
   *
   * @param s the record structure
   * @param section the record range to read
   * @return an ArrayStructure, with all the data read in.
   * @throws IOException on error
   */
  private ucar.ma2.Array readStructureData(ucar.nc2.Structure s, Section section) throws java.io.IOException {
    // has to be 1D
    Range recordRange = section.getRange(0);

    // create the ArrayStructure
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      Vinfo vinfo = (Vinfo) v2.getSPobject();
      m.setDataParam((int) (vinfo.begin - header.recStart));
    }

    // protect against too large of reads
    if (header.recsize > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Cant read records when recsize > " + Integer.MAX_VALUE);
    long nrecs = section.computeSize();
    if (nrecs * header.recsize > Integer.MAX_VALUE)
      throw new IllegalArgumentException(
          "Too large read: nrecs * recsize= " + (nrecs * header.recsize) + "bytes exceeds " + Integer.MAX_VALUE);

    members.setStructureSize((int) header.recsize);
    ArrayStructureBB structureArray = new ArrayStructureBB(members, new int[] {recordRange.length()});

    // loop over records
    byte[] result = structureArray.getByteBuffer().array();
    int count = 0;
    for (int recnum : recordRange) {
      if (debugRecord)
        System.out.println(" read record " + recnum);
      raf.seek(header.recStart + recnum * header.recsize); // where the record starts

      if (recnum != header.numrecs - 1) {
        raf.readFully(result, (int) (count * header.recsize), (int) header.recsize);
      } else {
        // "wart" allows file to be one byte short. since its always padding, we allow
        raf.read(result, (int) (count * header.recsize), (int) header.recsize);
      }
      count++;
    }

    return structureArray;
  }

  private ucar.array.Array<ucar.array.StructureData> readStructureDataArray(ucar.nc2.Structure s, Section section)
      throws java.io.IOException {
    // has to be 1D
    Preconditions.checkArgument(section.getRank() == 1);
    Range recordRange = section.getRange(0);

    // create the StructureMembers
    ucar.array.StructureMembers.Builder membersb = ucar.array.StructureMembers.makeStructureMembers(s);
    for (ucar.array.StructureMembers.MemberBuilder m : membersb.getStructureMembers()) {
      Variable v2 = s.findVariable(m.getName());
      Vinfo vinfo = (Vinfo) v2.getSPobject();
      m.setOffset((int) (vinfo.begin - header.recStart));
    }

    // protect against too large of reads
    if (header.recsize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Cant read records when recsize > " + Integer.MAX_VALUE);
    }
    long nrecs = section.computeSize();
    if (nrecs * header.recsize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Too large read: nrecs * recsize= " + (nrecs * header.recsize) + " bytes exceeds " + Integer.MAX_VALUE);
    }
    membersb.setStructureSize((int) header.recsize);

    byte[] result = new byte[(int) (nrecs * header.recsize)];
    int rcount = 0;
    // loop over records
    for (int recnum : recordRange) {
      raf.seek(header.recStart + recnum * header.recsize); // where the record starts

      if (recnum != header.numrecs - 1) {
        raf.readFully(result, (int) (rcount * header.recsize), (int) header.recsize);
      } else {
        // "wart" allows file to be one byte short. since its always padding, we allow
        raf.read(result, (int) (rcount * header.recsize), (int) header.recsize);
      }
      rcount++;
    }

    ucar.array.StructureMembers members = membersb.build();
    Storage<StructureData> storage =
        new ucar.array.StructureDataStorageBB(members, ByteBuffer.wrap(result), (int) section.getSize());
    return new ucar.array.StructureDataArray(members, section.getShape(), storage);
  }
}
