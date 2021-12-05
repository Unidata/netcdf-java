/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.NcdumpArray;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test basics of enhanced {@link NetcdfDataset} */
public class TestStandardVar {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final String filename = TestDir.cdmLocalTestDataDir + "standardVar.nc";

  @Test
  public void testWriteStandardVar() throws Exception {
    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.builder().setLocation(filename).setFill(false);

    // define dimensions
    Dimension latDim = writerb.addDimension("lat", 2);
    Dimension lonDim = writerb.addDimension("lon", 3);
    ArrayList<Dimension> dims = new ArrayList<>();
    dims.add(latDim);
    dims.add(lonDim);

    // case 1
    writerb.addVariable("t1", ArrayType.DOUBLE, dims).addAttribute(new Attribute(CDM.SCALE_FACTOR, 2.0))
        .addAttribute(new Attribute(CDM.ADD_OFFSET, 77.0));

    // case 2
    writerb.addVariable("t2", ArrayType.BYTE, dims).addAttribute(new Attribute(CDM.SCALE_FACTOR, (short) 2))
        .addAttribute(new Attribute(CDM.ADD_OFFSET, (short) 77));

    // case 3
    writerb.addVariable("t3", ArrayType.BYTE, dims).addAttribute(new Attribute(CDM.FILL_VALUE, (byte) 255));

    // case 4
    writerb.addVariable("t4", ArrayType.SHORT, dims).addAttribute(new Attribute(CDM.MISSING_VALUE, (short) -9999));

    // case 5
    writerb.addVariable("t5", ArrayType.SHORT, dims).addAttribute(new Attribute(CDM.MISSING_VALUE, (short) -9999))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, (short) 2))
        .addAttribute(new Attribute(CDM.ADD_OFFSET, (short) 77));

    // case m1
    writerb.addVariable("m1", ArrayType.DOUBLE, dims).addAttribute(new Attribute(CDM.MISSING_VALUE, -999.99));

    // create and write to the file
    try (NetcdfFormatWriter writer = writerb.build()) {
      int ny = latDim.getLength();
      int nx = lonDim.getLength();

      // write t1
      double[] parray = new double[nx * ny];
      for (int i = 0; i < ny; i++) {
        for (int j = 0; j < nx; j++) {
          parray[i * nx + j] = i * 10.0 + j;
        }
      }
      Array A = Arrays.factory(ArrayType.DOUBLE, new int[] {ny, nx}, parray);
      writer.write("t1", Index.ofRank(2), A);

      // write t2
      byte[] barray = new byte[nx * ny];
      for (int i = 0; i < ny; i++) {
        for (int j = 0; j < nx; j++) {
          barray[i * nx + j] = (byte) (i * 10 + j);
        }
      }
      A = Arrays.factory(ArrayType.BYTE, new int[] {ny, nx}, barray);
      writer.write("t2", Index.ofRank(2), A);

      // write t3
      writer.write("t3", Index.ofRank(2), A);

      // write t4
      short[] sarray = new short[nx * ny];
      for (int i = 0; i < ny; i++) {
        for (int j = 0; j < nx; j++) {
          sarray[i * nx + j] = (byte) (i * 10 + j);
        }
      }
      A = Arrays.factory(ArrayType.SHORT, new int[] {ny, nx}, sarray);
      writer.write("t4", Index.ofRank(2), A);

      // write t5
      sarray[0] = (short) -9999;
      A = Arrays.factory(ArrayType.SHORT, new int[] {ny, nx}, sarray);
      writer.write("t5", Index.ofRank(2), A);

      // write m1
      parray[nx + 1] = -999.99;
      A = Arrays.factory(ArrayType.DOUBLE, new int[] {ny, nx}, parray);
      writer.write("m1", Index.ofRank(2), A);
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
    assertThat(t1).isNotNull();
    assertThat(t1.getArrayType()).isEqualTo(ArrayType.DOUBLE);

    Attribute att = t1.findAttribute(CDM.SCALE_FACTOR);
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();
    assertThat(1).isEqualTo(att.getLength());
    assertThat(2.0).isEqualTo(att.getNumericValue().doubleValue());
    assertThat(ArrayType.DOUBLE).isEqualTo(att.getArrayType());

    // read
    Array<Double> A = (Array<Double>) t1.readArray();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        assertThat(A.get(ima.set(i, j))).isEqualTo(i * 10.0 + j);
      }
    }

    t1 = dsRead.findVariable("t1");
    assertThat(t1).isNotNull();
    assertThat(t1).isInstanceOf(VariableEnhanced.class);
    assertThat(t1.getArrayType()).isEqualTo(ArrayType.DOUBLE);

    A = (Array<Double>) t1.readArray();
    ima = A.getIndex();
    shape = A.getShape();

    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        assertThat(A.get(ima.set(i, j))).isEqualTo(2.0 * (i * 10.0 + j) + 77.0);
      }
    }
  }

  private void readByte2Short() throws Exception {
    Variable t2 = ncfileRead.findVariable("t2");
    assertThat(t2).isNotNull();
    assertThat(t2.getArrayType()).isEqualTo(ArrayType.BYTE);

    Attribute att = t2.findAttribute(CDM.SCALE_FACTOR);
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();
    assertThat(1).isEqualTo(att.getLength());
    assertThat(att.getNumericValue().doubleValue()).isEqualTo(2.0);
    assertThat(ArrayType.SHORT).isEqualTo(att.getArrayType());

    t2 = dsRead.findVariable("t2");
    assertThat(t2).isNotNull();
    assertThat(t2).isInstanceOf(VariableDS.class);
    VariableDS vs = (VariableDS) t2;
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.SHORT);
    assertThat(vs.hasMissing());

    Array<Short> A = (Array<Short>) vs.readArray();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        assertThat(A.get(ima.set(i, j))).isEqualTo(2 * (i * 10 + j) + 77);
      }
    }
  }

  private void readByte() throws Exception {
    Variable v = ncfileRead.findVariable("t3");
    assertThat(v).isNotNull();
    assertThat(v.getArrayType()).isEqualTo(ArrayType.BYTE);

    v = dsRead.findVariable("t3");
    assertThat(v).isNotNull();
    assertThat(v).isInstanceOf(VariableEnhanced.class);
    assertThat(v).isInstanceOf(VariableDS.class);
    VariableDS vs = (VariableDS) v;
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.BYTE);

    Attribute att = vs.findAttribute("_FillValue");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(1).isEqualTo(att.getLength());
    logger.debug("_FillValue = {}", att.getNumericValue().byteValue());
    assertThat(((byte) 255)).isEqualTo(att.getNumericValue().byteValue());
    assertThat(ArrayType.BYTE).isEqualTo(att.getArrayType());

    assertThat(vs.hasMissing());
    assertThat(vs.scaleMissingUnsignedProxy().hasFillValue());
    assertThat(vs.isMissing((byte) 255));
    assertThat(vs.scaleMissingUnsignedProxy().isFillValue((double) ((byte) 255)));

    Array<Byte> A = (Array<Byte>) vs.readArray();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        assertThat(A.get(ima.set(i, j))).isEqualTo(i * 10 + j);
      }
    }
  }

  private void readShortMissing() throws Exception {
    Variable v = ncfileRead.findVariable("t4");
    assertThat(v).isNotNull();
    assertThat(v.getArrayType()).isEqualTo(ArrayType.SHORT);

    // default use of missing_value
    v = dsRead.findVariable("t4");
    assertThat(v).isNotNull();
    assertThat(v).isInstanceOf(VariableEnhanced.class);
    assertThat(v).isInstanceOf(VariableDS.class);
    VariableDS vs = (VariableDS) v;
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.SHORT);

    Attribute att = vs.findAttribute(CDM.MISSING_VALUE);
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(1).isEqualTo(att.getLength());
    logger.debug("missing_value = {}", att.getNumericValue().shortValue());
    assertThat(((short) -9999)).isEqualTo(att.getNumericValue().shortValue());
    assertThat(ArrayType.SHORT).isEqualTo(att.getArrayType());

    assertThat(vs.hasMissing());
    assertThat(vs.scaleMissingUnsignedProxy().hasMissingValue());
    assertThat(vs.isMissing((double) ((short) -9999)));
    assertThat(vs.scaleMissingUnsignedProxy().isMissingValue((double) ((short) -9999)));

    Array<Short> A = (Array<Short>) vs.readArray();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        assertThat(A.get(ima.set(i, j))).isEqualTo(i * 10 + j);
      }
    }
  }


  private void readShort2FloatMissing() throws Exception {
    Variable v = ncfileRead.findVariable("t5");
    assertThat(v).isNotNull();
    assertThat(v.getArrayType()).isEqualTo(ArrayType.SHORT);

    // standard convert with missing data
    v = dsRead.findVariable("t5");
    assertThat(v).isNotNull();
    assertThat(v).isInstanceOf(VariableEnhanced.class);
    assertThat(v).isInstanceOf(VariableDS.class);
    VariableDS vs = (VariableDS) v;
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.SHORT);

    assertThat(vs.hasMissing());
    assertThat(vs.scaleMissingUnsignedProxy().hasMissingValue());
    double mv = 2 * (-9999) + 77;
    assertThat(vs.isMissing(mv));
    assertThat(vs.scaleMissingUnsignedProxy().isMissingValue(mv));

    Array<Short> A = (Array<Short>) vs.readArray();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 1; j < shape[1]; j++) {
        short val = A.get(ima.set(i, j));
        assertThat(val).isEqualTo(2 * (i * 10 + j) + 77);
      }
    }
    assertThat(vs.isMissing(A.get(ima.set(0, 0))));
  }

  private void readDoubleMissing() throws Exception {
    VariableDS v = (VariableDS) dsRead.findVariable("m1");
    assertThat(v).isNotNull();
    assertThat(v.getArrayType()).isEqualTo(ArrayType.DOUBLE);

    Array<Double> A = (Array<Double>) v.readArray();
    Index ima = A.getIndex();

    double val = A.get(ima.set(1, 1));
    assertThat(val).isNaN();
    assertThat(v.isMissing(val)).isTrue();
  }

  @Test
  public void testEnhanceDefer() throws IOException {
    DatasetUrl durl = DatasetUrl.create(null, filename);

    try (NetcdfDataset ncd =
        NetcdfDatasets.openDataset(durl, EnumSet.of(NetcdfDataset.Enhance.ApplyScaleOffset), -1, null, null)) {
      try (NetcdfDataset ncdefer = NetcdfDatasets.openDataset(durl, null, -1, null, null)) {

        VariableDS enhancedVar = (VariableDS) ncd.findVariable("t1");
        assertThat(enhancedVar).isNotNull();
        VariableDS deferVar = (VariableDS) ncdefer.findVariable("t1");
        assertThat(deferVar).isNotNull();

        ucar.array.Array<?> enhancedData = enhancedVar.readArray();
        ucar.array.Array<?> deferredData = deferVar.readArray();

        logger.debug("Enhanced = {}", NcdumpArray.printArray(enhancedData));
        logger.debug("Deferred = {}", NcdumpArray.printArray(deferredData));

        Formatter compareOutputFormatter = new Formatter();
        CompareNetcdf2 nc = new CompareNetcdf2(compareOutputFormatter, false, false, true);

        logger.debug("Comparison result = {}", compareOutputFormatter);
        assertThat(nc.compareData(enhancedVar.getShortName(), enhancedData, deferredData)).isFalse();

        ucar.array.Array<?> processedData = enhancedVar.scaleMissingUnsignedProxy().applyScaleOffset(deferredData);

        logger.debug("Processed = {}", NcdumpArray.printArray(deferredData));
        assertThat(nc.compareData(enhancedVar.getShortName(), enhancedData, processedData)).isTrue();
      }
    }
  }
}
