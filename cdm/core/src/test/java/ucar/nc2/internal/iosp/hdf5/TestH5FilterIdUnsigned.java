/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filter;
import ucar.nc2.filter.FilterProvider;
import ucar.nc2.filter.Filters;
import ucar.nc2.filter.UnknownFilterException;
import ucar.unidata.util.test.TestDir;

/**
 * HDF5 filter ids are unsigned 16-bit values. Ids from 32768 to 65535 must not be read as negative numbers.
 * <p>
 * The test files hold an int variable {@code v(24)} = 0..23, chunked by 8, with the optional filter 40000 in its
 * filter pipeline. The filter was not available when the files were written, so HDF5 stored the chunks unfiltered
 * and set the filter mask. {@code filter_id_40000_v1.nc} has a version 1 filter pipeline message and
 * {@code filter_id_40000_v2.nc} a version 2 message, in which the name length field is present only for ids of 256
 * and above. The filter parameters (cd_values) are {7}.
 * <p>
 * The files were made from a netCDF-4 file written by ncgen with HDF5 1.10.5 h5repack:
 * {@code h5repack -f v:UD=40000,1,1,7 raw.nc filter_id_40000_v1.nc} and
 * {@code h5repack --low=1 --high=1 -f v:UD=40000,1,1,7 raw.nc filter_id_40000_v2.nc}.
 */
public class TestH5FilterIdUnsigned {
  private static final String DIR = TestDir.cdmLocalTestDataDir + "hdf5/";
  private static final int HIGH_ID = 40000;

  @Test
  public void testGetFilterByHighId() throws UnknownFilterException {
    Map<String, Object> props = new HashMap<>();
    props.put(Filters.Keys.ID, (short) HIGH_ID); // as read from the file: -25536
    Filter filter = Filters.getFilter(props);
    assertThat(filter).isInstanceOf(HighIdFilter.class);
    assertThat(filter.getId()).isEqualTo(HIGH_ID);
  }

  @Test
  public void testReadFilterPipelineVersion1() throws IOException {
    checkFile("filter_id_40000_v1.nc");
  }

  @Test
  public void testReadFilterPipelineVersion2() throws IOException {
    checkFile("filter_id_40000_v2.nc");
  }

  private static void checkFile(String name) throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(DIR + name)) {
      Variable v = ncfile.findVariable("v");
      assertThat((Object) v).isNotNull();
      Array data = v.read();
      int[] expected = new int[24];
      for (int i = 0; i < expected.length; i++) {
        expected[i] = i;
      }
      assertThat((int[]) data.copyTo1DJavaArray()).isEqualTo(expected);
      // the filter parameters follow the name length field, so they are only right if that field was read
      assertThat((int[]) HighIdFilter.lastProperties.get(Filters.Keys.DATA)).isEqualTo(new int[] {7});
    }
  }

  /** Pass-through filter registered for id 40000 (see META-INF/services/ucar.nc2.filter.FilterProvider). */
  public static class HighIdFilter extends Filter {
    static volatile Map<String, Object> lastProperties;

    @Override
    public String getName() {
      return "test-high-id";
    }

    @Override
    public int getId() {
      return HIGH_ID;
    }

    @Override
    public byte[] encode(byte[] dataIn) {
      return dataIn;
    }

    @Override
    public byte[] decode(byte[] dataIn) {
      return dataIn;
    }

    public static class Provider implements FilterProvider {
      @Override
      public String getName() {
        return "test-high-id";
      }

      @Override
      public int getId() {
        return HIGH_ID;
      }

      @Override
      public Filter create(Map<String, Object> properties) {
        HighIdFilter.lastProperties = new HashMap<>(properties);
        return new HighIdFilter();
      }
    }
  }
}
