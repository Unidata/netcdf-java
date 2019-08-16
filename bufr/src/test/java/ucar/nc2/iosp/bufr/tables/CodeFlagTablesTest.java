package ucar.nc2.iosp.bufr.tables;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class CodeFlagTablesTest {

  @Test
  public void testStuff() {
    HashMap<Short, CodeFlagTables> tableMap1 = new HashMap<>(300);
    CodeFlagTables.init(tableMap1);

    HashMap<Short, CodeFlagTables> tableMap2 = new HashMap<>(300);
    CodeFlagTables.init2(tableMap2);

    System.out.printf("Compare 1 with 2%n");
    for (Map.Entry<Short, CodeFlagTables> ent : tableMap1.entrySet()) {
      CodeFlagTables t = ent.getValue();
      CodeFlagTables t2 = tableMap2.get(ent.getKey());
      if (t2 == null)
        System.out.printf(" NOT FOUND in 2: %s (%d)%n", t.fxy(), t.getId());
      else {
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
      if (t1 == null)
        System.out.printf(" NOT FOUND in 1: %s (%d)%n", t.fxy(), t.getId());
      else {
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
}
