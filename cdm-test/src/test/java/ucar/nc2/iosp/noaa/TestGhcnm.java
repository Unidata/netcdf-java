package ucar.nc2.iosp.noaa;

import static ucar.nc2.iosp.noaa.Ghcnm.RECORD;
import static ucar.nc2.iosp.noaa.Ghcnm.STNID;
import static ucar.nc2.iosp.noaa.Ghcnm.STNS;
import static ucar.nc2.iosp.noaa.Ghcnm.YEAR;
import static ucar.nc2.iosp.noaa.Ghcnm.dataPattern;

import com.google.re2j.Matcher;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.Sequence;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestGhcnm {

  static private NetcdfFile open(String filename) throws IOException {
    Ghcnm iosp = new Ghcnm();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, filename);
    iosp.open(raf, ncfile, null);
    return ncfile;
  }

  static private void stnDuplicates(String filename, Set<Integer> stns, boolean wantDups) throws IOException {
    System.out.printf("%s%n", filename);
    int count = 0;
    int countDups = 0;
    NetcdfFile ncfile = open(filename);
    Sequence seq = (Sequence) ncfile.findVariable(STNS);
    try (StructureDataIterator iter = seq.getStructureIterator(-1)) {
      while (iter.hasNext()) {
        count++;
        StructureData sdata = iter.next();
        StructureMembers.Member m = sdata.findMember(STNID);
        int stnid = sdata.getScalarInt(m);
        if (stns.contains(stnid)) {
          countDups++;
          if (!wantDups) System.out.printf("  dup %d%n", stnid);
        } else {
          stns.add(stnid);
          if (wantDups) System.out.printf("  dup %d%n", stnid);
        }
      }
    }
    System.out.printf(" counts=%d dups=%d%n", count, countDups);
  }

  // LOOK make into test
  static public void main2(String args[]) throws IOException {
    Set<Integer> stns = new HashSet<>(10 * 1000);
    stnDuplicates("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qae.inv", stns, false);
    stnDuplicates("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qca.inv", stns, true);
    stnDuplicates("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qcu.inv", stns, true);
  }

  static private int parseLine(String line) {
    int balony = 0;
    Matcher matcher = dataPattern.matcher(line);
    if (matcher.matches()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        String r = matcher.group(i);
        if (r == null) continue;
        int value = (int) Long.parseLong(r.trim());
        balony += value;
      }
    } else {
      System.out.printf("Fail on %s%n", line);
    }
    return balony;
  }


  static private void readDataRegexp(String filename) throws IOException {
    int balony = 0;
    long start = System.currentTimeMillis();
    System.out.printf("regexp %s%n", filename);
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      String line;
      while (true) {
        line = raf.readLine();
        if (line == null) break;
        if (line.startsWith("#")) continue;
        if (line.trim().length() == 0) continue;
        balony += parseLine(line);
      }
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf("DONE %d == %d msecs%n", balony, took);
  }

  static private void readData(String filename) throws IOException {
    long start = System.currentTimeMillis();
    System.out.printf("%s%n", filename);
    int balony = 0;
    try (NetcdfFile ncfile = open(filename)) {
      Sequence seq = (Sequence) ncfile.findVariable(RECORD);
      try (StructureDataIterator iter = seq.getStructureIterator(-1)) {
        while (iter.hasNext()) {
          StructureData sdata = iter.next();
          StructureMembers.Member m = sdata.findMember(YEAR);
          balony += sdata.getScalarInt(m);

        /* StructureMembers sm = sdata.getStructureMembers();
       for (StructureMembers.Member m : sm.getMembers()) {
         Array data = sdata.getArray(m);
         balony += data.getSize();
       } */
        }
      }
    }
    long took = System.currentTimeMillis() - start;
    System.out.printf("DONE %d == %d msecs%n", balony, took);
  }

  // LOOK make into test
  public void testStuff() throws IOException {
    readData("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qcu.dat");
    readDataRegexp("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qcu.dat");
  }

}


/*

  static public void main(String args[]) throws IOException {
    //InputStream is = cl.getResourceAsStream("resources/nj22/tables/nexrad.tbl");
    InputStream is = new FileInputStream("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qae.inv");

    List<TableParser.Record> recs = TableParser.readTable(is, "11,20d,30d,37d,68,73i,74,79i,81,83,85,87i,88,90i,106,107", 10);
    Formatter f = new Formatter(System.out);
    //f.format("CNTRY WMO ID      YEAR  ELEM  VAL DM QC DS%n");
    for (TableParser.Record record : recs) {
      record.toString(f);
    }
  }
 */

