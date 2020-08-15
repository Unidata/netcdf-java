/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.net.URLnaming;

/** Test URLnaming.resolve() */
public class TestURLnaming {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testBlanks() {
    testResolve("file:/test/me/", "blank in dir", "file:/test/me/blank in dir");
  }

  @Test
  public void testResolve() {
    testResolve("http://test/me/", "wanna", "http://test/me/wanna");
    testResolve("http://test/me/", "/wanna", "http://test/wanna");
    testResolve("file:/test/me/", "wanna", "file:/test/me/wanna");
    testResolve("file:/test/me/", "/wanna", "/wanna"); // LOOK doesnt work for URI.resolve() directly.

    testResolve("file://test/me/", "http:/wanna", "http:/wanna");
    testResolve("file://test/me/", "file:/wanna", "file:/wanna");
    testResolve("file://test/me/", "C:/wanna", "C:/wanna");
    testResolve("http://test/me/", "file:wanna", "file:wanna");
  }

  private void testResolve(String base, String rel, String result) {
    System.out.println("baseUri          = " + base);
    System.out.println("reletiveUri      = " + rel);
    System.out.println("URLnaming.resolve= " + URLnaming.resolve(base, rel));
    System.out.println();
    if (result != null)
      assert URLnaming.resolve(base, rel).equals(result);
  }

}
