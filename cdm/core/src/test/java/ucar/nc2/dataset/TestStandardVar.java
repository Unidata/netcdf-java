/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.Ncdump;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Formatter;

public class TestStandardVar {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String filename = TestDir.cdmLocalTestDataDir + "standardVar.nc";

  @Test
  public void testWriteStandardVar() throws Exception {
    NetcdfFormatWriter.Builder writerb =
        NetcdfFormatWriter.builder().setNewFile(true).setLocation(filename).setFill(false);

    // define dimensions
    Dimension latDim = writerb.addDimension("lat", 2);
    Dimension lonDim = writerb.addDimension("lon", 3);
    ArrayList<Dimension> dims = new ArrayList<>();
    dims.add(latDim);
    dims.add(lonDim);

    // case 1
    writerb.addVariable("t1", DataType.DOUBLE, dims).addAttribute(new Attribute(CDM.SCALE_FACTOR, 2.0))
        .addAttribute(new Attribute(CDM.ADD_OFFSET, 77.0));

    // case 2
    writerb.addVariable("t2", DataType.BYTE, dims).addAttribute(new Attribute(CDM.SCALE_FACTOR, (short) 2))
        .addAttribute(new Attribute(CDM.ADD_OFFSET, (short) 77));

    // case 3
    writerb.addVariable("t3", DataType.BYTE, dims).addAttribute(new Attribute(CDM.FILL_VALUE, (byte) 255));

    // case 4
    writerb.addVariable("t4", DataType.SHORT, dims).addAttribute(new Attribute(CDM.MISSING_VALUE, (short) -9999));

    // case 5
    writerb.addVariable("t5", DataType.SHORT, dims).addAttribute(new Attribute(CDM.MISSING_VALUE, (short) -9999))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, (short) 2))
        .addAttribute(new Attribute(CDM.ADD_OFFSET, (short) 77));

    // case m1
    writerb.addVariable("m1", DataType.DOUBLE, dims).addAttribute(new Attribute(CDM.MISSING_VALUE, -999.99));

    // create and write to the file
    try (NetcdfFormatWriter writer = writerb.build()) {
      // write t1
      ArrayDouble A = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
      int i, j;
      Index ima = A.getIndex();
      // write
      for (i = 0; i < latDim.getLength(); i++)
        for (j = 0; j < lonDim.getLength(); j++)
          A.setDouble(ima.set(i, j), (i * 10.0 + j));
      int[] origin = new int[2];
      writer.write("t1", origin, A);

      // write t2
      ArrayByte Ab = new ArrayByte.D2(latDim.getLength(), lonDim.getLength(), false);
      ima = Ab.getIndex();
      for (i = 0; i < latDim.getLength(); i++)
        for (j = 0; j < lonDim.getLength(); j++)
          Ab.setByte(ima.set(i, j), (byte) (i * 10 + j));
      writer.write("t2", origin, Ab);

      // write t3
      writer.write("t3", origin, Ab);

      // write t4
      Array As = new ArrayShort.D2(latDim.getLength(), lonDim.getLength(), false);
      ima = As.getIndex();
      for (i = 0; i < latDim.getLength(); i++)
        for (j = 0; j < lonDim.getLength(); j++)
          As.setShort(ima.set(i, j), (short) (i * 10 + j));
      writer.write("t4", origin, As);

      As.setShort(ima.set(0, 0), (short) -9999);
      writer.write("t5", origin, As);

      // write m1
      ArrayDouble.D2 Ad = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
      for (i = 0; i < latDim.getLength(); i++)
        for (j = 0; j < lonDim.getLength(); j++)
          Ad.setDouble(ima.set(i, j), (i * 10.0 + j));
      Ad.set(1, 1, -999.99);
      writer.write("m1", new int[2], Ad);
    }
  }

  private NetcdfFile ncfileRead;
  private NetcdfDataset dsRead;

  @Test
  public void testReadStandardVar() throws Exception {
    ncfileRead = NetcdfFiles.open(filename);
    dsRead = NetcdfDatasets.openDataset(filename);

    readDouble();
    readByte2Short();
    readByte();
    readShortMissing();
    readShort2FloatMissing();

    readDoubleMissing();

    ncfileRead.close();
    dsRead.close();
  }

  private void readDouble() throws Exception {
    Variable t1 = ncfileRead.findVariable("t1");
    assert (null != t1);
    assert (t1.getDataType() == DataType.DOUBLE);

    Attribute att = t1.findAttribute(CDM.SCALE_FACTOR);
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (2.0 == att.getNumericValue().doubleValue());
    assert (DataType.DOUBLE == att.getDataType());

    // read
    Array A = t1.read();
    int i, j;
    Index ima = A.getIndex();
    int[] shape = A.getShape();

    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        assert (A.getDouble(ima.set(i, j)) == (double) (i * 10.0 + j));
      }
    }

    t1 = dsRead.findVariable("t1");
    assert (null != t1);
    assert t1 instanceof VariableEnhanced;
    assert (t1.getDataType() == DataType.DOUBLE);

    A = t1.read();
    ima = A.getIndex();
    shape = A.getShape();

    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        assert (A.getDouble(ima.set(i, j)) == (2.0 * (i * 10.0 + j) + 77.0));
      }
    }
  }

  private void readByte2Short() throws Exception {
    Variable t2 = ncfileRead.findVariable("t2");
    assert (null != t2);
    assert (t2.getDataType() == DataType.BYTE);

    Attribute att = t2.findAttribute(CDM.SCALE_FACTOR);
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (2 == att.getNumericValue().doubleValue());
    assert (DataType.SHORT == att.getDataType());

    t2 = dsRead.findVariable("t2");
    assert (null != t2);
    assert t2 instanceof VariableEnhanced;
    VariableDS vs = (VariableDS) t2;
    assert (vs.getDataType() == DataType.SHORT) : vs.getDataType();
    assert (vs.hasMissing());

    Array A = vs.read();
    assert (A.getElementType() == short.class) : A.getElementType();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i, j;
    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        assert (A.getShort(ima.set(i, j)) == (2 * (i * 10 + j) + 77));
      }
    }
  }

  private void readByte() throws Exception {
    Variable v = ncfileRead.findVariable("t3");
    assert (v != null);
    assert (v.getDataType() == DataType.BYTE);

    v = dsRead.findVariable("t3");
    assert (v != null);
    assert v instanceof VariableEnhanced;
    assert v instanceof VariableDS;
    VariableDS vs = (VariableDS) v;
    assert (vs.getDataType() == DataType.BYTE);

    Attribute att = vs.findAttribute("_FillValue");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    logger.debug("_FillValue = {}", att.getNumericValue().byteValue());
    assert (((byte) 255) == att.getNumericValue().byteValue());
    assert (DataType.BYTE == att.getDataType());

    assert (vs.hasMissing());
    assert (vs.hasFillValue());
    assert (vs.isMissing((double) ((byte) 255)));
    assert (vs.isFillValue((double) ((byte) 255)));

    Array A = vs.read();
    assert (A.getElementType() == byte.class) : A.getElementType();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i, j;
    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        assert (A.getFloat(ima.set(i, j)) == (i * 10 + j));
      }
    }
  }

  private void readShortMissing() throws Exception {
    Variable v = ncfileRead.findVariable("t4");
    assert (v != null);
    assert (v.getDataType() == DataType.SHORT);

    // default use of missing_value
    v = dsRead.findVariable("t4");
    assert (v != null);
    assert v instanceof VariableEnhanced;
    assert v instanceof VariableDS;
    VariableDS vs = (VariableDS) v;
    assert (vs.getDataType() == DataType.SHORT);

    Attribute att = vs.findAttribute(CDM.MISSING_VALUE);
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    logger.debug("missing_value = {}", att.getNumericValue().shortValue());
    assert (((short) -9999) == att.getNumericValue().shortValue());
    assert (DataType.SHORT == att.getDataType());

    assert (vs.hasMissing());
    assert (vs.hasMissingValue());
    assert (vs.isMissing((double) ((short) -9999)));
    assert (vs.isMissingValue((double) ((short) -9999)));

    Array A = vs.read();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i, j;
    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        assert (A.getFloat(ima.set(i, j)) == (i * 10 + j));
      }
    }
  }


  private void readShort2FloatMissing() throws Exception {
    Variable v = ncfileRead.findVariable("t5");
    assert (v != null);
    assert (v.getDataType() == DataType.SHORT);

    // standard convert with missing data
    v = dsRead.findVariable("t5");
    assert (v != null);
    assert v instanceof VariableEnhanced;
    assert v instanceof VariableDS;
    VariableDS vs = (VariableDS) v;
    assert (vs.getDataType() == DataType.SHORT);

    assert (vs.hasMissing());
    assert (vs.hasMissingValue());
    double mv = 2 * (-9999) + 77;
    assert (vs.isMissing(mv));
    assert (vs.isMissingValue(mv));

    Array A = vs.read();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i, j;

    assert (vs.isMissing(A.getShort(ima.set(0, 0))));

    for (i = 0; i < shape[0]; i++) {
      for (j = 1; j < shape[1]; j++) {
        float val = A.getShort(ima.set(i, j));
        float want = 2 * (i * 10 + j) + 77;
        if (val != want)
          logger.debug("{} {} {} {}", i, j, val, want);
        assert (val == want);
      }
    }
  }

  private void readDoubleMissing() throws Exception {
    VariableDS v = (VariableDS) dsRead.findVariable("m1");
    assert (v != null);
    assert (v.getDataType() == DataType.DOUBLE);

    Array A = v.read();
    Index ima = A.getIndex();

    double val = A.getFloat(ima.set(1, 1));
    assert Double.isNaN(val);
    assert v.isMissing(val);
  }

  @Test
  public void testEnhanceDefer() throws IOException {
    DatasetUrl durl = DatasetUrl.create(null, filename);

    try (NetcdfDataset ncd =
        NetcdfDatasets.openDataset(durl, EnumSet.of(NetcdfDataset.Enhance.ApplyScaleOffset), -1, null, null)) {
      try (NetcdfDataset ncdefer = NetcdfDatasets.openDataset(durl, null, -1, null, null)) {

        VariableDS enhancedVar = (VariableDS) ncd.findVariable("t1");
        assert (enhancedVar != null);
        VariableDS deferVar = (VariableDS) ncdefer.findVariable("t1");
        assert (deferVar != null);

        Array enhancedData = enhancedVar.read();
        Array deferredData = deferVar.read();

        logger.debug("Enhanced = {}", Ncdump.printArray(enhancedData));
        logger.debug("Deferred = {}", Ncdump.printArray(deferredData));

        Formatter compareOutputFormatter = new Formatter();
        CompareNetcdf2 nc = new CompareNetcdf2(compareOutputFormatter, false, false, true);

        logger.debug("Comparison result = {}", compareOutputFormatter.toString());
        assert !nc.compareData(enhancedVar.getShortName(), enhancedData, deferredData, false);

        Array processedData = enhancedVar.applyScaleOffset(deferredData);

        logger.debug("Processed = {}", Ncdump.printArray(deferredData));
        assert nc.compareData(enhancedVar.getShortName(), enhancedData, processedData, false);
      }
    }
  }
}
