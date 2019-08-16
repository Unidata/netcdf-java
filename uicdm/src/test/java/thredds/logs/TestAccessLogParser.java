package thredds.logs;

import com.google.re2j.Matcher;
import java.io.IOException;
import org.junit.Test;

public class TestAccessLogParser {

  @Test
  public void testStuff() throws IOException {
    AccessLogParser p = new AccessLogParser();
    String line = "24.18.236.132 - - [04/Feb/2011:17:49:03 -0700] \"GET /thredds/fileServer//nexrad/level3/N0R/YUX/20110205/Level3_YUX_N0R_20110205_0011.nids \" 200 10409 \"-\" \"-\" 17";
    Matcher m = AccessLogParser.regPattern.matcher(line);
    System.out.printf("%s %s%n", m.matches(), m);
    for (int i=0; i<m.groupCount(); i++) {
      System.out.println(" "+i+ " "+m.group(i));
    }

    LogReader.Log log = p.parseLog(line);
    System.out.printf("%s%n", log);
  }


}
