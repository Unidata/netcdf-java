/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.logs;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import ucar.nc2.util.EscapeStrings;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Read TDS access logs
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class ServletLogParser implements LogReader.LogParser {
  public static class ServletLog extends LogReader.Log {
    long reqTime;
    long reqSeq;
    String level, where;
    public StringBuilder extra;
    boolean isDone, isStart;
    int nthreads;

    public long getReqTime() {
      return reqTime;
    }

    public long getReqSeq() {
      return reqSeq;
    }

    public String getLevel() {
      return level;
    }

    public boolean isExtra() {
      return extra != null;
    }

    public boolean isDone() {
      return isDone;
    }

    public boolean isStart() {
      return isStart;
    }

    public int getNthreads() {
      return nthreads;
    }

    public void setNthreads(int nthreads) {
      this.nthreads = nthreads;
    }

    public String toString() {
      Formatter f = new Formatter();
      f.format("%s [%d] [%d] %s %s: ", getDate(), reqTime, reqSeq, level, where);

      if (isStart)
        f.format(" (%s) %s %n", ip, getPath());
      else if (isDone)
        f.format(" %d %d %d %n", returnCode, sizeBytes, msecs);

      if (extra != null)
        f.format(" %s", extra);

      return f.toString();
    }

    void addExtra(String s) {
      if (extra == null)
        extra = new StringBuilder(300);
      extra.append(s);
      extra.append("\n");
    }

  }

  ///////////////////////////////////////////////////////
  // log reading

  // sample
  // 128.117.140.75 - - [02/May/2008:00:46:26 -0600] "HEAD
  // /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080501_1800.grib2.dds HTTP/1.1" 200 - "null"
  // "Java/1.6.0_05" 21
  // Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\"
  // (\\d+)");

  // 2009-03-10T16:08:55.184 -0600 [ 16621850][ 162233] INFO - thredds.server.opendap.NcDODSServlet - Remote host:
  // 128.117.140.71 - Request: "GET
  // /thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20090309_0000.grib1.dods?Geopotential_height%5B7:1:7%5D%5B0:10:10%5D%5B0:1:64%5D%5B0:1:92%5D
  // HTTP/1.1"
  // 2009-03-10T16:08:55.218 -0600 [ 16621884][ 162233] INFO - thredds.server.opendap.NcDODSServlet - Request Completed
  // - 200 - -1 - 34
  //
  /*
   * 1 2009-03-10T16:08:54.617 -0600
   * 2 16621283
   * 3 162230
   * 4 INFO
   * 5 thredds.server.opendap.NcDODSServlet
   * 6 Request Completed - 200 - -1 - 47
   */

  private static final Pattern donePattern = Pattern.compile("^Request Completed - (.*) - (.*) - (.*)");
  static final Pattern startPattern = Pattern.compile("^Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");
  static final Pattern commonPattern =
      Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ [+-]\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - (.*)");

  /*
   * Difficult thing is to return the extra line assocated with the previous good log
   * We do this by not returning until we get a match on the next log. We have to rewind.
   */
  public LogReader.Log nextLog(BufferedReader dataIS) throws IOException {

    ServletLog log = new ServletLog();
    boolean haveLog = false;

    while (true) {
      dataIS.mark(20 * 1000); // track where we are
      String line = dataIS.readLine();
      if (line == null) {
        return haveLog ? log : null;
      }

      try {
        Matcher m = commonPattern.matcher(line);
        if (m.matches()) {
          if (haveLog) { // have a log, next one matches, proceed
            try {
              dataIS.reset();
              return log;

            } catch (Throwable t) {
              System.out.println("Cant reset " + line);
            }
          }
          haveLog = true; // next match will return the current log

          log.date = convertDate(m.group(1));
          log.reqTime = parseLong(m.group(2));
          log.reqSeq = parseLong(m.group(3));
          log.level = m.group(4).intern();
          log.where = m.group(5);

          String rest = m.group(6);
          if (rest.contains("Request Completed")) {
            int pos = rest.indexOf("Request Completed");
            Matcher m2 = donePattern.matcher(rest.substring(pos));
            if (m2.matches()) {
              log.returnCode = parse(m2.group(1));
              log.sizeBytes = parseLong(m2.group(2));
              log.msecs = parseLong(m2.group(3));
              log.isDone = true;

            } else {
              System.out.println("Cant parse donePattern= " + rest);
              System.out.println(" line= " + line);
              log.addExtra(rest);
            }

          } else if (rest.contains("Remote host")) {
            int pos = rest.indexOf("Remote host");
            Matcher m2 = startPattern.matcher(rest.substring(pos));
            if (m2.matches()) {
              log.ip = m2.group(1);
              log.verb = m2.group(2).intern();
              log.path = EscapeStrings.urlDecode(m2.group(3)); // old URLDecoder.decode(m2.group(3));
              if (m2.groupCount() > 4)
                log.http = m2.group(4).intern();
              log.isStart = true;

            } else {
              System.out.println("Cant parse startPattern= " + rest);
              System.out.println(" line= " + line);
              log.addExtra(rest);
            }

          } else { // a non-start, non-done log
            log.addExtra(rest);
          }

        } else { // the true extra line
          log.addExtra(line);
        }

      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Cant parse " + line);
        log.addExtra(line);
      }
    }

  }

  // 2010-04-21T13:05:22.006 -0600
  private SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  private long convertDate(String accessDateFormat) {
    // 30/Sep/2009:23:50:47 -0600
    try {
      Date d = df.parse(accessDateFormat);
      return d.getTime();
    } catch (Throwable t) {
      System.out.printf("Bad date format = %s err = %s%n", accessDateFormat, t.getMessage());
    }
    return -1;
  }


  private int parse(String s) {
    s = s.trim();
    if (s.isEmpty())
      return 0;
    if (s.equals("-"))
      return 0;
    return Integer.parseInt(s);
  }

  private long parseLong(String s) {
    s = s.trim();
    if (s.isEmpty())
      return 0;
    if (s.equals("-"))
      return 0;
    return Long.parseLong(s);
  }

}
