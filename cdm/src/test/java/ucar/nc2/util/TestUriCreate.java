package ucar.nc2.util;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;

/** Examine how java.net.URI brakes up a uri string */
public class TestUriCreate {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testMisc() {
    showUri("file:test/dir");
    showUri("file:/test/dir");
    showUri("file://test/dir");
    showUri("file:///test/dir");

    // test("file:C:/Program Files (x86)/Apache Software Foundation/Tomcat 5.0/content/thredds/cache"); // fail on blank char
    // test("file:C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\cache"); // fail on blank char
    showUri("http://localhost:8080/thredds/catalog.html?hi=lo");
  }

  @Test
  public void testReletiveFile() throws MalformedURLException, URISyntaxException {
    new URL("file:src/test/data/ncml/nc/");

    showUri("src/test/data/ncml/nc/");
    URI uri = new URI("src/test/data/ncml/nc/");

    showUri("file:/src/test/data/ncml/nc/");
    uri = new URI("file:/src/test/data/ncml/nc/");
    new File(uri); // ok

    showUri("file:src/test/data/ncml/nc/");
    uri = new URI("file:src/test/data/ncml/nc/");
  }

  @Test
  public void testDods() throws URISyntaxException {
    String uriString = "http://" + TestDir.dap2TestServer + "/dts/test.53.dods?types[0:1:9]";
    showUri(uriString);
  }

  private void showUri(String uriS) {
    System.out.println(uriS);
    try {
      URI uri = URI.create(uriS);
      System.out.println(" scheme=" + uri.getScheme());
      System.out.println(" getSchemeSpecificPart=" + uri.getSchemeSpecificPart());
      System.out.println(" getAuthority=" + uri.getAuthority());
      System.out.println(" getPath=" + uri.getPath());
      System.out.println(" getQuery=" + uri.getQuery());
      System.out.println(" isAbsolute=" + uri.isAbsolute());
      System.out.println(" isOpaque=" + uri.isOpaque());
      System.out.println();
    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }
  }

}
