/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * Re: github issue https://github.com/Unidata/thredds/issues/431
 * Test that a single HTTPSession with many method invocations
 * does not hang properly releases the connections.
 */

package ucar.nc2.util.net;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.test.TestDir;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Test accessing a number of urls
 * with at least one being non-existent
 */

@RunWith(Parameterized.class)
public class TestHang {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static final int NCONN = 3;

  static final Integer[] XFAIL = new Integer[] {0};

  static private HTTPSession session;

  protected static final String server = "http://" + TestDir.dap2TestServer;

  protected static final String url = server + "/dts/test.%02d.dds";

  static boolean isxfail(int x) {
    for (Integer i : XFAIL) {
      if (i == x)
        return true;
    }
    return false;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    for (int i = 0; i < NCONN; i++) {
      result.add(new Object[] {(Integer) i});
    }
    return result;
  }

  @BeforeClass
  public static void setupCorrect() throws HTTPException {
    HTTPSession sess = HTTPFactory.newSession(server);
    session = sess;
  }

  ///////////

  Integer datasetno;

  public TestHang(Integer i) {
    this.datasetno = i;
  }

  @Test
  public void testSession() throws Exception {
    String fullUrl = String.format(url, this.datasetno);
    try (HTTPMethod m = HTTPFactory.Get(session, fullUrl)) {
      logger.debug("Connecting to {}", fullUrl);
      int status = 0;
      try {
        status = m.execute();
      } catch (Exception e) {
        e.printStackTrace();
        status = 500;
      }
      logger.debug("    return from {} status= {}", fullUrl, status);
      if (isxfail(this.datasetno))
        Assert.assertTrue("Expected 404: return status: " + status, status == 404);
      else
        Assert.assertTrue("Expected 200: return status: " + status, status == 200);
    }
  }
}
