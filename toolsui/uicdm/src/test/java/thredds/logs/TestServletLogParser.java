package thredds.logs;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

public class TestServletLogParser {

  static class MyLogFilter implements LogReader.LogFilter {
    int count = 0;
    int limit;

    MyLogFilter(int limit) {
      this.limit = limit;
    }

    public boolean pass(LogReader.Log log) {
      return (limit < 0) || count++ < limit;
    }
  }

  @Test
  public void testStuff1() throws IOException {
    // test
    final LogReader reader = new LogReader(new ServletLogParser());

    long startElapsed = System.nanoTime();
    LogReader.Stats stats = new LogReader.Stats();

    reader.readAll(new File("D:/mlode/logs/servlet/"), null, new LogReader.Closure() {
      public void process(LogReader.Log log) throws IOException {
        // if (count < limit) System.out.printf(" %s %n", log);
      }
    }, new MyLogFilter(-1), stats);

    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" total= %d passed=%d%n", stats.total, stats.passed);
    System.out.printf(" elapsed=%d secs%n", elapsedTime / (1000 * 1000 * 1000));
  }

  @Test
  public void testStuff2() {
    // 1 2 3 4 5 6 7 8 9
    String s1 =
        "2009-03-10T16:08:55.184 -0600 [  16621850][  162233] INFO  - thredds.server.opendap.NcDODSServlet - Remote host: 128.117.140.71 - Request: \"GET /thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20090309_0000.grib1.dods?Geopotential_height%5B7:1:7%5D%5B0:10:10%5D%5B0:1:64%5D%5B0:1:92%5D HTTP/1.1\"";
    // 1 2 3 4 5 6 7 8 9
    Pattern p1 = Pattern.compile(
        "^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");

    show(s1, p1);

    // 1 2 3 4 5 6 7 8 9
    String s2 =
        "2009-03-10T16:08:54.617 -0600 [  16621283][  162230] INFO  - thredds.server.opendap.NcDODSServlet - Request Completed - 200 - -1 - 47";
    // 1 2 3 4 5 6 7 8 9
    Pattern p2 = Pattern.compile(
        "^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - Request Completed - (\\d+) - (.*) - (.*)");
    // Pattern p1 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+-
    // ([^-]+) - Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");

    show(s2, p2);

    // Pattern p2 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+-
    // ([^-]+) - Request Completed - (\\d+) - (.*) - (.*)");
    // Pattern p1 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+-
    // ([^-]+) - Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");
    Pattern p3 =
        Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - (.*)");

    show(s1, p3);
    show(s2, p3);

  }

  @Test
  public void testStuff() {
    // 1 2 3 4 5 6 7 8 9
    String test =
        "2009-04-03T21:48:54.000 -0600 [   4432114][    1184] INFO  - thredds.servlet.ServletUtil - Remote host: 128.117.140.75 - Request: \"GET /thredds/dodsC/fmrc/NCEP/NAM/CONUS_20km/surface/forecast/NCEP-NAM-CONUS_20km-surface_ConstantForecast_2009-04-04T15:00:00Z.dods?time,time_run,time_offset,Lambert_Conformal,pressure_difference_layer_bounds,depth_below_surface_layer_bounds,pressure_difference_layer1_bounds,depth_below_surface_layer1_bounds,depth_below_surface_layer2_bounds,pressure_difference_layer2_bounds,pressure_difference_layer3_bounds,pressure_difference_layer4_bounds,pressure_difference_layer5_bounds,height_above_ground_layer1_bounds,pressure_layer_bounds,height_above_ground_layer_bounds,y,x,height_above_ground1,height_above_ground2,pressure_difference_layer,depth_below_surface_layer,pressure_difference_layer1,depth_below_surface_layer1,depth_below_surface_layer2,height_above_ground,pressure_difference_layer2,height_above_ground3,pressure,depth_below_surface,pressure1,pressure_difference_layer3,pressure_difference_layer4,pressure_difference_layer5,height_above_ground_layer1,pressure2,pressure_layer,height_above_ground_layer HTTP/1.1\"";
    show(test, ServletLogParser.commonPattern);

    String test2 =
        "Remote host: 128.117.140.75 - Request: \"GET /thredds/dodsC/fmrc/NCEP/NAM/CONUS_20km/surface/forecast/NCEP-NAM-CONUS_20km-surface_ConstantForecast_2009-04-04T15:00:00Z.dods?time,time_run,time_offset,Lambert_Conformal,pressure_difference_layer_bounds,depth_below_surface_layer_bounds,pressure_difference_layer1_bounds,depth_below_surface_layer1_bounds,depth_below_surface_layer2_bounds,pressure_difference_layer2_bounds,pressure_difference_layer3_bounds,pressure_difference_layer4_bounds,pressure_difference_layer5_bounds,height_above_ground_layer1_bounds,pressure_layer_bounds,height_above_ground_layer_bounds,y,x,height_above_ground1,height_above_ground2,pressure_difference_layer,depth_below_surface_layer,pressure_difference_layer1,depth_below_surface_layer1,depth_below_surface_layer2,height_above_ground,pressure_difference_layer2,height_above_ground3,pressure,depth_below_surface,pressure1,pressure_difference_layer3,pressure_difference_layer4,pressure_difference_layer5,height_above_ground_layer1,pressure2,pressure_layer,height_above_ground_layer HTTP/1.1\"";
    show(test2, ServletLogParser.startPattern);

  }

  private static void show(String s, Pattern p) {
    System.out.println("==============================");
    Matcher m = p.matcher(s);
    System.out.printf(" match against %s = %s %n", m, m.matches());
    if (!m.matches())
      return;
    for (int i = 1; i <= m.groupCount(); i++)
      System.out.println(" " + i + " " + m.group(i));

  }

}
