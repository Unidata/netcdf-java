/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal;

import java.io.File;
import java.net.URI;
import java.net.URL;
import org.junit.Test;

/** Examine how java.net.URI breaks up a uri string */
public class TestUriCreate {

  @Test
  public void testMisc() {
    showUri("file:test/dir");
    showUri("file:/test/dir");
    showUri("file://test/dir");
    showUri("file:///test/dir");

    // test("file:C:/Program Files (x86)/Apache Software Foundation/Tomcat 5.0/content/thredds/cache"); // fail on blank
    // char
    // test("file:C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\cache"); // fail on
    // blank char
    showUri("http://localhost:8080/thredds/catalog.html?hi=lo");
  }

  @Test
  public void testReletiveFile() throws Exception {
    new URL("file:src/test/data/ncml/nc/");

    showUri("src/test/data/ncml/nc/");
    URI uri = new URI("src/test/data/ncml/nc/");

    showUri("file:/src/test/data/ncml/nc/");
    uri = new URI("file:/src/test/data/ncml/nc/");
    new File(uri); // ok

    showUri("file:src/test/data/ncml/nc/");
    uri = new URI("file:src/test/data/ncml/nc/");
  }

  private void showUri(String uriS) {
    System.out.println(uriS);
    URI uri = URI.create(uriS);
    System.out.println(" scheme=" + uri.getScheme());
    System.out.println(" getSchemeSpecificPart=" + uri.getSchemeSpecificPart());
    System.out.println(" getAuthority=" + uri.getAuthority());
    System.out.println(" getPath=" + uri.getPath());
    System.out.println(" getQuery=" + uri.getQuery());
    System.out.println(" isAbsolute=" + uri.isAbsolute());
    System.out.println(" isOpaque=" + uri.isOpaque());
    System.out.println();
  }

}
