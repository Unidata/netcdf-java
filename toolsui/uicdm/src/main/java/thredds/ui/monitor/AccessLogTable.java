/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ui.monitor;

import com.github.lgooddatepicker.components.DateTimePicker;
import java.time.ZoneId;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import thredds.client.catalog.TimeDuration;
import thredds.logs.AccessLogParser;
import thredds.logs.LogCategorizer;
import thredds.logs.LogReader;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Access Logs in TdsMonitor.
 *
 * @author caron
 * @since Mar 26, 2009
 */
public class AccessLogTable extends JPanel {
  private final PreferencesExt prefs;
  private final DnsLookup dnsLookup;

  private final BeanTable<LogReader.Log> logTable;
  private final BeanTable<User> userTable;
  private final BeanTable<Dataroot> datarootTable;
  private final BeanTable<Service> serviceTable;
  private final BeanTable<Client> clientTable;
  private final JPanel timeSeriesPanel;

  private ArrayList<LogReader.Log> completeLogs;
  private ArrayList<LogReader.Log> restrictLogs;

  private boolean calcUser = true;
  private boolean calcRoot = true;
  private boolean calcService = true;
  private boolean calcClient = true;

  private final JTabbedPane tabbedPanel;

  private final TextHistoryPane infoTA;
  private final IndependentWindow infoWindow;

  private final DateTimePicker dateTimePickerStart;
  private final DateTimePicker dateTimePickerEnd;

  public AccessLogTable(DateTimePicker dateTimePickerStart, DateTimePicker dateTimePickerEnd, PreferencesExt prefs,
      DnsLookup dnsLookup) {
    this.dateTimePickerStart = dateTimePickerStart;
    this.dateTimePickerEnd = dateTimePickerEnd;
    this.prefs = prefs;
    this.dnsLookup = dnsLookup;

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 100)));

    logTable = new BeanTable<>(LogReader.Log.class, (PreferencesExt) prefs.node("Logs"), false);
    logTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        LogReader.Log log = logTable.getSelectedBean();
        if (log == null)
          return;
        infoTA.setText(log.toString());
        infoWindow.show();
      }
    });
    PopupMenu varPopup =
        logTable.addPopupOption("Show Log", logTable.makeShowAction(infoTA, infoWindow, b -> b.toString()));
    varPopup.addAction("DNS Lookup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        LogReader.Log log = logTable.getSelectedBean();
        if (log == null)
          return;
        try {
          infoTA.setText(log.getIp() + " = " + dnsLookup.reverseDNS(log.getIp()));
        } catch (Exception ee) {
          infoTA.setTextFromStackTrace(ee);
        }
        infoWindow.show();
      }
    });
    varPopup.addAction("Resend URL", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        LogReader.Log log = logTable.getSelectedBean();
        if (log == null)
          return;
        String urlString = log.getPath();
        AccessLogTable.this.firePropertyChange("UrlDump", null, "http://" + manager.getServer() + urlString);
      }
    });
    varPopup.addAction("Remove selected logs", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<LogReader.Log> all = logTable.getBeans();
        List<LogReader.Log> selected = logTable.getSelectedBeans();
        all.removeAll(selected);
        logTable.setBeans(all);
      }
    });

    userTable = new BeanTable<>(User.class, (PreferencesExt) prefs.node("LogUser"), false);
    userTable.addListSelectionListener(e -> {
      User accum = userTable.getSelectedBean();
      if (accum == null)
        return;
      accum.run();
    });
    PopupMenu varPopupU = new PopupMenu(userTable.getJTable(), "Options");
    varPopupU.addAction("User requests", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Accum accum = userTable.getSelectedBean();
        if (accum == null)
          return;
        logTable.setBeans(accum.logs);
        tabbedPanel.setSelectedIndex(0);
      }
    });

    datarootTable = new BeanTable<>(Dataroot.class, (PreferencesExt) prefs.node("DataRoot"), false);
    datarootTable.addListSelectionListener(e -> {
      Dataroot accum = datarootTable.getSelectedBean();
      if (accum == null)
        return;
    });
    PopupMenu varPopupR = new PopupMenu(datarootTable.getJTable(), "Options");
    varPopupR.addAction("User requests", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Accum accum = datarootTable.getSelectedBean();
        if (accum == null)
          return;
        logTable.setBeans(accum.logs);
        tabbedPanel.setSelectedIndex(0);
      }
    });

    serviceTable = new BeanTable<>(Service.class, (PreferencesExt) prefs.node("Service"), false);
    serviceTable.addListSelectionListener(e -> {
      Service accum = serviceTable.getSelectedBean();
      if (accum == null)
        return;
    });
    ucar.ui.widget.PopupMenu varPopupS = new PopupMenu(serviceTable.getJTable(), "Options");
    varPopupS.addAction("User requests", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Accum accum = serviceTable.getSelectedBean();
        if (accum == null)
          return;
        logTable.setBeans(accum.logs);
        tabbedPanel.setSelectedIndex(0);
      }
    });

    clientTable = new BeanTable<>(Client.class, (PreferencesExt) prefs.node("Service"), false);
    clientTable.addListSelectionListener(e -> {
      Client accum = clientTable.getSelectedBean();
      if (accum == null)
        return;
    });
    ucar.ui.widget.PopupMenu varPopupC = new PopupMenu(clientTable.getJTable(), "Options");
    varPopupC.addAction("User requests", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Accum accum = clientTable.getSelectedBean();
        if (accum == null)
          return;
        logTable.setBeans(accum.logs);
        tabbedPanel.setSelectedIndex(0);
      }
    });

    timeSeriesPanel = new JPanel();
    timeSeriesPanel.setLayout(new BorderLayout());

    // tabbed panes
    tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
    tabbedPanel.addTab("LogTable", logTable);
    tabbedPanel.addTab("User", userTable);
    tabbedPanel.addTab("DataRoot", datarootTable);
    tabbedPanel.addTab("Service", serviceTable);
    tabbedPanel.addTab("Client", clientTable);
    tabbedPanel.addTab("TimeSeries", timeSeriesPanel);
    tabbedPanel.setSelectedIndex(0);

    tabbedPanel.addChangeListener(e -> {
      java.util.ArrayList<LogReader.Log> useBeans = (java.util.ArrayList<LogReader.Log>) logTable.getBeans();

      int idx = tabbedPanel.getSelectedIndex();
      String title = tabbedPanel.getTitleAt(idx);
      if (title.equals("User"))
        initUserLogs(useBeans);
      if (title.equals("DataRoot"))
        initDatarootLogs(useBeans);
      if (title.equals("Service"))
        initServiceLogs(useBeans);
      if (title.equals("Client"))
        initClientLogs(useBeans);
      if (title.equals("TimeSeries"))
        showTimeSeriesAll(useBeans);
    });

    setLayout(new BorderLayout());
    add(tabbedPanel, BorderLayout.CENTER);
  }

  public void exit() {
    if (executor != null)
      executor.shutdownNow();

    logTable.saveState(false);
    userTable.saveState(false);
    datarootTable.saveState(false);
    serviceTable.saveState(false);
    clientTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  private LogLocalManager manager;
  private java.util.List<LogLocalManager.FileDateRange> accessLogFiles;

  public void setLocalManager(LogLocalManager manager) {
    this.manager = manager;

    LogCategorizer.setRoots(manager.getRoots());
  }

  void showLogs(LogReader.LogFilter filter) {
    Date start = null, end = null;
    try {
      start = Date.from(dateTimePickerStart.getDateTimeStrict().atZone(ZoneId.systemDefault()).toInstant());
      end = Date.from(dateTimePickerEnd.getDateTimeStrict().atZone(ZoneId.systemDefault()).toInstant());
      accessLogFiles = manager.getLocalFiles(start, end);
    } catch (Exception e) {
      e.printStackTrace();
      accessLogFiles = manager.getLocalFiles(null, null);
    }

    LogReader reader = new LogReader(new AccessLogParser());
    completeLogs = new ArrayList<>(30000);

    if ((start != null) && (end != null))
      filter = new LogReader.DateFilter(start.getTime(), end.getTime(), filter);

    try {
      long startElapsed = System.nanoTime();
      LogReader.Stats stats = new LogReader.Stats();

      for (LogLocalManager.FileDateRange fdr : accessLogFiles)
        reader.scanLogFile(fdr.f, new MyClosure(completeLogs), filter, stats);

      long elapsedTime = System.nanoTime() - startElapsed;
      System.out.printf(" setLogFile total= %d passed=%d%n", stats.total, stats.passed);
      System.out.printf(" elapsed=%f msecs %n", elapsedTime / (1000 * 1000.0));

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }

    resetLogs();
  }

  void showInfo(Formatter f) {
    f.format(" Current time =   %s%n%n", new Date().toString());

    int n = 0;
    if (completeLogs != null) {
      n = completeLogs.size();
      f.format("Complete logs n=%d%n", n);
      f.format("  first log date= %s%n", completeLogs.get(0).getDate());
      f.format("   last log date= %s%n", completeLogs.get(n - 1).getDate());
    }
    List<LogReader.Log> restrict = logTable.getBeans();
    if (restrict != null && (restrict.size() != n)) {
      f.format("%nRestricted logs n=%d%n", restrict.size());
    }
    if (accessLogFiles != null) {
      f.format("%nFiles used%n");
      for (LogLocalManager.FileDateRange fdr : accessLogFiles) {
        f.format(" %s [%s,%s]%n", fdr.f.getName(), fdr.start, fdr.end);
      }
    }
  }

  void resetLogs() {
    logTable.setBeans(completeLogs);
    tabbedPanel.setSelectedIndex(0);

    userTable.setBeans(new ArrayList<>());
    datarootTable.setBeans(new ArrayList<>());
    calcUser = true;
    calcRoot = true;
    calcService = true;
    calcClient = true;
    restrictLogs = completeLogs;

    /*
     * int n = completeLogs.size();
     * if (n > 0) {
     * startDateField.setText(completeLogs.get(0).getDate());
     * endDateField.setText(completeLogs.get(n-1).getDate());
     * }
     */
  }

  void restrictLogs(String restrict) {
    restrictLogs = new ArrayList<>(1000);
    for (LogReader.Log log : completeLogs) {
      String ip = log.getIp();
      if (ip.startsWith(restrict))
        continue;
      restrictLogs.add(log);
    }

    logTable.setBeans(restrictLogs);
    tabbedPanel.setSelectedIndex(0);

    userTable.setBeans(new ArrayList<>());
    datarootTable.setBeans(new ArrayList<>());
    calcUser = true;
    calcRoot = true;
    calcService = true;
    calcClient = true;
  }

  ////////////////////////////////////////////////////////

  static class MyClosure implements LogReader.Closure {
    ArrayList<LogReader.Log> logs;

    MyClosure(ArrayList<LogReader.Log> logs) {
      this.logs = logs;
    }

    public void process(LogReader.Log log) {
      logs.add(log);
    }
  }

  ////////////////////////////////////////////////

  public static class Accum {
    public String getName() {
      return name;
    }

    public long getMsecs() {
      return msecs;
    }

    public long getMsecsPerRequest() {
      return msecs / count;
    }

    public long getKbytes() {
      return bytes / 1000;
    }

    public int getCount() {
      return count;
    }

    ArrayList<LogReader.Log> logs = new ArrayList<>(100);
    String name;
    long msecs;
    long bytes;
    int count;

    public Accum() {}

    Accum(String name) {
      this.name = name;
    }

    void add(LogReader.Log log) {
      logs.add(log);
      count++;
      bytes += log.getBytes();
      msecs += log.getMsecs();
    }
  }


  public class User extends Accum implements Runnable {
    String ip, namer;

    public String getIp() {
      return ip;
    }

    public String getNameReverse() {
      if (name != null && namer == null) {
        StringBuilder sbuff = new StringBuilder();
        String[] p = name.split("\\.");
        for (int i = p.length - 1; i >= 0; i--) {
          sbuff.append(p[i]);
          if (i != 0)
            sbuff.append('.');
        }
        namer = sbuff.toString();
      }
      return namer;
    }

    public User() {}

    User(String ip) {
      this.ip = ip;
    }

    public void run() {
      if (name != null)
        return;
      try {
        long startElapsed = System.nanoTime();
        name = dnsLookup.reverseDNS(ip);
        long elapsedTime = System.nanoTime() - startElapsed;
        if (showDNStime)
          System.out.printf(" reverseDNS took=%f msecs %n", elapsedTime / (1000 * 1000.0));
      } catch (Throwable e) {
        name = e.getMessage();
      }
    }
  }

  public static class Client extends Accum {
    public Client() {}

    Client(String client) {
      super(client);
    }
  }

  void initClientLogs(ArrayList<LogReader.Log> logs) {
    if (!calcClient)
      return;
    if (logs == null)
      return;

    HashMap<String, Client> map = new HashMap<>();

    for (LogReader.Log log : logs) {
      String clientName = log.getClient();
      if (clientName == null)
        clientName = "";
      Client accum = map.get(clientName);
      if (accum == null) {
        accum = new Client(clientName);
        map.put(clientName, accum);
      }
      accum.add(log);
    }

    clientTable.setBeans(new ArrayList<>(map.values()));
    calcClient = false;
  }

  private boolean showDNStime = false;

  void initUserLogs(ArrayList<LogReader.Log> logs) {
    if (!calcUser)
      return;
    if (logs == null)
      return;

    HashMap<String, User> map = new HashMap<>();

    for (LogReader.Log log : logs) {
      User accum = map.get(log.getIp());
      if (accum == null) {
        accum = new User(log.getIp());
        map.put(log.getIp(), accum);
      }
      accum.add(log);
    }

    userTable.setBeans(new ArrayList<>(map.values()));
    calcUser = false;
  }

  private ExecutorService executor;

  void showDNS() {

    if (null == executor)
      executor = Executors.newFixedThreadPool(3); // number of threads
    ArrayList<User> accums = (ArrayList<User>) userTable.getBeans();
    for (User a : accums) {
      executor.execute(a);
    }

    /*
     * boolean ok = false;
     * try {
     * ok = executor.awaitTermination(3, TimeUnit.SECONDS);
     * } catch (InterruptedException e) {
     * e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
     * }
     * long elapsedTime = System.nanoTime() - startElapsed;
     * System.out.printf(" reverseDNS took=%f msecs ok=%s %n", elapsedTime / (1000 * 1000.0), ok);
     */
  }

  ////////////////////////////////////////////////

  public static class Dataroot extends Accum {
    public Dataroot() {}

    Dataroot(String name) {
      super(name);
    }
  }

  void initDatarootLogs(ArrayList<LogReader.Log> logs) {
    if (!calcRoot)
      return;
    if (logs == null)
      return;

    HashMap<String, Dataroot> map = new HashMap<>();

    for (LogReader.Log log : logs) {
      String path = log.getPath();
      if (path == null)
        continue;
      String dataRoot = LogCategorizer.getDataroot(path, log.getStatus());
      Dataroot accum = map.get(dataRoot);
      if (accum == null) {
        accum = new Dataroot(dataRoot);
        map.put(dataRoot, accum);
      }
      accum.add(log);
    }

    datarootTable.setBeans(new ArrayList<>(map.values()));
    calcRoot = false;
  }

  ////////////////////////////////////////////////

  public static class Service extends Accum {
    public Service() {}

    Service(String name) {
      super(name);
    }
  }

  void initServiceLogs(ArrayList<LogReader.Log> logs) {
    if (!calcService)
      return;
    if (logs == null)
      return;

    HashMap<String, Service> map = new HashMap<>();

    for (LogReader.Log log : logs) {
      String path = log.getPath();
      if (path == null) {
        System.out.printf("FAIL %s%n", log);
        continue;
      }
      String service = LogCategorizer.getService(path);
      Service accum = map.get(service);
      if (accum == null) {
        accum = new Service(service);
        map.put(service, accum);
      }
      accum.add(log);
    }

    serviceTable.setBeans(new ArrayList<>(map.values()));
    calcService = false;
  }

  //////////////////

  // construct the TImeSeries plot for the list of logs passed in

  private void showTimeSeriesAll(java.util.List<LogReader.Log> logs) {
    TimeSeries bytesSentData = new TimeSeries("Bytes Sent", Minute.class);
    TimeSeries timeTookData = new TimeSeries("Average Latency", Minute.class);
    TimeSeries nreqData = new TimeSeries("Number of Requests", Minute.class);

    String intervalS = "5 minute"; // interval.getText().trim();
    // if (intervalS.length() == 0) intervalS = "5 minute";
    long period = 1000 * 60 * 5;
    try {
      TimeDuration tu = TimeDuration.parse(intervalS);
      period = (long) (1000 * tu.getValueInSeconds());
    } catch (Exception e) {
      System.out.printf("Illegal Time interval=%s %n", intervalS);
    }

    long current = 0;
    long bytes = 0;
    long timeTook = 0;
    long total_count = 0;
    long count = 0;
    for (LogReader.Log log : logs) {
      long msecs = log.date;
      if (msecs - current > period) {
        if (current > 0) {
          total_count += count;
          addPoint(bytesSentData, timeTookData, nreqData, new Date(current), bytes, count, timeTook);
        }
        bytes = 0;
        count = 0;
        timeTook = 0;
        current = msecs;
      }
      bytes += log.getBytes();
      timeTook += log.getMsecs();
      count++;
    }
    if (count > 0)
      addPoint(bytesSentData, timeTookData, nreqData, new Date(current), bytes, count, timeTook);
    total_count += count;
    System.out.printf("showTimeSeriesAll: total_count = %d logs = %d%n", total_count, logs.size());

    MultipleAxisChart mc = new MultipleAxisChart("Access Logs", intervalS + " window", "Mbytes Sent", bytesSentData);
    mc.addSeries("Number of Requests", nreqData);
    mc.addSeries("Average Latency (secs)", timeTookData);
    mc.finish(new java.awt.Dimension(1000, 1000));

    // MultipleAxisChart mc = new MultipleAxisChart("Bytes Sent", "5 min average", "Mbytes/sec", bytesSentData);
    // Chart c2 = new Chart("Average Latency", "5 min average", "Millisecs", timeTookData);
    // Chart c3 = new Chart("Number of Requests/sec", "5 min average", "", nreqData);

    timeSeriesPanel.removeAll();
    timeSeriesPanel.add(mc);
  }

  void addPoint(TimeSeries bytesSentData, TimeSeries timeTookData, TimeSeries nreqData, Date date, long bytes,
      long count, long timeTook) {

    bytesSentData.add(new Minute(date), bytes / 1000. / 1000.);
    double latency = (double) timeTook / count / 1000.;
    // timeTookData.add(new Minute(date), (latency > 10*1000) ? 0 : latency); // note latency limited to 10 secs.
    timeTookData.add(new Minute(date), latency);
    nreqData.add(new Minute(date), (double) count);
  }

}
