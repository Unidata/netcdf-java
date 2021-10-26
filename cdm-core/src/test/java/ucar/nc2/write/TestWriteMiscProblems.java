/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Charsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Test miscellaneous {@link NetcdfFormatWriter} problems. */
public class TestWriteMiscProblems {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWriteBigString() throws IOException {
    String filename = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    int len = 120000;
    char[] carray1 = new char[len];
    for (int i = 0; i < len; i++)
      carray1[i] = '1';
    writerb.addAttribute(Attribute.fromArray("tooLongChar", Arrays.factory(ArrayType.CHAR, new int[] {len}, carray1)));

    char[] carray2 = new char[len];
    for (int i = 0; i < len; i++)
      carray2[i] = '2';
    String val = new String(carray2);
    writerb.addAttribute(new Attribute("tooLongString", val));

    try (NetcdfFormatWriter ncfile = writerb.build()) {
    }
  }

  @Test
  public void testCharMultidim() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    Dimension Time_dim = writerb.addUnlimitedDimension("Time");
    Dimension DateStrLen_dim = writerb.addDimension("DateStrLen", 19);

    /* define variables */
    List<Dimension> Times_dimlist = new ArrayList<Dimension>();
    Times_dimlist.add(Time_dim);
    Times_dimlist.add(DateStrLen_dim);
    writerb.addVariable("Times", ArrayType.CHAR, Times_dimlist);

    try (NetcdfFormatWriter writer = writerb.build()) {
      /* assign variable data */
      String contents = "2005-04-11_12:00:002005-04-11_13:00:00";
      byte[] cdata = contents.getBytes(Charsets.UTF_8);
      assertThat(cdata.length).isEqualTo(2 * 19);
      Array<Byte> data = Arrays.factory(ArrayType.CHAR, new int[] {2, 19}, cdata);
      Variable v = writer.findVariable("Times");
      writer.write(v, data.getIndex(), data);
    }

    try (NetcdfFile nc = NetcdfFiles.open(filename)) {
      Variable v = nc.findVariable("Times");
      assertThat(v).isNotNull();
      Array<Byte> cdata = (Array<Byte>) v.readArray();
      Array<String> sdata = Arrays.makeStringsFromChar(cdata);

      assertThat(sdata.get(0)).isEqualTo("2005-04-11_12:00:00");
      assertThat(sdata.get(1)).isEqualTo("2005-04-11_13:00:00");
    }
  }

  @Test(expected = RuntimeException.class)
  public void testFileHandleReleaseAfterHeaderWriteFailure() throws IOException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    Attribute invalidNc3Attr = Attribute.builder().setName("will_fail").setNumericValue(1, true).build();
    writerb.addAttribute(invalidNc3Attr);

    try (NetcdfFormatWriter writer = writerb.build()) {
      // this call *should* trigger a runtime exception (IllegalArgumentException)
    } catch (RuntimeException iae) {
      // if we throw a runtime error during writerb.build(), we ended up in a state
      // where the underlying RAF was not closed because the code would encounter the same issue and
      // throw another runtime error. If a user was trying to handle the runtime error, this could end
      // up causing a file handle leak.
      // this test makes sure we are able to close the file.
      File fileToDelete = new File(filename);
      assertThat(fileToDelete.exists()).isTrue();
      // if the file handle has not been released, the file delete will fail
      // assertThat(fileToDelete.delete()).isTrue();
      // still want the IllegalArgumentException to happen, we'd just like to make sure the file handle is released
      throw iae;
    }
  }
}
