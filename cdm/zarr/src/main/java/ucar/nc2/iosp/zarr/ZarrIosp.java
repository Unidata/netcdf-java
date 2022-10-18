/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
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

/**
 * IOSP for reading/writing Zarr/NCZarr formats
 */
public class ZarrIosp extends AbstractIOServiceProvider {

  static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup ( ).lookupClass ( ));

  private static final String fileTypeId = "Zarr";
  private static final String fileTypeDescription = "Zarr v2 formatted dataset";

  private ZarrHeader header;

  @Override
  public boolean isValidFile(RandomAccessFile raf) {
    return raf.isDirectory();
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

    // Watch for floating point fill values encoded as Strings
    final Object fillValueObj = vinfo.getFillValue();

    Object fillValue = fillValueObj;

    if (fillValueObj instanceof String) {
      final String fillValueStr = (String)fillValueObj;

      logger.debug("Fill value is String with value '{}'", fillValueStr);

      if ("".equals(fillValueStr)) {
        fillValue = null;
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
              logger.debug("String value '{}' not handled for float fill value", fillValueStr);
            }
            break;

          default:
            logger.debug("String value '{}' not handled for {} fill value", fillValueStr, dataType);
            break;
        }
      }
    }

    // create layout object
    Layout layout = new ZarrLayoutBB(v2, section, this.raf);
    Object data = IospHelper.readDataFill((LayoutBB) layout, dataType, fillValue);

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
}

