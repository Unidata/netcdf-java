/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation. Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.test.UnitTestCommon;
import java.lang.invoke.MethodHandles;
import java.util.List;

public class TestHTTPSession extends UnitTestCommon {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //////////////////////////////////////////////////
  // Constants

  // static final String TESTURL1 = "http://" + TestDir.dap2TestServer + "/dts/test.01.dds";
  static final String TESTURL1 = "https://thredds-dev.unidata.ucar.edu";
  static final String GLOBALAGENT = "TestUserAgent123global";
  static final String SESSIONAGENT = "TestUserAgent123session";

  static final String USER = "dmh";
  static final String PWD = "FakDennisPassword";
  //////////////////////////////////////////////////
  // Define the test sets

  int passcount = 0;
  int xfailcount = 0;
  int failcount = 0;
  boolean verbose = true;
  boolean pass = false;

  String datadir = null;
  String threddsroot = null;

  public TestHTTPSession() {
    super();
    setTitle("HTTP Session tests");
    HTTPSession.TESTING = true;
  }

  @Test
  public void testAgent() throws Exception {

    logger.debug("*** Testing: User Agent");
    logger.debug("*** URL: {}", TESTURL1);
    logger.debug("Test: HTTPSession.setGlobalUserAgent({})", GLOBALAGENT);

    HTTPSession.setInterceptors(false);
    HTTPSession.setGlobalUserAgent(GLOBALAGENT);
    try (HTTPSession session = HTTPFactory.newSession(TESTURL1)) {
      List<Header> agents = null;
      HTTPMethod method = HTTPFactory.Get(session, TESTURL1);
      method.execute();
      // Use special interface to access the request
      // Look for the user agent header
      agents = HTTPSession.debugRequestInterceptor().getHeaders(HTTPSession.HEADER_USERAGENT);
      Assert.assertFalse("User-Agent Header not found", agents.size() == 0);
      // It is possible to see multiple same headers, so verify that they have same value
      String agentvalue = null;
      for (Header h : agents) {
        Assert.assertTrue("Bad Agent Header", h.getName().equals("User-Agent"));
        if (agentvalue == null)
          agentvalue = h.getValue();
        else
          Assert.assertTrue("Bad Agent Value", h.getValue().equals(agentvalue));
      }
      Assert.assertTrue(String.format("User-Agent mismatch: expected %s found:%s", GLOBALAGENT, agentvalue),
          GLOBALAGENT.equals(agentvalue));
      logger.debug("*** Pass: set global agent");
      // method.close();

      logger.debug("Test: HTTPSession.setUserAgent({})", SESSIONAGENT);
      HTTPSession.resetInterceptors();
      session.setUserAgent(SESSIONAGENT);
      method = HTTPFactory.Get(session, TESTURL1);
      method.execute();
      // Use special interface to access the request
      agents = HTTPSession.debugRequestInterceptor().getHeaders(HTTPSession.HEADER_USERAGENT);
      Assert.assertFalse("User-Agent Header not found", agents.size() == 0);
      agentvalue = null;
      for (Header h : agents) {
        Assert.assertTrue("Bad Agent Header", h.getName().equals("User-Agent"));
        if (agentvalue == null)
          agentvalue = h.getValue();
        else
          Assert.assertTrue("Bad Agent Value", h.getValue().equals(agentvalue));
      }
      Assert.assertTrue(String.format("User-Agent mismatch: expected %s found:%s", SESSIONAGENT, agentvalue),
          SESSIONAGENT.equals(agentvalue));
      logger.debug("*** Pass: set session agent");
      method.close();
    }
  }

  // Verify that other configuration parameters Can at least be set.
  @Test
  public void testConfigure() throws Exception {
    try (HTTPSession session = HTTPFactory.newSession(TESTURL1)) {
      logger.debug("Test: HTTPSession: Configuration");
      session.setSoTimeout(17777);
      session.setConnectionTimeout(37777);
      session.setMaxRedirects(111);
      Credentials bp = new UsernamePasswordCredentials(USER, PWD);
      BasicCredentialsProvider bcp = new BasicCredentialsProvider();
      bcp.setCredentials(AuthScope.ANY, bp);
      session.setCredentialsProvider(bcp);
      // session.setAuthorizationPreemptive(true); not implemented

      HTTPMethod method = HTTPFactory.Get(session, TESTURL1);
      method.execute();

      // Use special interface to access the config
      RequestConfig dbgcfg = method.getDebugConfig();

      boolean b = dbgcfg.isCircularRedirectsAllowed();
      logger.debug("Test: Circular Redirects");
      Assert.assertTrue("*** Fail: Circular Redirects", b);
      logger.debug("*** Pass: Circular Redirects");

      logger.debug("Test: Max Redirects");
      int n = dbgcfg.getMaxRedirects();
      Assert.assertTrue("*** Fail: Max Redirects", n == 111);
      logger.debug("*** Pass: Max Redirects");

      logger.debug("Test: SO Timeout");
      n = dbgcfg.getSocketTimeout();
      Assert.assertTrue("*** Fail: SO Timeout", n == 17777);
      logger.debug("*** Pass: SO Timeout");

      logger.debug("Test: Connection Timeout");
      n = dbgcfg.getConnectTimeout();
      Assert.assertTrue("*** Fail: Connection Timeout", n == 37777);
      logger.debug("*** Pass: SO Timeout");
      method.close();
    }
  }
}
