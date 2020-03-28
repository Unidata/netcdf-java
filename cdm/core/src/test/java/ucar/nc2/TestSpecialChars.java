/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.write.NcmlWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/**
 * @author caron
 * @since Aug 7, 2007
 */
public class TestSpecialChars {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  String trouble = "here is a &, <, >, \', \", \n, \r, \t, to handle";

  @Test
  public void testWriteAndRead() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFileWriter ncfilew = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {
      ncfilew.addGlobalAttribute("omy", trouble);

      ncfilew.addDimension("t", 1);

      // define Variables
      Variable tvar = ncfilew.addStringVariable(null, "t", new ArrayList<>(), trouble.length());
      ncfilew.addVariableAttribute("t", "yow", trouble);

      ncfilew.create();

      Array data = Array.factory(DataType.STRING, new int[0]);
      data.setObject(data.getIndex(), trouble);
      ncfilew.writeStringData(tvar, data);
    }

    String ncmlFilePath = tempFolder.newFile().getAbsolutePath();
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      String val = ncfile.getRootGroup().findAttValueIgnoreCase("omy", null);
      assert val != null;
      assert val.equals(trouble);

      Variable v = ncfile.findVariable("t");
      v.setCachedData(v.read(), true);

      val = v.findAttValueIgnoreCase("yow", null);
      assert val != null;
      assert val.equals(trouble);

      try (OutputStream out = new FileOutputStream(ncmlFilePath)) {
        NcmlWriter ncmlWriter = new NcmlWriter();
        Element netcdfElem = ncmlWriter.makeNetcdfElement(ncfile, null);
        ncmlWriter.writeToStream(netcdfElem, out);
      }

      try (NetcdfFile ncfile2 = NetcdfDataset.openFile(ncmlFilePath, null)) {
        String val2 = ncfile2.getRootGroup().findAttValueIgnoreCase("omy", null);
        assert val2 != null;
        assert val2.equals(trouble);

        Variable v2 = ncfile2.findVariable("t");
        v2.setCachedData(v2.read(), true);

        val2 = v2.findAttValueIgnoreCase("yow", null);
        assert val2 != null;
        assert val2.equals(trouble);
      }
    }

    try (NetcdfFile ncfile = NetcdfDataset.openFile(ncmlFilePath, null)) {
      System.out.println("ncml= " + ncfile.getLocation());

      String val = ncfile.getRootGroup().findAttValueIgnoreCase("omy", null);
      assert val != null;
      assert val.equals(trouble);

      Variable v = ncfile.findVariable("t");
      v.setCachedData(v.read(), true);

      val = v.findAttValueIgnoreCase("yow", null);
      assert val != null;
      assert val.equals(trouble);
    }
  }
}
