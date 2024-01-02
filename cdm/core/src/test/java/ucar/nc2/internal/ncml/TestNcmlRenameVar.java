/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ncml.TestNcmlRead;

public class TestNcmlRenameVar {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final double TOLERANCE = 1e-8;

  static NetcdfFile ncfile = null;
  static String filename = "file:./" + TestNcmlRead.topDir + "renameVar.xml";

  @BeforeClass
  public static void setUp() throws IOException {
    ncfile = NcmlReader.readNcml(filename, null, null).build();
  }

  @AfterClass
  public static void tearDown() throws IOException {
    ncfile.close();
  }

  @Test
  public void testStructure() {
    Attribute att = ncfile.findGlobalAttribute("title");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();
    assertThat(att.isString()).isTrue();
    assertThat(att.getDataType()).isEqualTo(DataType.STRING);
    assertThat(att.getStringValue()).isEqualTo("Example Data");
    assertThat(att.getNumericValue()).isNull();
    assertThat(att.getNumericValue(3)).isNull();

    att = ncfile.findGlobalAttribute("testFloat");
    assertThat(att).isNotNull();
    assertThat(att.isArray()).isTrue();
    assertThat(att.isString()).isFalse();
    assertThat(att.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(att.getStringValue()).isNull();
    assertThat(att.getNumericValue()).isEqualTo(1.0f);
    assertThat(att.getNumericValue(3)).isEqualTo(4.0f);

    Dimension latDim = ncfile.findDimension("lat");
    assertThat(latDim).isNotNull();
    assertThat(latDim.getShortName()).isEqualTo("lat");
    assertThat(latDim.getLength()).isEqualTo(3);
    assertThat(latDim.isUnlimited()).isFalse();

    Dimension timeDim = ncfile.findDimension("time");
    assertThat(timeDim).isNotNull();
    assertThat(timeDim.getShortName()).isEqualTo("time");
    assertThat(timeDim.getLength()).isEqualTo(4);
    assertThat(timeDim.isUnlimited()).isTrue();
  }

  @Test
  public void testReadCoordvar() throws IOException {
    Variable lat = ncfile.findVariable("lat");
    assertThat((Object) lat).isNotNull();
    assertThat(lat.getShortName()).isEqualTo("lat");
    assertThat(lat.getRank()).isEqualTo(1);
    assertThat(lat.getSize()).isEqualTo(3);
    assertThat(lat.getShape()[0]).isEqualTo(3);
    assertThat(lat.getDataType()).isEqualTo(DataType.FLOAT);

    assertThat(lat.isUnlimited()).isFalse();
    assertThat(lat.getDimension(0)).isEqualTo(ncfile.findDimension("lat"));

    Attribute att = lat.findAttribute("units");
    assertThat(att).isNotNull();
    assertThat(att.isArray()).isFalse();
    assertThat(att.isString()).isTrue();
    assertThat(att.getDataType()).isEqualTo(DataType.STRING);
    assertThat(att.getStringValue()).isEqualTo("degrees_north");
    assertThat(att.getNumericValue()).isNull();
    assertThat(att.getNumericValue(3)).isNull();

    Array data = lat.read();
    assertThat(data.getRank()).isEqualTo(1);
    assertThat(data.getSize()).isEqualTo(3);
    assertThat(data.getShape()[0]).isEqualTo(3);
    assertThat(data.getElementType()).isEqualTo(float.class);

    IndexIterator dataI = data.getIndexIterator();
    assertThat(dataI.getDoubleNext()).isWithin(TOLERANCE).of(41.0);
    assertThat(dataI.getDoubleNext()).isWithin(TOLERANCE).of(40.0);
    assertThat(dataI.getDoubleNext()).isWithin(TOLERANCE).of(39.0);
  }

  @Test
  public void testReadData() throws IOException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    assertThat((Object) v).isNotNull();
    assertThat(v.getShortName()).isEqualTo("ReletiveHumidity");
    assertThat(v.getRank()).isEqualTo(3);
    assertThat(v.getSize()).isEqualTo(48);
    assertThat(v.getShape()[0]).isEqualTo(4);
    assertThat(v.getShape()[1]).isEqualTo(3);
    assertThat(v.getShape()[2]).isEqualTo(4);
    assertThat(v.getDataType()).isEqualTo(DataType.INT);

    assertThat(v.isCoordinateVariable()).isFalse();
    assertThat(v.isUnlimited()).isTrue();

    assertThat(v.getDimension(0)).isEqualTo(ncfile.findDimension("time"));
    assertThat(v.getDimension(1)).isEqualTo(ncfile.findDimension("lat"));
    assertThat(v.getDimension(2)).isEqualTo(ncfile.findDimension("lon"));

    Attribute att = v.findAttribute("units");
    assertThat(att).isNotNull();
    assertThat(att.isArray()).isFalse();
    assertThat(att.isString()).isTrue();
    assertThat(att.getDataType()).isEqualTo(DataType.STRING);
    assertThat(att.getStringValue()).isEqualTo("percent");
    assertThat(att.getNumericValue()).isNull();
    assertThat(att.getNumericValue(3)).isNull();

    Array data = v.read();
    assertThat(data.getRank()).isEqualTo(3);
    assertThat(data.getSize()).isEqualTo(48);
    assertThat(data.getShape()[0]).isEqualTo(4);
    assertThat(data.getShape()[1]).isEqualTo(3);
    assertThat(data.getShape()[2]).isEqualTo(4);
    assertThat(data.getElementType()).isEqualTo(int.class);

    IndexIterator dataI = data.getIndexIterator();
    assertThat(dataI.getIntNext()).isEqualTo(1);
    assertThat(dataI.getIntNext()).isEqualTo(2);
    assertThat(dataI.getIntNext()).isEqualTo(3);
    assertThat(dataI.getIntNext()).isEqualTo(4);
    assertThat(dataI.getIntNext()).isEqualTo(5);
  }

  @Test
  public void testReadSlice() throws InvalidRangeException, IOException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};

    Array data = v.read(origin, shape);
    assertThat(data.getRank()).isEqualTo(3);
    assertThat(data.getSize()).isEqualTo(6);
    assertThat(data.getShape()[0]).isEqualTo(2);
    assertThat(data.getShape()[1]).isEqualTo(3);
    assertThat(data.getShape()[2]).isEqualTo(1);
    assertThat(data.getElementType()).isEqualTo(int.class);

    IndexIterator dataI = data.getIndexIterator();
    assertThat(dataI.getIntNext()).isEqualTo(1);
    assertThat(dataI.getIntNext()).isEqualTo(5);
    assertThat(dataI.getIntNext()).isEqualTo(9);
    assertThat(dataI.getIntNext()).isEqualTo(21);
    assertThat(dataI.getIntNext()).isEqualTo(25);
    assertThat(dataI.getIntNext()).isEqualTo(29);
  }

  @Test
  public void testReadSlice2() throws InvalidRangeException, IOException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};

    Array data = v.read(origin, shape).reduce();
    assertThat(data.getRank()).isEqualTo(2);
    assertThat(data.getSize()).isEqualTo(6);
    assertThat(data.getShape()[0]).isEqualTo(2);
    assertThat(data.getShape()[1]).isEqualTo(3);
    assertThat(data.getElementType()).isEqualTo(int.class);

    IndexIterator dataI = data.getIndexIterator();
    assertThat(dataI.getIntNext()).isEqualTo(1);
    assertThat(dataI.getIntNext()).isEqualTo(2);
    assertThat(dataI.getIntNext()).isEqualTo(3);
    assertThat(dataI.getIntNext()).isEqualTo(21);
    assertThat(dataI.getIntNext()).isEqualTo(22);
    assertThat(dataI.getIntNext()).isEqualTo(23);
  }

}
