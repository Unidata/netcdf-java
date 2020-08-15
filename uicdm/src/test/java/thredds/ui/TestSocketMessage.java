package thredds.ui;

import java.io.IOException;
import org.junit.Test;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.ui.util.SocketMessage;
import ucar.nc2.internal.util.EscapeStrings;

public class TestSocketMessage {
  private static final boolean testing = false;

  @Test
  public void testStuff() throws IOException {
    if (!testing) {
      SocketMessage sm = new SocketMessage(9999, "startNewServer");
      sm.setRaw(true);

    } else {
      String url = "http://localhost:8080/thredds/test/it" // + EscapeStrings.escapeOGC("yabba/bad[0]/good")
          + "?" + EscapeStrings.escapeOGC("quuery[1]");
      System.out.printf("send '%s'%n", url);
      try (HTTPMethod method = HTTPFactory.Head(url)) {
        method.execute();
        int status = method.getStatusCode();
        System.out.printf("%d%n", status);
      } // close method, close method internal session
    }
  }

}
