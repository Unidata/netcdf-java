/*
 * Copyright (c) 2021-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.*;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.zarr.RandomAccessDirectory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * IOSP for reading/writing Zarr/NCZarr formats
 */
public class ZarrIosp extends AbstractIOServiceProvider {

  static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String fileTypeId = "Zarr";
  private static final String fileTypeDescription = "Zarr v2 formatted dataset";

  private ZarrHeader header;

  @Override
  public boolean isValidFile(RandomAccessFile raf) {
    return raf.isDirectory();
  }

  /**
   * Set the SortGroup to GROUP_1 so this IOSP will be checked first, since `isValidFile()` is a quick check
   *
   * @return SortGroup.GROUP_1
   */
  @Override
  public SortGroup getSortGroup() {
    return SortGroup.GROUP_1;
  }

  @Override
  public String getFileTypeId() {
    return fileTypeId;
  }

  @Override
  public String getFileTypeDescription() {
    return fileTypeDescription;
  }

  @Override
  public boolean isBuilder() {
    return true;
  }

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, null, cancelTask);
    header = new ZarrHeader((RandomAccessDirectory) raf, rootGroup);
    header.read(); // build CDM from Zarr
  }

  @Override
  public void buildFinish(NetcdfFile ncfile) {} // NO-OP

  @Override
  public Array readData(Variable v2, Section section) {
    // find variable in RAF
    ZarrHeader.VInfo vinfo = (ZarrHeader.VInfo) v2.getSPobject();
    DataType dataType = v2.getDataType();
    logger.debug("DataType is '{}'", dataType);

    Object fillValue = getFillValue(vinfo, dataType);

    // create layout object
    LayoutBB layout = new ZarrLayoutBB(v2, section, this.raf);
    final Object data;
    if (dataType == DataType.STRING) {
      // fixed-length string types (S/U) need custom decoding (not handled by the generic IospHelper string reader).
      data = readStringData(layout, vinfo, fillValue);
    } else {
      data = IospHelper.readDataFill(layout, dataType, fillValue);
    }

    Array array = Array.factory(dataType, section.getShape(), data);
    if (vinfo.getOrder() == ZArray.Order.F) {
      int n = v2.getDimensions().size();
      int[] dims = new int[n];
      for (int i = 0; i < n; i++) {
        dims[i] = n - i - 1;
      }
      array = array.permute(dims);
    }

    return array;
  }

  /**
   * Read fixed-length string data ('S' or 'U' dtypes) from the layout.
   *
   * <p>
   * See https://github.com/Unidata/netcdf-java/issues/1534
   */
  private static String[] readStringData(LayoutBB layout, ZarrHeader.VInfo vinfo, Object fillValue) {
    final int nelems = (int) layout.getTotalNelems();
    final int recSize = layout.getElemSize();
    final String[] pa = new String[nelems];
    if (fillValue instanceof String) {
      java.util.Arrays.fill(pa, (String) fillValue);
    }

    final Charset charset;
    if (vinfo.isUnicodeString()) {
      charset =
          vinfo.getByteOrder() == ByteOrder.BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
    } else {
      charset = StandardCharsets.UTF_8;
    }

    while (layout.hasNext()) {
      LayoutBB.Chunk chunk = layout.next();
      ByteBuffer bb = chunk.getByteBuffer();
      // if chunk is empty, use fill value
      if (!bb.hasRemaining()) {
        continue;
      }
      bb.position(chunk.getSrcElem() * recSize);
      int pos = (int) chunk.getDestElem();
      final byte[] raw = new byte[recSize];
      for (int i = 0; i < chunk.getNelems(); i++) {
        bb.get(raw);
        pa[pos++] = decodeFixedLengthString(raw, charset);
      }
    }
    return pa;
  }

  private static String decodeFixedLengthString(byte[] raw, Charset charset) {
    String s = new String(raw, charset);
    // NumPy fixed-length strings are null-padded, so strip trailing NUL characters
    int end = s.length();
    while (end > 0 && s.charAt(end - 1) == '\0') {
      end--;
    }
    return s.substring(0, end);
  }

  private Object getFillValue(ZarrHeader.VInfo vinfo, DataType dataType) {

    // Watch for floating point fill values encoded as Strings
    final Object fillValueObj = vinfo.getFillValue();

    Object fillValue = fillValueObj;

    if (fillValueObj instanceof String) {
      final String fillValueStr = (String) fillValueObj;
      logger.debug("Fill value is String with value '{}'", fillValueStr);
      if (dataType.isString()) {
        return fillValueStr;
      }

      if (fillValueStr.isEmpty()) {
        return null;
      } else {
        switch (dataType) {
          case FLOAT:
            if ("NaN".equals(fillValueStr)) {
              fillValue = Float.NaN;
            } else if ("Infinity".equals(fillValueStr)) {
              fillValue = Float.POSITIVE_INFINITY;
            } else if ("-Infinity".equals(fillValueStr)) {
              fillValue = Float.NEGATIVE_INFINITY;
            } else {
              logger.debug("String value '{}' not handled for float fill value", fillValueStr);
            }
            break;

          case DOUBLE:
            if ("NaN".equals(fillValueStr)) {
              fillValue = Double.NaN;
            } else if ("Infinity".equals(fillValueStr)) {
              fillValue = Double.POSITIVE_INFINITY;
            } else if ("-Infinity".equals(fillValueStr)) {
              fillValue = Double.NEGATIVE_INFINITY;
            } else {
              logger.debug("String value '{}' not handled for double fill value", fillValueStr);
            }
            break;

          default:
            logger.debug("String value '{}' not handled for {} fill value", fillValueStr, dataType);
            break;
        }
      }
    }
    return fillValue;
  }

  @Override
  public long getLastModified() {
    if (raf == null) {
      try {
        reacquire();
      } catch (IOException e) {
        return 0;
      }
    }
    return raf.getLastModified();
  }
}
