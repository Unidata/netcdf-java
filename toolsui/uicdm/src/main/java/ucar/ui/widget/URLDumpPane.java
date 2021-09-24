/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ucar.nc2.internal.http.HttpService;
import ucar.nc2.util.IO;
import ucar.ui.util.HttpUtils;
import ucar.unidata.util.Urlencoded;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.ComboBox;

/**
 * A text widget to dump a web URL.
 */
public class URLDumpPane extends TextHistoryPane {
  private static final int MAX_BODY = 50_000;

  private enum Library {
    HttpService, HttpURLConnection
  }

  private enum Command {
    GET, HEAD
  }

  private final ComboBox<String> cb;
  private final JComboBox<Library> implCB;
  private HttpURLConnection currentConnection;

  public URLDumpPane(PreferencesExt prefs) {
    super(true);

    // combo box holds a list of urls
    cb = new ComboBox<>(prefs);

    // holds Library impl enum
    implCB = new JComboBox<>();
    for (Library e : Library.values())
      implCB.addItem(e);

    JButton buttHead = new JButton("Head");
    buttHead.setToolTipText("Open URL connection, Headers only");
    buttHead.addActionListener(e -> {
      String urlString = (String) cb.getSelectedItem();
      process(urlString, Command.HEAD);
      gotoTop();
      cb.addItem(urlString);
    });


    JButton buttRead = new JButton("Get");
    buttRead.setToolTipText("Open URL connection, Get content");
    buttRead.addActionListener(e -> {
      String urlString = (String) cb.getSelectedItem();
      process(urlString, Command.GET);
      gotoTop();
      cb.addItem(urlString);
    });

    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    buttPanel.add(implCB);
    buttPanel.add(buttHead);
    buttPanel.add(buttRead);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(new JLabel("URL:"), BorderLayout.WEST);
    topPanel.add(cb, BorderLayout.CENTER);
    topPanel.add(buttPanel, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);
  }

  private void process(String urlString, Command cmd) {
    clear();

    Library impl = (Library) implCB.getSelectedItem();
    if (impl == Library.HttpService) {
      openWithUcarHttpservices(urlString, cmd);
    } else if (impl == Library.HttpURLConnection) {
      if (cmd == Command.GET)
        readURL(urlString);
      else
        openURL(urlString, cmd);
    }
  }

  ///////////////////////////////////////////////////////
  // Uses ucar.httpservices
  @Urlencoded
  private void openWithUcarHttpservices(String urlString, Command cmd) {

    try {
      HttpRequest request = HttpService.standardGetRequestBuilder(urlString).build();
      HttpResponse<String> response = HttpService.standardRequestForString(request);
      HttpHeaders responseHeaders = response.headers();

      appendLine("HttpRequest " + cmd + " " + urlString);
      printHeaders("Request Headers = ", request.headers());
      appendLine(" ");

      appendLine("Status Code = " + response.statusCode());
      printHeaders("Response Headers = ", responseHeaders);

      if (cmd == Command.GET) {
        appendLine("\nResponseBody---------------");
        String body = response.body();
        appendLine("  contents length =" + body.length());
        if (body.length() > MAX_BODY) {
          body = body.substring(0, MAX_BODY);
        }
        appendLine(body);
      }

    } catch (IOException e) {
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      appendLine(sw.toString());
    }
  }

  private void printHeaders(String title, HttpHeaders headers) {
    appendLine(title);
    headers.map().forEach((key, value) -> {
      appendLine(String.format("  %s: %s", key, value));
    });
  }

  private void printHeaders(String title, Collection<Map.Entry<String, String>> headers) {
    if (headers.isEmpty())
      return;
    appendLine(title);
    for (Map.Entry<String, String> entry : headers) {
      appendLine(String.format("  %s = %s", entry.getKey(), entry.getValue()));
    }
  }

  private void printSet(String title, Set<String> en) {
    appendLine(title);
    for (String s : en) {
      append("  " + s);
    }
    appendLine("");
  }

  ///////////////////////////////////////////////////////
  // Uses java.net

  private void openURL(String urlString, Command command) {
    try {
      // Open the URLConnection for reading
      URL u = new URL(urlString);
      currentConnection = (HttpURLConnection) u.openConnection();
      currentConnection.setRequestMethod(command.toString()); // GET or HEAD
      currentConnection.setAllowUserInteraction(true);

      clear();
      appendLine(command + " request for " + urlString);

      // request headers
      Map<String, List<String>> reqs = currentConnection.getRequestProperties();
      for (Map.Entry<String, List<String>> ent : reqs.entrySet()) {
        append(" " + ent.getKey() + ": ");
        for (String v : ent.getValue())
          append(v + " ");
        appendLine("");
      }
      appendLine("");
      appendLine("getFollowRedirects=" + HttpURLConnection.getFollowRedirects());
      appendLine("getInstanceFollowRedirects=" + currentConnection.getInstanceFollowRedirects());
      appendLine("AllowUserInteraction=" + currentConnection.getAllowUserInteraction());
      appendLine("");

      int code = currentConnection.getResponseCode();
      String response = currentConnection.getResponseMessage();

      // response headers
      appendLine(" HTTP/1.x " + code + " " + response);
      appendLine(" content-length: " + currentConnection.getContentLength());
      appendLine(" content-encoding: " + currentConnection.getContentEncoding());
      appendLine(" content-type: " + currentConnection.getContentType());
      appendLine("\nHeaders: ");

      for (int j = 1; true; j++) {
        String header = currentConnection.getHeaderField(j);
        String key = currentConnection.getHeaderFieldKey(j);
        if (header == null || key == null)
          break;
        appendLine(" " + key + ": " + header);
      }

      appendLine("");
      appendLine("contents:");

      // read it
      java.io.InputStream is = currentConnection.getInputStream();
      ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
      IO.copy(is, bout);
      is.close();

      append(new String(bout.toByteArray(), StandardCharsets.UTF_8));
      appendLine("end contents");

    } catch (MalformedURLException e) {
      append(urlString + " is not a parseable URL");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void readURL(String urlString) {
    try {
      // Open the URLConnection for reading
      URL u = new URL(urlString);
      currentConnection = (HttpURLConnection) u.openConnection();
      // uc.setAllowUserInteraction(true);

      clear();
      appendLine("GET request for " + urlString);

      // request headers
      Map<String, List<String>> reqs = currentConnection.getRequestProperties();
      for (Map.Entry<String, List<String>> ent : reqs.entrySet()) {
        append(" " + ent.getKey() + ": ");
        for (String v : ent.getValue())
          append(v + " ");
        appendLine("");
      }
      appendLine("");

      int code = currentConnection.getResponseCode();
      String response = currentConnection.getResponseMessage();

      // result headers
      appendLine(" HTTP/1.x " + code + " " + response);
      for (int j = 1; true; j++) {
        String header = currentConnection.getHeaderField(j);
        String key = currentConnection.getHeaderFieldKey(j);
        if (header == null || key == null)
          break;
        appendLine(" " + key + ": " + header);
      }

      appendLine("");

      // read it
      java.io.InputStream is = currentConnection.getInputStream();
      ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
      IO.copy(is, bout);
      is.close();

      append(new String(bout.toByteArray(), StandardCharsets.UTF_8));

    } catch (MalformedURLException e) {
      append(urlString + " is not a parseable URL");
    } catch (IOException e) {
      e.printStackTrace();
      appendLine(e.getMessage());
    }
  }

  public void setURL(String urlString) {
    if (urlString == null)
      return;
    cb.addItem(urlString);
    clear();
  }

  void putURL(String urlString) {
    if (urlString == null)
      return;
    String contents = ta.getText();
    HttpUtils.HttpResult result = HttpUtils.putToURL(urlString, contents);
    javax.swing.JOptionPane.showMessageDialog(this, "Status code= " + result.statusCode + "\n" + result.message);
  }

  public void save() {
    cb.save();
  }

  public void append(String text) {
    ta.append(text);
  }
}
