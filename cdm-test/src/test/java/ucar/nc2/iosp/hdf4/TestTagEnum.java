package ucar.nc2.iosp.hdf4;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import org.junit.Test;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.TestDir;

public class TestTagEnum {

  @Test
  public void testStuff() throws IOException {
    try (FileInputStream ios = new FileInputStream(TestDir.cdmLocalTestDataDir + "hdf4/HDF4.2r1/hdf/src/htags.h")) {
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, CDM.utf8Charset));
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        if (line.startsWith("#define")) {
          StringTokenizer stoker = new StringTokenizer(line, " ()");
          stoker.nextToken(); // skip define
          String name = stoker.nextToken();
          if (!stoker.hasMoreTokens()) continue;
          //System.out.println(line);

          if (name.startsWith("DFTAG_"))
            name = name.substring(6);

          String code = stoker.nextToken();
          if (code.startsWith("u"))
            code = stoker.nextToken();

          int pos = line.indexOf("/*");
          String desc = "";
          if (pos > 0) {
            int pos2 = line.indexOf("*/");
            desc = (pos2 > 0) ? line.substring(pos + 3, pos2) : line.substring(pos + 3);
            desc = desc.trim();
          }

          System.out.println("  public final static Tags " + name + " = new Tags(\"" + name + "\", \"" + desc + "\", (short) " + code + ");");
        }
      }
    }
  }


}
