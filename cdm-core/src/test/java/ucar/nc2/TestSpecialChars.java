/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.NcmlWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import ucar.nc2.write.NetcdfFormatWriter;

import static com.google.common.truth.Truth.assertThat;

/** Test writing and reading some special characters. */
public class TestSpecialChars {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  String trouble = "here is a &, <, >, \', \", \n, \r, \t, to handle";

  @Test
  public void testWriteAndRead() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addAttribute(new Attribute("omy", trouble));
    writerb.addDimension("t", 1);
    writerb
        .addDimension(Dimension.builder().setName("t_strlen").setLength(trouble.length()).setIsShared(false).build());

    // define Variables
    writerb.addVariable("t", ArrayType.CHAR, "t_strlen").addAttribute(new Attribute("yow", trouble));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable("t");
      assertThat(v).isNotNull();
      writer.writeStringData(v, Index.ofRank(1), trouble);
    }

    String ncmlFilePath = tempFolder.newFile().getAbsolutePath();
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      String val = ncfile.getRootGroup().findAttributeString("omy", null);
      assert val != null;
      assert val.equals(trouble);

      Variable v = ncfile.findVariable("t");

      val = v.findAttributeString("yow", null);
      assert val != null;
      assert val.equals(trouble);

      try (OutputStream out = new FileOutputStream(ncmlFilePath)) {
        NcmlWriter ncmlWriter = new NcmlWriter();
        Element netcdfElem = ncmlWriter.makeNetcdfElement(ncfile, null);
        ncmlWriter.writeToStream(netcdfElem, out);
      }

      try (NetcdfFile ncfile2 = NetcdfDatasets.openFile(ncmlFilePath, null)) {
        String val2 = ncfile2.getRootGroup().findAttributeString("omy", null);
        assert val2 != null;
        assert val2.equals(trouble);

        Variable v2 = ncfile2.findVariable("t");

        val2 = v2.findAttributeString("yow", null);
        assert val2 != null;
        assert val2.equals(trouble);
      }
    }

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(ncmlFilePath, null)) {
      System.out.println("ncml= " + ncfile.getLocation());

      String val = ncfile.getRootGroup().findAttributeString("omy", null);
      assert val != null;
      assert val.equals(trouble);

      Variable v = ncfile.findVariable("t");

      val = v.findAttributeString("yow", null);
      assert val != null;
      assert val.equals(trouble);
    }
  }
}
