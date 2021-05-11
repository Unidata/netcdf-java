package thredds.ui;

import com.google.common.net.UrlEscapers;
import java.io.IOException;
import org.junit.Test;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.ui.util.SocketMessage;

public class TestSocketMessage {
  private static final boolean testing = false;

  @Test
  public void testStuff() throws IOException {
    if (!testing) {
      SocketMessage sm = new SocketMessage(9999, "startNewServer");
      sm.setRaw(true);

    } else {
      String query = UrlEscapers.urlFragmentEscaper().escape("quuery[1]");
      String url = "http://localhost:8080/thredds/test/it?" + query;
      System.out.printf("send '%s'%n", url);
      try (HTTPMethod method = HTTPFactory.Head(url)) {
        method.execute();
        int status = method.getStatusCode();
        System.out.printf("%d%n", status);
      } // close method, close method internal session
    }
  }

}
