/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.logs;

import java.nio.charset.StandardCharsets;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Superclass to read TDS logs
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class LogReader {
  private static SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  public interface LogParser {
    Log nextLog(BufferedReader reader) throws IOException;
  }

  public static class Log {
    public String getIp() {
      return ip;
    }

    public long getDateMillisec(){
      return date;
    }

    public String getDate() {
      return df.format(new Date(date));
    }

    public String getReferrer() {
      return referrer;
    }

    public String getClient() {
      return client;
    }

    public int getStatus() {
      return returnCode;
    }

    public long getMsecs() {
      return msecs;
    }

    public long getBytes() {
      return sizeBytes;
    }

    public String getPath() {
      return (path == null) ? null : StringUtil2.unescape(path);
    }

    public long date;
    String verb, referrer, client;
    int returnCode;
    long msecs, sizeBytes;
    String ip, path, http;

    public String toCSV() {
      //return ip + "," + date + ",\"" + verb + "\","+ path + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
      return ip + "," + getDate() + "," + verb + ",\"" + getPath() + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
    }

    public String toString() {
      return ip + " [" + getDate() + "] " + verb + " " + getPath() + " " + http + " " + returnCode + " " + sizeBytes + " " + referrer + " " + client + " " + msecs;
    }

    public void toString(Formatter f) {
      f.format("path = %s%n", path);
      int pos = path.indexOf('?');
      if (pos > 0 ) {
        f.format("  path = %s%n", path.substring(0,pos));
        f.format("  query = %s%n", path.substring(pos+1));
      }
      f.format("%n");
      f.format("date = %s%n", getDate());
      f.format("verb = %s%n", verb);
      f.format("status = %d%n", returnCode);
      f.format("took = %d msecs%n", msecs);
      f.format("size = %d bytes%n", sizeBytes);
      f.format("from address = %s%n", ip);
      f.format("client = %s%n", client);
      f.format("referrer = %s%n", referrer);
    }


  }

  public interface Closure {
    void process(Log log) throws IOException;
  }


  public static class Stats {
    public long total;
    public long passed;
  }

  ////////////////////////////////////////////////////////////

  public interface LogFilter {
    boolean pass(Log log);
  }

  public static class DateFilter implements LogFilter {
    long start, end;
    LogReader.LogFilter chain;

    public DateFilter(long start, long end, LogReader.LogFilter chain) {
      this.start = start;
      this.end = end;
      this.chain = chain;
    }

    public boolean pass(LogReader.Log log) {
      if (chain != null && !chain.pass(log))
        return false;

      if ((log.date < start) || (log.date > end))
        return false;

      return true;
    }
  }

  public static class IpFilter implements LogFilter {
    String[] match;
    LogReader.LogFilter chain;

    public IpFilter(String[] match, LogReader.LogFilter chain) {
      this.match = match;
      this.chain = chain;
    }

    public boolean pass(LogReader.Log log) {
      if (chain != null && !chain.pass(log))
        return false;

      for (String s : match)
        if (log.getIp().startsWith(s))
          return false;

      return true;
    }
  }

  public static class ErrorOnlyFilter implements LogFilter {
    LogReader.LogFilter chain;

    public ErrorOnlyFilter(LogReader.LogFilter chain) {
      this.chain = chain;
    }

    public boolean pass(LogReader.Log log) {
      if (chain != null && !chain.pass(log))
        return false;

      int status = log.getStatus();
      if ((status < 400) || (status >= 1000)) return false;

      return true;
    }
  }

  public static class FilterNoop implements LogFilter {
    public boolean pass(LogReader.Log log) {
      return true;
    }
  }
  /////////////////////////////////////////////////////////////////////

  private int maxLines = -1;
  private LogParser parser;

  public LogReader(LogParser parser)  {
    this.parser = parser;
  }

  /**
   * Read all the files in a directory and process them. Files are sorted by filename.
   * @param dir read from this directory
   * @param ff files must pass this filter (may be null)
   * @param closure send each Log to this closure
   * @param logf filter out these Logs (may be null)
   * @param stat accumulate statistics (may be null)
   * @throws IOException on read error
   */
  public void readAll(File dir, FileFilter ff, Closure closure, LogFilter logf, Stats stat) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      System.out.printf("Dir has no files= %s%n", dir);
      return;
    }
    List<File> list = Arrays.asList(files);
    Collections.sort(list);

    for (File f : list) {
      if ((ff != null) && !ff.accept(f)) continue;
      if (f.isDirectory())
        readAll(f, ff, closure, logf, stat);
      else
        scanLogFile(f, closure, logf, stat);
    }
  }

  /**
   * Read a log file.
   * @param file file to read
   * @param closure send each Log to this closure
   * @param logf filter out these Logs (may be null)
   * @param stat accumulate statistics (may be null)
   * @throws IOException on read error
   */
  public void scanLogFile(File file, Closure closure, LogFilter logf, Stats stat) throws IOException {
    try (InputStream ios = new FileInputStream(file)) {
      System.out.printf("-----Reading %s %n", file.getPath());

      BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, StandardCharsets.UTF_8), 40 * 1000);
      int total = 0;
      int count = 0;
      while ((maxLines < 0) || (count < maxLines)) {
        Log log = parser.nextLog(dataIS);
        if (log == null) break;
        total++;

        if ((logf != null) && !logf.pass(log)) continue;

        closure.process(log);
        count++;
      }

      if (stat != null) {
        stat.total += total;
        stat.passed += count;
      }

      System.out.printf("----- %s total requests=%d passed=%d %n", file.getPath(), total, count);
    }
  }
}
