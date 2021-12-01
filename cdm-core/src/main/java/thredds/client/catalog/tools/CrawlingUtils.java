/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.tools;

import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Utilities for crawling and testing TDS servers */
public class CrawlingUtils {
  private static final Random random = new Random();

  // read a 2D slice out all the variables in the dataset, report stats
  // Its a Runnable, so you can put it into a Thread for mulithreaded testing
  public static class TDSdatasetReader implements Runnable {
    private final boolean showDetail;
    private final String who;
    private final String datasetUrl;
    private final CancelTask cancel;

    public TDSdatasetReader(String who, String datasetUrl, CancelTask cancel, boolean showDetail) {
      this.who = who;
      this.datasetUrl = datasetUrl;
      this.cancel = cancel;
      this.showDetail = showDetail;
    }

    public void run() {
      int count = 0;
      long total = 0, time = 0;
      System.out.printf("TDSdatasetReader %s started url=%s%n", who, datasetUrl);
      try (NetcdfFile ncfile = NetcdfDatasets.openFile(datasetUrl, cancel)) {
        for (Variable var : ncfile.getAllVariables()) {
          long start = System.currentTimeMillis();
          Array result = doLimitedRead(var);
          long took = System.currentTimeMillis() - start;

          long size = result.getSize();
          double rate = (took == 0) ? 0.0 : (double) size / took / 1000.0;
          if (showDetail)
            System.out.printf(" took= %d msecs rate= %f MB/sec%n", took, rate);
          total += size;
          time += took;
          // if (stop.isCancel()) break;
          count++;
        }

        double totald = total / (1000. * 1000.);
        double rate = (time == 0) ? 0 : (double) total / time / 1000.0;

        System.out.printf("%n%s%n", ncfile.getLocation());
        System.out.printf(" took= %f secs rate= %f MB/sec%n", totald, rate);

      } catch (IOException | InvalidRangeException e) {
        e.printStackTrace();
      }
      System.out.printf(" thread done %d%n", count);
    }

    private Array<?> doLimitedRead(Variable v) throws IOException, InvalidRangeException {
      long size = v.getSize() * v.getElementSize();
      if (size < 1000 * 1000 || v.getRank() < 3) {
        return v.readArray();

      } else {
        // randomly choose a 2D slice
        int rank = v.getRank();
        List<Range> ranges = new ArrayList<>();
        int i = 0;
        for (Dimension dim : v.getDimensions()) {
          if (i < rank - 2) {
            int first = random.nextInt(dim.getLength());
            ranges.add(new Range(first, first));
          } else {
            ranges.add(new Range(0, dim.getLength() - 1));
          }
          i++;
        }
        Section s = new Section(ranges);
        if (showDetail)
          System.out.printf(" thread %s read %s(%s) bytes= %d ", who, v.getFullName(), s, s.computeSize());
        Array<?> result = v.readArray(s);
        assert result.getSize() == s.computeSize();
        return result;
      }
    }
  }
}
