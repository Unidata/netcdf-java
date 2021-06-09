/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import java.io.IOException;
import javax.annotation.Nullable;

/*
 * From https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_8h_source.html on 3/26/2020
 * #define NC_FORMAT_CLASSIC (1)
 * 
 * After adding CDF5 support, the NC_FORMAT_64BIT flag is somewhat confusing. So, it is renamed.
 * Note that the name in the contributed code NC_FORMAT_64BIT was renamed to NC_FORMAT_CDF2
 * 
 * #define NC_FORMAT_64BIT_OFFSET (2)
 * #define NC_FORMAT_64BIT (NC_FORMAT_64BIT_OFFSET)
 * #define NC_FORMAT_NETCDF4 (3)
 * #define NC_FORMAT_NETCDF4_CLASSIC (4)
 * #define NC_FORMAT_64BIT_DATA (5)
 * 
 * /// Alias
 * #define NC_FORMAT_CDF5 NC_FORMAT_64BIT_DATA
 * 
 * #define NC_FORMATX_NC3 (1)
 * #define NC_FORMATX_NC_HDF5 (2)
 * #define NC_FORMATX_NC4 NC_FORMATX_NC_HDF5
 * #define NC_FORMATX_NC_HDF4 (3)
 * #define NC_FORMATX_PNETCDF (4)
 * #define NC_FORMATX_DAP2 (5)
 * #define NC_FORMATX_DAP4 (6)
 * #define NC_FORMATX_UDF0 (8)
 * #define NC_FORMATX_UDF1 (9)
 * #define NC_FORMATX_ZARR (10)
 * #define NC_FORMATX_UNDEFINED (0)
 * 
 * To avoid breaking compatibility (such as in the python library), we need to retain the NC_FORMAT_xxx format as well.
 * This may come out eventually, as the NC_FORMATX is more clear that it's an extended format specifier.
 * 
 * #define NC_FORMAT_NC3 NC_FORMATX_NC3
 * #define NC_FORMAT_NC_HDF5 NC_FORMATX_NC_HDF5
 * #define NC_FORMAT_NC4 NC_FORMATX_NC4
 * #define NC_FORMAT_NC_HDF4 NC_FORMATX_NC_HDF4
 * #define NC_FORMAT_PNETCDF NC_FORMATX_PNETCDF
 * #define NC_FORMAT_DAP2 NC_FORMATX_DAP2
 * #define NC_FORMAT_DAP4 NC_FORMATX_DAP4
 * #define NC_FORMAT_UNDEFINED NC_FORMATX_UNDEFINED
 */

/*
 * From https://www.unidata.ucar.edu/software/netcdf/docs/faq.html#How-many-netCDF-formats-are-there-and-what-are-the-
 * differences-among-them
 * 
 * Q: How many netCDF formats are there, and what are the differences among them?
 * 
 * A: There are four netCDF format variants:
 * 
 * - the classic format
 * - the 64-bit offset format
 * - the 64-bit data format
 * - the netCDF-4 format
 * - the netCDF-4 classic model format
 * 
 * (In addition, there are two textual representations for netCDF data, though these are not usually thought of as
 * formats: CDL and NcML.)
 * 
 * The classic format was the only format for netCDF data created between 1989 and 2004 by the reference software from
 * Unidata.
 * It is still the default format for new netCDF data files, and the form in which most netCDF data is stored.
 * This format is also referred as CDF-1 format.
 * 
 * In 2004, the 64-bit offset format variant was added. Nearly identical to netCDF classic format, it allows users to
 * create and access
 * far larger datasets than were possible with the original format. (A 64-bit platform is not required to write or read
 * 64-bit offset
 * netCDF files.) This format is also referred as CDF-2 format.
 * 
 * In 2008, the netCDF-4 format was added to support per-variable compression, multiple unlimited dimensions, more
 * complex data types,
 * and better performance, by layering an enhanced netCDF access interface on top of the HDF5 format.
 * 
 * At the same time, a fourth format variant, netCDF-4 classic model format, was added for users who needed the
 * performance benefits
 * of the new format (such as compression) without the complexity of a new programming interface or enhanced data model.
 * 
 * In 2016, the 64-bit data format variant was added. To support large variables with more than 4-billion array
 * elements, it replaces
 * most of the 32-bit integers used in the format specification with 64-bit integers. It also adds support for several
 * new data types
 * including unsigned byte, unsigned short, unsigned int, signed 64-bit int and unsigned 64-bit int. A 64-bit platform
 * is required to
 * write or read 64-bit data netCDF files. This format is also referred as CDF-5 format.
 * 
 * With each additional format variant, the C-based reference software from Unidata has continued to support access to
 * data stored in
 * previous formats transparently, and to also support programs written using previous programming interfaces.
 * 
 * Although strictly speaking, there is no single "netCDF-3 format", that phrase is sometimes used instead of the more
 * cumbersome but
 * correct "netCDF classic CDF-1, 64-bit offset CDF-2, or 64-bit data CDF-5 format" to describe files created by the
 * netCDF-3
 * (or netCDF-1 or netCDF-2) libraries. Similarly "netCDF-4 format" is sometimes used informally to mean "either the
 * general netCDF-4 format
 * or the restricted netCDF-4 classic model format". We will use these shorter phrases in FAQs below when no confusion
 * is likely.
 * 
 * A more extensive description of the netCDF formats and a formal specification of the classic and 64-bit formats is
 * available as a NASA ESDS community standard
 * (https://earthdata.nasa.gov/sites/default/files/esdswg/spg/rfc/esds-rfc-011/ESDS-RFC-011v2.00.pdf)
 * 
 * The 64-bit data CDF-5 format specification is available in
 * http://cucis.ece.northwestern.edu/projects/PnetCDF/CDF-5.html.
 */

/** Enumeration of the kinds of NetCDF file formats. NETCDF3_64BIT_DATA is not currently supported in this library. */
public enum NetcdfFileFormat {
  INVALID(0, "Invalid"), //
  NETCDF3(1, "NetCDF-3"), //
  NETCDF3_64BIT_OFFSET(2, "netcdf-3 64bit-offset"), //
  NETCDF4(3, "NetCDF-4"), // This is really just HDF-5, dont know yet if its written by netcdf4.
  NETCDF4_CLASSIC(4, "netcdf-4 classic"), // psuedo format I think
  NETCDF3_64BIT_DATA(5, "netcdf-5"); // from PnetCDF project

  // NCSTREAM(42, "ncstream"); // No assigned version, not part of C library.

  private static final int MAGIC_NUMBER_LEN = 8;
  private static final long MAXHEADERPOS = 50000; // header's gotta be within this range

  private static final byte[] H5HEAD = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', (byte) 0x1a, '\n'};
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

  public boolean isNetdf3format() {
    return this == NETCDF3 || this == NETCDF3_64BIT_OFFSET || this == NETCDF3_64BIT_DATA;
  }

  public boolean isNetdf4format() {
    return this == NETCDF4 || this == NETCDF4_CLASSIC;
  }

  public boolean isExtendedModel() {
    return this == NETCDF4; // || this == NCSTREAM;
  }

  public boolean isLargeFile() {
    return this == NETCDF3_64BIT_OFFSET;
  }

  public boolean isClassicModel() {
    return this == NETCDF3 || this == NETCDF3_64BIT_OFFSET || this == NETCDF4_CLASSIC || this == NETCDF3_64BIT_DATA;
  }

  // this converts old NetcdfFileWriter.Version string.
  @Nullable
  public static NetcdfFileFormat convertVersionToFormat(String netcdfFileWriterVersion) {
    switch (netcdfFileWriterVersion.toLowerCase()) {
      case "netcdf3":
        return NetcdfFileFormat.NETCDF3;
      case "netcdf4":
        return NetcdfFileFormat.NETCDF4;
      case "netcdf4_classic":
        return NetcdfFileFormat.NETCDF4_CLASSIC;
      // case "netcdf3c": LOOK dont have an equivalent
      // return NetcdfFileFormat.NETCDF3_64BIT_OFFSET;
      case "netcdf3c64":
        return NetcdfFileFormat.NETCDF3_64BIT_OFFSET;
      default:
        return null;
    }
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

    // If this is not an HDF5 file, then the magic number is at position 0;
    // If it is an HDF5 file, then we need to search forward for it.

    // Look for the relevant leading tag
    raf.seek(0);
    if (raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN) // why read 8, why not 4 ??
      return NetcdfFileFormat.INVALID;

    int hdrlen = CDF1HEAD.length; // all CDF headers are assumed to be same length

    NetcdfFileFormat format = null;
    if (memequal(CDF1HEAD, magic, CDF1HEAD.length))
      format = NETCDF3;
    else if (memequal(CDF2HEAD, magic, CDF2HEAD.length))
      format = NETCDF3_64BIT_OFFSET;
    else if (memequal(CDF5HEAD, magic, CDF5HEAD.length))
      format = NETCDF3_64BIT_DATA;

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
