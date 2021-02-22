/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DDS;
import opendap.dap.parsers.ParseException;
import thredds.server.opendap.servers.ServerDDS;
import thredds.server.opendap.servlet.www.jscriptCore;
import thredds.server.opendap.servlet.www.wwwFactory;
import thredds.server.opendap.servlet.www.wwwOutPut;
import ucar.nc2.constants.CDM;

/**
 * Default handler for OPeNDAP .html requests. This class is used
 * by AbstractServlet. This code exists as a separate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetHTMLInterfaceHandler {

  private static final boolean _Debug = false;
  private String helpLocation = "http://www.opendap.org/online_help_files/";

  /**
   * ************************************************************************
   * Default handler for OPeNDAP .html requests. Returns an html form
   * and javascript code that allows the user to use their browser
   * to select variables and build constraints for a data request.
   * The DDS and DAS for the data set are used to build the form. The
   * types in opendap.servlet.www are integral to the form generation.
   *
   * @param rs The <code>ReqState</code> from the client.
   * @param dataSet
   * @param sdds
   * @param myDAS
   * @throws DAP2Exception
   * @throws ParseException
   * @see wwwFactory
   */
  public void sendDataRequestForm(ReqState rs, String dataSet, ServerDDS sdds, DAS myDAS) // changed jc
      throws DAP2Exception, ParseException {


    if (_Debug)
      System.out.println(
          "Sending DODS Data Request Form For: " + dataSet + "    CE: '" + rs.getRequest().getQueryString() + "'");
    String requestURL;

    /*
     * // Turn this on later if we discover we're supposed to accept
     * // constraint expressions as input to the Data Request Web Form
     * String ce;
     * if(request.getQueryString() == null){
     * ce = "";
     * }
     * else {
     * ce = "?" + request.getQueryString();
     * }
     */


    int suffixIndex = rs.getRequest().getRequestURL().toString().lastIndexOf(".");

    requestURL = rs.getRequest().getRequestURL().substring(0, suffixIndex);

    String dapCssUrl = "/" + requestURL.split("/", 5)[3] + "/" + "tdsDap.css";


    try {

      // PrintWriter pw = new PrintWriter(response.getOutputStream());
      PrintWriter pw;
      if (false) {
        pw = new PrintWriter(new FileOutputStream(new File("debug.html")));
      } else
        pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(), StandardCharsets.UTF_8));


      wwwOutPut wOut = new wwwOutPut(pw);

      // Get the DDS and the DAS (if one exists) for the dataSet.
      DDS myDDS = getWebFormDDS(dataSet, sdds);
      // DAS myDAS = dServ.getDAS(dataSet); // change jc

      jscriptCore jsc = new jscriptCore();

      pw.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"\n"
          + "\"http://www.w3.org/TR/REC-html40/loose.dtd\">\n"
          + "<html><head><title>OPeNDAP Dataset Query Form</title>\n"
          + "<link type=\"text/css\" rel=\"stylesheet\" media=\"screen\" href=\"" + dapCssUrl + "\"/>\n"
          + "<base href=\"" + helpLocation + "\">\n" + "<script type=\"text/javascript\">\n" + "<!--\n");
      pw.flush();

      pw.println(jsc.jScriptCode);
      pw.flush();

      pw.println("DODS_URL = new dods_url(\"" + requestURL + "\");\n" + "// -->\n" + "</script>\n" + "</head>\n"
          + "<body>\n" + "<p><h2 align='center'>OPeNDAP Dataset Access Form</h2>\n" + "<hr>\n" + "<form action=\"\">\n"
          + "<table>\n");
      pw.flush();

      wOut.writeDisposition(requestURL);
      pw.println("<tr><td><td><hr>\n");

      wOut.writeGlobalAttributes(myDAS, myDDS);
      pw.println("<tr><td><td><hr>\n");

      wOut.writeVariableEntries(myDAS, myDDS);
      pw.println("</table></form>\n");
      pw.println("<hr>\n");


      pw.println("<address>Send questions or comments to: " + "<a href=\"mailto:support@unidata.ucar.edu\">"
          + "support@unidata.ucar.edu" + "</a></address>" + "</body></html>\n");

      pw.println("<hr>");
      pw.println("<h2>DDS:</h2>");

      pw.println("<pre>");
      myDDS.print(pw);
      pw.println("</pre>");
      pw.println("<hr>");
      pw.flush();


    } catch (IOException ioe) {
      System.out.println("OUCH! IOException: " + ioe.getMessage());
      ioe.printStackTrace(System.out);
    }


  }


  /**
   * ************************************************************************
   * Gets a DDS for the specified data set and builds it using the class
   * factory in the package <b>opendap.servlet.www</b>.
   * <p/>
   * Currently this method uses a deprecated API to perform a translation
   * of DDS types. This is a known problem, and as soon as an alternate
   * way of achieving this result is identified we will implement it.
   * (Your comments appreciated!)
   *
   * @param dataSet A <code>String</code> containing the data set name.
   *        3 * @return A DDS object built using the www interface class factory.
   * @see DDS
   * @see wwwFactory
   */
  public DDS getWebFormDDS(String dataSet, ServerDDS sDDS) // changed jc
      throws DAP2Exception, ParseException {

    // Get the DDS we need, using the getDDS method
    // for this particular server
    // ServerDDS sDDS = dServ.getDDS(dataSet);

    // Make a special print writer to catch the ServerDDS's
    // persistent representation in a String.
    StringWriter ddsSW = new StringWriter();
    sDDS.print(new PrintWriter(ddsSW));

    // Now use that string to make an input stream to
    // pass to our new DDS for parsing.

    // Since parser expects/requires InputStream,
    // we must adapt utf16 string to at least utf-8

    ByteArrayInputStream bai = null;
    try {
      bai = new ByteArrayInputStream(ddsSW.toString().getBytes(CDM.UTF8));
    } catch (UnsupportedEncodingException uee) {
      throw new DAP2Exception("UTF-8 encoding not supported");
    }

    // Make a new DDS parser using the web form (www interface) class factory
    wwwFactory wfactory = new wwwFactory();
    DDS wwwDDS = new DDS(dataSet, wfactory);
    wwwDDS.setURL(dataSet);
    wwwDDS.parse(bai);
    return (wwwDDS);


  }


}


