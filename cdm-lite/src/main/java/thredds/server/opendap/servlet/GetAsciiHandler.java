/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import thredds.server.opendap.servlet.ascii.toASCII;
import opendap.dap.BaseType;
import opendap.dap.DAP2Exception;
import opendap.dap.DConnect2;
import opendap.dap.DataDDS;
import opendap.dap.parsers.ParseException;
import thredds.server.opendap.servlet.ascii.asciiFactory;

/**
 * Default handler for OPeNDAP ascii requests. This class is used
 * by AbstractServlet. This code exists as a separate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetAsciiHandler {
  private static final boolean _Debug = false;

  /**
   * ************************************************************************
   * Default handler for OPeNDAP ascii requests. Returns OPeNDAP DAP2 data in
   * comma delimited ascii columns for ingestion into some not so
   * OPeNDAP enabled application such as MS-Excel. Accepts constraint
   * expressions in exactly the same way as the regular OPeNDAP dataserver.
   *
   * @param rs
   * @throws DAP2Exception
   * @throws ParseException
   */
  public void sendASCII(ReqState rs, String dataSet) throws DAP2Exception, ParseException {
    String requestURL, ce;
    DataDDS dds;

    if (rs.getConstraintExpression() == null) {
      ce = "";
    } else {
      ce = "?" + rs.getConstraintExpression();
    }

    int suffixIndex = rs.getRequestURL().toString().lastIndexOf(".");

    requestURL = rs.getRequestURL().substring(0, suffixIndex);

    if (_Debug)
      System.out.println("Making connection to .dods service...");

    try (DConnect2 url = new DConnect2(requestURL, true)) {

      if (_Debug)
        System.out.println("Requesting data...");
      dds = url.getData(ce, null, new asciiFactory());

      if (_Debug)
        System.out.println(" ASC DDS: ");
      if (_Debug)
        dds.print(System.out);

      PrintWriter pw =
          new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(), StandardCharsets.UTF_8));
      PrintWriter pwDebug = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

      if (dds != null) {
        dds.print(pw);
        pw.println("---------------------------------------------");


        String s = "";
        for (BaseType bt : dds.getVariables()) {
          if (_Debug)
            ((toASCII) bt).toASCII(pwDebug, true, null, true);
          // bt.toASCII(pw,addName,getNAme(),true);
          ((toASCII) bt).toASCII(pw, true, null, true);
        }
      } else {

        String betterURL = rs.getRequestURL().substring(0, rs.getRequestURL().lastIndexOf(".")) + ".dods?"
            + rs.getConstraintExpression();

        pw.println("-- ASCII RESPONSE HANDLER PROBLEM --");
        pw.println("");
        pw.println("The ASCII response handler was unable to obtain requested data set.");
        pw.println("");
        pw.println("Because this handler calls it's own OPeNDAP server to get the requested");
        pw.println("data the source error is obscured.");
        pw.println("");
        pw.println("To get a better idea of what is going wrong, try requesting the URL:");
        pw.println("");
        pw.println("    " + betterURL);
        pw.println("");
        pw.println("And then look carefully at the returned document. Note that if you");
        pw.println("are using a browser to access the URL the returned document will");
        pw.println("more than likely be treated as a download and written to your");
        pw.println("local disk. It should be a file with the extension \".dods\"");
        pw.println("");
        pw.println("Locate it, open it with a text editor, and find your");
        pw.println("way to happiness and inner peace.");
        pw.println("");
      }

      // pw.println("</pre>");
      pw.flush();
      if (_Debug)
        pwDebug.flush();

    } catch (FileNotFoundException fnfe) {
      System.out.println("OUCH! FileNotFoundException: " + fnfe.getMessage());
      fnfe.printStackTrace(System.out);
    } catch (MalformedURLException mue) {
      System.out.println("OUCH! MalformedURLException: " + mue.getMessage());
      mue.printStackTrace(System.out);
    } catch (IOException ioe) {
      System.out.println("OUCH! IOException: " + ioe.getMessage());
      ioe.printStackTrace(System.out);
    } catch (Throwable t) {
      System.out.println("OUCH! Throwable: " + t.getMessage());
      t.printStackTrace(System.out);
    }

    if (_Debug)
      System.out.println(" GetAsciiHandler done");
  }

}


