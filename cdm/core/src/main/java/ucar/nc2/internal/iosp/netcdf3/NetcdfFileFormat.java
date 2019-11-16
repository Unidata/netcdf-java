package ucar.nc2.internal.iosp.netcdf3;

import java.io.IOException;

/*
 * From netcdf.h:
 * #define NC_FORMAT_CLASSIC (1)
 * After adding CDF5 support, the NC_FORMAT_64BIT flag is somewhat confusing. So, it is renamed.
 * Note that the name in the contributed code NC_FORMAT_64BIT was renamed to NC_FORMAT_CDF2
 * #define NC_FORMAT_64BIT_OFFSET (2)
 * #define NC_FORMAT_64BIT (NC_FORMAT_64BIT_OFFSET)
 * #define NC_FORMAT_NETCDF4 (3)
 * #define NC_FORMAT_NETCDF4_CLASSIC (4)
 * #define NC_FORMAT_64BIT_DATA (5)
 *
 * This is probably bogus, but leave it until we port hdf5.
 */

/** Enumeration of the kinds of NetCDF file formats. */
public enum NetcdfFileFormat {
  INVALID(0, "Invalid"), CLASSIC(1, "netcdf-3"), OFFSET_64BIT(2, "netcdf-3 64bit-offset"), NETCDF4(3, "netcdf-4"), // This
                                                                                                                   // is
                                                                                                                   // really
                                                                                                                   // just
                                                                                                                   // HDF-5,
                                                                                                                   // dont
                                                                                                                   // know
                                                                                                                   // yet
                                                                                                                   // if
                                                                                                                   // its
                                                                                                                   // written
                                                                                                                   // by
                                                                                                                   // netcdf4.
  NETCDF4_CLASSIC(4, "netcdf-4 classic"), // psuedo format I think
  DATA_64BIT(5, "netcdf-5"), HDF4(0x7005, "hdf-4"); // why is this here. and wheres hdf-5 ?

  private static final int MAGIC_NUMBER_LEN = 8;
  private static final long MAXHEADERPOS = 50000; // header's gotta be within this range

  private static final byte[] H5HEAD = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', (byte) 0x1a, '\n'};
  private static final byte[] H4HEAD = {(byte) 0x0e, (byte) 0x03, (byte) 0x13, (byte) 0x01};
  private static final byte[] CDF1HEAD = {(byte) 'C', (byte) 'D', (byte) 'F', (byte) 0x01};
  private static final byte[] CDF2HEAD = {(byte) 'C', (byte) 'D', (byte) 'F', (byte) 0x02};
  private static final byte[] CDF5HEAD = {(byte) 'C', (byte) 'D', (byte) 'F', (byte) 0x05};

  private final int version;
  private final String formatName;

  NetcdfFileFormat(int version, String formatName) {
    this.version = version;
    this.formatName = formatName;
  }

  public int version() {
    return version;
  }

  public String formatName() {
    return formatName;
  }

  /**
   * Figure out what kind of netcdf-related file we have.
   * Constraint: leave raf read pointer to point just after the magic number.
   *
   * @param raf to test type
   * @return NetcdfFileFormat that matches constants in netcdf-c/include/netcdf.h, or INVALID if not a netcdf file.
   */
  public static NetcdfFileFormat findNetcdfFormatType(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    byte[] magic = new byte[MAGIC_NUMBER_LEN];

    // If this is not an HDF5 file, then the magic number is at
    // position 0; If it is an HDF5 file, then we need to search
    // forward for it.

    // Look for the relevant leading tag
    raf.seek(0);
    if (raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN) // why read 8, why not 4 ??
      return NetcdfFileFormat.INVALID;

    int hdrlen = CDF1HEAD.length; // all CDF headers are assumed to be same length

    NetcdfFileFormat format = null;
    if (memequal(CDF1HEAD, magic, CDF1HEAD.length))
      format = CLASSIC;
    else if (memequal(CDF2HEAD, magic, CDF2HEAD.length))
      format = OFFSET_64BIT;
    else if (memequal(CDF5HEAD, magic, CDF5HEAD.length))
      format = DATA_64BIT;
    else if (memequal(H4HEAD, magic, H4HEAD.length))
      format = HDF4;

    if (format != null) {
      raf.seek(hdrlen);
      return format;
    }

    // For HDF5, we need to search forward
    long filePos = 0;
    long size = raf.length();
    while ((filePos < size - 8) && (filePos < MAXHEADERPOS)) {
      boolean match;
      raf.seek(filePos);
      if (raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN)
        return INVALID; // unknown
      // Test for HDF5
      if (memequal(H5HEAD, magic, H5HEAD.length)) {
        format = NETCDF4;
        break;
      }
      filePos = (filePos == 0) ? 512 : 2 * filePos;
    }
    if (format != null)
      raf.seek(filePos + H5HEAD.length);
    return format == null ? INVALID : format;
  }

  private static boolean memequal(byte[] b1, byte[] b2, int len) {
    if (b1 == b2)
      return true;
    if (b1 == null || b2 == null)
      return false;
    if (b1.length < len || b2.length < len)
      return false;
    for (int i = 0; i < len; i++) {
      if (b1[i] != b2[i])
        return false;
    }
    return true;
  }
}
