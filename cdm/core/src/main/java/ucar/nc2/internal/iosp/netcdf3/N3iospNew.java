/* Copyright Unidata */
package ucar.nc2.internal.iosp.netcdf3;

import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

import java.io.File;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.Optional;
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
import ucar.nc2.internal.iosp.netcdf3.N3headerNew.Vinfo;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;
import javax.annotation.Nullable;

/**
 * Netcdf 3 version iosp, using Builders for immutability.
 *
 * @author caron
 * @since 9/29/2019.
 */
public class N3iospNew extends AbstractIOServiceProvider implements IOServiceProvider {
  protected static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3iospNew.class);

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

  protected N3headerNew header;
  protected long lastModified; // used by sync
  private boolean debugRecord = false;

  private Charset valueCharset;

  @Override
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return N3headerNew.isValidFile(raf);
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

  //////////////////////////////////////////////////////////////////////////////////////
  // read existing file

  @Deprecated
  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    String location = raf.getLocation();
    if (!location.startsWith("http:")) {
      File file = new File(location);
      if (file.exists())
        lastModified = file.lastModified();
    }

    raf.order(RandomAccessFile.BIG_ENDIAN);
    header = createHeader();

    Group.Builder rootGroup = Group.builder().setName("").setNcfile(ncfile);
    header.read(raf, rootGroup, null);
    ncfile.setRootGroup(rootGroup.build());
    ncfile.finish();
  }

  @Override
  public boolean isBuilder() {
    return true;
  }

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
  private N3headerNew createHeader() throws IOException {
    return new N3headerNew(this);
  }

  /////////////////////////////////////////////////////////////////////////////
  // data reading

  @Override
  public Array readData(ucar.nc2.Variable v2, Section section) throws IOException, InvalidRangeException {
    if (v2 instanceof Structure)
      return readRecordData((Structure) v2, section);

    Vinfo vinfo = (Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section)
        : new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);

    if (layout.getTotalNelems() == 0) {
      return Array.factory(dataType, section.getShape());
    }

    Object data = readData(layout, dataType);
    return Array.factory(dataType, section.getShape(), data);
  }

  @Override
  public long readToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    if (v2 instanceof Structure)
      return readRecordData((Structure) v2, section, channel);

    Vinfo vinfo = (Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section)
        : new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);

    return readData(layout, dataType, channel);
  }

  // TODO
  private long readRecordData(ucar.nc2.Structure s, Section section, WritableByteChannel out)
      throws java.io.IOException {
    long count = 0;

    /*
     * RegularIndexer index = new RegularIndexer( s.getShape(), recsize, recStart, section, recsize);
     * while (index.hasNext()) {
     * Indexer.Chunk chunk = index.next();
     * count += raf.readBytes( out, chunk.getFilePos(), chunk.getNelems() * s.getElementSize());
     * }
     */

    // LOOK this is the OTW layout based on netcdf-3
    // not sure this works but should give an idea of timing
    Range recordRange = section.getRange(0);
    /*
     * int stride = recordRange.stride();
     * if (stride == 1) {
     * int first = recordRange.first();
     * int n = recordRange.length();
     * if (false) System.out.println(" read record " + first+" "+ n * header.recsize+" bytes ");
     * return raf.readToByteChannel(out, header.recStart + first * header.recsize, n * header.recsize);
     *
     * } else {
     */

    for (int recnum : recordRange) {
      if (debugRecord)
        System.out.println(" read record " + recnum);
      raf.seek(header.recStart + recnum * header.recsize); // where the record starts
      count += raf.readToByteChannel(out, header.recStart + recnum * header.recsize, header.recsize);
    }
    // }

    return count;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean syncExtend() throws IOException {
    // boolean result = header.synchNumrecs();
    // if (result && log.isDebugEnabled())
    // log.debug(" N3iosp syncExtend " + raf.getLocation() + " numrecs =" + header.numrecs);
    return true;
  }

  public void flush() throws java.io.IOException {
    if (raf != null) {
      raf.flush();
    }
  }

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
    }
    if (message == NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE) {
      this.useRecordStructure = true;
      return null;
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

  /**
   * Read data subset from file for a variable, create primitive array.
   *
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   */
  private Object readData(Layout index, DataType dataType) throws java.io.IOException {
    return IospHelper.readDataFill(raf, index, dataType, null, -1);
  }

  /**
   * Read data subset from file for a variable, to WritableByteChannel.
   * Will send as bigendian, since thats what the underlying file has.
   *
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   */
  private long readData(Layout index, DataType dataType, WritableByteChannel out) throws java.io.IOException {
    long count = 0;
    if (dataType.getPrimitiveClassType() == byte.class || dataType == DataType.CHAR) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel(out, chunk.getSrcPos(), chunk.getNelems());
      }

    } else if (dataType.getPrimitiveClassType() == short.class) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel(out, chunk.getSrcPos(), 2 * chunk.getNelems());
      }

    } else if (dataType.getPrimitiveClassType() == int.class || (dataType == DataType.FLOAT)) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel(out, chunk.getSrcPos(), 4 * chunk.getNelems());
      }

    } else if ((dataType == DataType.DOUBLE) || dataType.getPrimitiveClassType() == long.class) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        count += raf.readToByteChannel(out, chunk.getSrcPos(), 8 * chunk.getNelems());
      }
    }

    return count;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Read data from record structure. For N3, this is the only possible structure, and there can be no nesting.
   * Read all variables for each record, put in ByteBuffer.
   *
   * @param s the record structure
   * @param section the record range to read
   * @return an ArrayStructure, with all the data read in.
   * @throws IOException on error
   */
  private ucar.ma2.Array readRecordData(ucar.nc2.Structure s, Section section) throws java.io.IOException {
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
}
