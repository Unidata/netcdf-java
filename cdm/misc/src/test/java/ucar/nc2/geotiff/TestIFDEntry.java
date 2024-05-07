/*
 * Copyright (c) 1998-2024 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.geotiff;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * IFDEntry read/write
 *
 * @author Ben Root
 * @since 5/6/2024
 */
@RunWith(Parameterized.class)
public class TestIFDEntry {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final IFDEntry ifd;
  private final int testValue;

  @Parameterized.Parameters(name = "{0}_{1}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    // Unsigned
    result.add(new Object[] {new IFDEntry(null, FieldType.BYTE, 1), 0});
    result.add(new Object[] {new IFDEntry(null, FieldType.BYTE, 1), Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.BYTE, 1), 255});
    result.add(new Object[] {new IFDEntry(null, FieldType.ASCII, 1), 0});
    result.add(new Object[] {new IFDEntry(null, FieldType.ASCII, 1), Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.ASCII, 1), 255});
    result.add(new Object[] {new IFDEntry(null, FieldType.SHORT, 1), 0});
    result.add(new Object[] {new IFDEntry(null, FieldType.SHORT, 1), Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SHORT, 1), Short.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SHORT, 1), 65535});
    result.add(new Object[] {new IFDEntry(null, FieldType.LONG, 1), 0});
    result.add(new Object[] {new IFDEntry(null, FieldType.LONG, 1), Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.LONG, 1), Short.MAX_VALUE});
    // NOTE: because of the API design, unsigned longs can't be properly read or written
    // for all possible values because Java's integer is signed.
    result.add(new Object[] {new IFDEntry(null, FieldType.LONG, 1), Integer.MAX_VALUE});

    // Signed
    result.add(new Object[] {new IFDEntry(null, FieldType.SBYTE, 1), 0});
    result.add(new Object[] {new IFDEntry(null, FieldType.SBYTE, 1), Byte.MIN_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SBYTE, 1), Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SSHORT, 1), 0});
    result.add(new Object[] {new IFDEntry(null, FieldType.SSHORT, 1), Byte.MIN_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SSHORT, 1), -Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SSHORT, 1), Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SSHORT, 1), Short.MIN_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SSHORT, 1), Short.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SLONG, 1), 0});
    result.add(new Object[] {new IFDEntry(null, FieldType.SLONG, 1), -Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SLONG, 1), Byte.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SLONG, 1), -Short.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SLONG, 1), Short.MAX_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SLONG, 1), Integer.MIN_VALUE});
    result.add(new Object[] {new IFDEntry(null, FieldType.SLONG, 1), Integer.MAX_VALUE});

    return result;
  }

  public TestIFDEntry(IFDEntry ifd, int testValue) {
    this.ifd = ifd;
    this.testValue = testValue;
  }

  @Test
  public void testRoundtrip() {
    // 16 bytes should be more than enough
    ByteBuffer buffer = ByteBuffer.allocate(16);
    ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    buffer.order(byteOrder);

    int writeSize = GeoTiff.writeIntValue(buffer, ifd, testValue);
    Assert.assertEquals(ifd.type.size, writeSize);
    buffer.position(0);

    int readValue = GeoTiff.readIntValue(buffer, ifd);

    Assert.assertEquals(testValue, readValue);
  }
}
