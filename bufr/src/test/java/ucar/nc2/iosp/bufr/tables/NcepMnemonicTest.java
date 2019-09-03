package ucar.nc2.iosp.bufr.tables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Formatter;
import org.junit.Test;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil2;

public class NcepMnemonicTest {
  /**
   * Read NCEP mnemonic BUFR tables.
   */
  private static void readSubCategories(String fileIn, PrintStream out, String token) throws IOException {
    System.out.printf("%s%n", fileIn);
    try (FileInputStream in = new FileInputStream(fileIn)) {
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      while (true) {
        String line = dataIS.readLine();
        if (line == null)
          break;
        int posb = line.indexOf("DISCONTINUED");
        if (posb > 0)
          continue;
        posb = line.indexOf("NO LONGER");
        if (posb > 0)
          continue;
        posb = line.indexOf("WAS REPLACED");
        if (posb > 0)
          continue;

        int pos = line.indexOf(token);
        if (pos < 0)
          continue;
        System.out.printf("%s%n", line);

        boolean is31 = token.equals("031-");
        String subline = is31 ? line.substring(pos) : line.substring(pos + token.length());
        // if (is31) System.out.printf(" '%s'%n", subline);

        int pos2 = subline.indexOf(' ');
        String catS = subline.substring(0, pos2);
        String desc = subline.substring(pos2 + 1);
        // System.out.printf(" cat='%s'%n", catS);
        // System.out.printf(" desc='%s'%n", desc);
        int cat = Integer.parseInt(catS.substring(0, 3));
        int subcat = Integer.parseInt(catS.substring(4, 7));
        desc = StringUtil2.remove(desc, '|').trim();
        // System.out.printf(" cat=%d subcat=%d%n", cat,subcat);
        // System.out.printf(" desc='%s'%n", desc);
        // System.out.printf("%d, %d, %s%n", cat, subcat, desc);
        out.printf("%d; %d; %s%n", cat, subcat, desc);
      }
    }
  }

  @Test
  public void testReadBufrTable() throws IOException {
    Path tempDirectory = Files.createTempDirectory("NcepMnemonicTest");
    File fileOut = Files.createTempFile(tempDirectory, "temp", null).toFile();

    try (PrintStream pout = new PrintStream(fileOut, CDM.UTF8)) {
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.000.txt", pout, "MSG TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.001.txt", pout, "MESSAGE TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.002.txt", pout, "MSG TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.003.txt", pout, "MTYP ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.004.txt", pout, "MSG TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.005.txt", pout, "MSG TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.006.txt", pout, "M TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.007.txt", pout, "MTYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.008.txt", pout, "MSG TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.012.txt", pout, "M TYPE ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.021.txt", pout, "MTYP ");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.031.txt", pout, "031-");
      readSubCategories("../bufr/src/main/sources/ncep/bufrtab.255.txt", pout, "MTYP ");
    }
    System.out.printf("=======================================%n");
    System.out.printf("%s%n", IO.readFile(fileOut.getAbsolutePath()));
  }

  @Test
  public void testNcepBufrTable() throws IOException {
    String locationOld = "resource:/resources/bufrTables/local/ncep/ncep.bufrtab.ETACLS1";
    String location = "../bufr/src/main/resources/resources/bufrTables/local/ncep/ncep.bufrtab.ETACLS1";
    try (InputStream ios = BufrTables.openStream(location)) {
      BufrTables.Tables tables = new BufrTables.Tables();
      NcepMnemonic.read(ios, tables);

      Formatter out = new Formatter(System.out);
      tables.b.show(out);
      tables.d.show(out);
    }

  }



}
