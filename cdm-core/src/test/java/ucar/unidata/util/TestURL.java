/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.util;

import org.junit.Test;
import java.net.*;

public class TestURL {

  @Test
  public void testURL() throws Exception {
    doAll("http://adde.ucar.edu/pointdata?select='id%20TXKF'");
    doAll("http://adde.ucar.edu/test%20test2");
  }

  private void doAll(String s) throws Exception {
    doURIencoded(s);
    doURI(s);
    doURL(s);
  }

  private void doURL(String u) throws MalformedURLException {
    URL url = new URL(u);
    System.out.println("TestURL host = " + url.getHost());
    System.out.println("TestURL file = " + url.getFile());
  }

  private void doURI(String u) throws URISyntaxException {
    URI uri = new URI(u);
    System.out.println("TestURI url = " + uri.toString());
  }

  private void doURIencoded(String u) throws URISyntaxException {
    String encoded = URLEncoder.encode(u);
    URI uri = new URI(encoded);
    System.out.println("TestURI url encoded = " + uri.toString());
  }

}
