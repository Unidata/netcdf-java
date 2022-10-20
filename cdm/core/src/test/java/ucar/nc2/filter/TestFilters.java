/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import com.google.common.primitives.Ints;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class TestFilters {

  private static final String DATA_DIR = "src/test/data/filter";

  private static byte[] decoded_data;

  @BeforeClass
  public static void setUp() throws IOException {
    decoded_data = readAsByteArray("raw_data");
  }

  private static byte[] readAsByteArray(String filename) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(DATA_DIR + "/" + filename, "r");
    raf.seek(0);
    byte[] bytes = new byte[(int) raf.length()];
    raf.read(bytes);
    return bytes;
  }

  @Test
  public void testDeflate() throws IOException {
    // test deflate level 1
    Map<String, Object> props = new HashMap<>();
    props.put("id", "zlib");
    props.put("level", 1);
    Filter filter = new Deflate(props);
    testEncodeDecode(filter, "deflate_level1");

    // test deflate level 9
    props.replace("level", 9);
    filter = new Deflate(props);
    testEncodeDecode(filter, "deflate_level9");
  }

  @Test
  public void testShuffle() throws IOException {
    Map<String, Object> props = new HashMap<>();
    props.put("id", "shuffle");
    Filter filter = new Shuffle(props);
    testEncodeDecode(filter, "shuffle");
  }

  @Test
  public void testChecksum32() throws IOException {
    // test Adler32
    Filter filter = new Checksum32(Checksum32.CType.ADLER);
    testEncodeDecode(filter, "adler32");

    // test CRC32
    filter = new Checksum32(Checksum32.CType.CRC);
    testEncodeDecode(filter, "crc32");
  }

  @Test
  public void testScaleOffset() throws IOException {
    // NOTE: The scaleoffset test file holds the decoded data, which should encode
    // as the data held in `decoded_data`
    Map<String, Object> props = new HashMap<>();
    props.put("id", "fixedscaleoffset");
    props.put("offset", 1000);
    props.put("scale", 10);
    props.put("dtype", "<f4");
    props.put("astype", "<u1");
    Filter filter = new ScaleOffset(props);

    // convert decoded data to uint8
    byte[] encoded = new byte[decoded_data.length / Integer.BYTES];
    for (int i = 0; i < encoded.length; i++) {
      encoded[i] = decoded_data[i * Integer.BYTES];
    }

    // test encode
    byte[] input = readAsByteArray("scaleoffset");
    byte[] out = filter.encode(input);
    assertThat(out).isEqualTo(encoded);

    // test decode
    byte[] decoded = filter.decode(encoded);
    assertThat(decoded).isEqualTo(input);
  }

  private void testEncodeDecode(Filter filter, String filename) throws IOException {
    // test encode
    byte[] out = filter.encode(decoded_data);
    byte[] encoded = readAsByteArray(filename);
    assertThat(out).isEqualTo(encoded);

    // test decode
    byte[] decoded = filter.decode(encoded);
    assertThat(decoded).isEqualTo(decoded_data);
  }

  @Test
  public void testFletcher() {
    // test case from Wikipeda Fletcher test vectors
    String testString = "abcdefgh";
    int knownChecksum = -1785599007;
    byte[] checksumBytes = Ints.toByteArray(knownChecksum);
    Checksum32 filter = new Checksum32(Checksum32.CType.FLETCHER);
    byte[] expected = testString.getBytes(StandardCharsets.UTF_8);
    byte[] in = new byte[expected.length + 4];
    System.arraycopy(expected, 0, in, 0, expected.length);
    System.arraycopy(checksumBytes, 0, in, expected.length, 4);
    byte[] out = filter.decode(in);
    assertThat(out).isEqualTo(expected);
  }

}
