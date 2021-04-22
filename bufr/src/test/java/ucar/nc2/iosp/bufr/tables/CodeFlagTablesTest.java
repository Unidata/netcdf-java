package ucar.nc2.iosp.bufr.tables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import ucar.unidata.util.StringUtil2;

/** Test {@link CodeFlagTables} */
public class CodeFlagTablesTest {

  @Ignore("too verbose")
  @Test
  public void testCompareTables() throws IOException {
    HashMap<Short, CodeFlagTables> tableMap1 = new HashMap<>(300);
    CodeFlagTables.init(tableMap1);

    HashMap<Short, CodeFlagTables> tableMap2 = new HashMap<>(300);
    this.init2(tableMap2);

    System.out.printf("Compare 1 with 2%n");
    for (Map.Entry<Short, CodeFlagTables> ent : tableMap1.entrySet()) {
      CodeFlagTables t = ent.getValue();
      CodeFlagTables t2 = tableMap2.get(ent.getKey());
      if (t2 == null) {
        System.out.printf(" NOT FOUND in 2: %s (%d)%n", t.fxy(), t.getId());
      } else {
        for (int no : t.getMap().keySet()) {
          String name1 = t.getMap().get(no);
          String name2 = t2.getMap().get(no);
          if (name2 == null)
            System.out.printf(" %s val %d name '%s' missing in 2%n", t.fxy(), no, name1);
          else if (!name1.equals(name2))
            System.out.printf(" *** %s names different%n  %s%n  %s%n", t.fxy(), name1, name2);
        }
      }
    }

    System.out.printf("Compare 2 with 1%n");
    for (Map.Entry<Short, CodeFlagTables> ent : tableMap2.entrySet()) {
      CodeFlagTables t = ent.getValue();
      CodeFlagTables t1 = tableMap1.get(ent.getKey());
      if (t1 == null) {
        System.out.printf(" NOT FOUND in 1: %s (%d)%n", t.fxy(), t.getId());
      } else {
        for (int no : t.getMap().keySet()) {
          String name = t.getMap().get(no);
          String name1 = t1.getMap().get(no);
          if (name1 == null)
            System.out.printf(" %s val %d name '%s' missing%n", t.fxy(), no, name);
          else if (!name.equals(name1))
            System.out.printf(" %s names different%n  %s%n  %s%n", t.fxy(), name, name1);
        }
      }
    }
  }

  private final boolean showReadErrs = false;

  void init2(Map<Short, CodeFlagTables> table) throws IOException {
    // Table is hand modified from BC_CodeFlagTable.xls file from AShimazaki@wmo.int Dec 01, 2009
    String filename = BufrTables.RESOURCE_PATH + "wmo/BC_CodeFlagTable.csv";

    try (InputStream is = CodeFlagTables.class.getResourceAsStream(filename);
        BufferedReader dataIS = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF8")))) {
      int count = 0;
      while (true) {
        String line = dataIS.readLine();
        if (line == null)
          break;
        if (line.startsWith("#"))
          continue;
        count++;

        if (count == 1) { // skip first line - its the header
          continue;
        }

        // any commas that are embedded in quotes - replace with blanks for now so split works
        int pos1 = line.indexOf('"');
        if (pos1 >= 0) {
          int pos2 = line.indexOf('"', pos1 + 1);
          StringBuilder sb = new StringBuilder(line);
          for (int i = pos1; i < pos2; i++)
            if (sb.charAt(i) == ',')
              sb.setCharAt(i, ' ');
          line = sb.toString();
        }

        String[] flds = line.split(",");
        if (flds.length < 4) {
          if (showReadErrs)
            System.out.printf("%d BAD split == %s%n", count, line);
          continue;
        }

        // SNo,FXY,CodeFigure,enDescription1,enDescription2,enDescription3
        int fldidx = 1; // start at 1 to skip sno
        try {
          int xy = Integer.parseInt(flds[fldidx++].trim());
          int no;
          try {
            no = Integer.parseInt(flds[fldidx++].trim());
          } catch (NumberFormatException e) {
            if (showReadErrs)
              System.out.printf("%d skip == %s%n", count, line);
            continue;
          }
          String name = StringUtil2.remove(flds[fldidx], '"');
          String nameLow = name.toLowerCase();
          if (nameLow.startsWith("reserved"))
            continue;
          if (nameLow.startsWith("not used"))
            continue;

          int x = xy / 1000;
          int y = xy % 1000;
          int fxy = (x << 8) + y;

          CodeFlagTables ct = table.get((short) fxy);
          if (ct == null) {
            ct = new CodeFlagTables((short) fxy, null);
            table.put((short) fxy, ct);
            // System.out.printf(" added in 2: %s (%d)%n", ct.fxy(), ct.fxy);
          }
          ct.addValue((short) no, name);

        } catch (NumberFormatException e) {
          if (showReadErrs)
            System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
        }
      }
    }
  }

}
