package ucar.nc2.iosp.zarr;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestZarrFilters {

  private static final String ZARR_COMPRESSED_FILENAME = "zarr_compressed_data.zarr/";

  private static final String FILE_PATH = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + ZARR_COMPRESSED_FILENAME;

  private static NetcdfFile ncfile;

  @BeforeClass
  public static void setUpTests() throws IOException {
    ncfile = NetcdfFiles.open(FILE_PATH);
  }

  @AfterClass
  public static void cleanUpTests() throws IOException {
    ncfile.close();
  }

  @Test
  public void testReadNullCompressedData() throws IOException {
    Array data = ncfile.findVariable("null_compressor").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();
  }

  @Test
  public void testReadCompressedData() throws IOException {
    Array data;
    // test zlib
    data = ncfile.findVariable("compressed/deflate1").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();
    data = ncfile.findVariable("compressed/deflate9").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();

    // test 32 bit checksums
    data = ncfile.findVariable("compressed/crc32").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();
    data = ncfile.findVariable("compressed/adler32").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();

    // test shuffle
    data = ncfile.findVariable("compressed/shuffle").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();

    // test scaleoffset
    data = ncfile.findVariable("compressed/scaleOffset").read();
    assertThat(hasExpectedValues(data, 10, 1000)).isTrue();
  }

  @Test
  public void testReadFilteredData() throws IOException {
    Array data;
    // test adler32 as filter
    data = ncfile.findVariable("filtered/adler32").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();

    // test adler32 and shuffle as filters
    data = ncfile.findVariable("filtered/adler_shuffle").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();
  }

  @Test
  public void testReadCompressedAndFilteredData() throws IOException {
    Array data;
    // test shuffle as filter, zlib as compressor
    data = ncfile.findVariable("comp_filt/shuffle_deflate").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();

    // test adler32 and shuffle as filters, zlib as compressor
    data = ncfile.findVariable("comp_filt/Adler_shuffle_deflate").read();
    assertThat(hasExpectedValues(data, 1, 0)).isTrue();
  }

  private static boolean hasExpectedValues(Array data, int scale, float offset) {
    // For scaled data, 1/scale is the last significant digit
    double e = 1/(double)scale;
    double[] dataAsDoubles = (double[])data.get1DJavaArray(DataType.DOUBLE);
    for (int i = 0; i < dataAsDoubles.length; i++) {
      double scaledVal = (dataAsDoubles[i]-offset) * scale;
      if (Math.abs(scaledVal - (double)i%200) > e) {
        return false;
      }
    }
    return true;
  }
}
