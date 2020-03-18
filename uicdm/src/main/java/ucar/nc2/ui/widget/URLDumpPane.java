/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.widget;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.http.entity.StringEntity;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.httpservices.HttpNameValue;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.IO;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.util.Urlencoded;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.ComboBox;

/**
 * A text widget to dump a web URL.
 *
 * @author John Caron
 * @see UrlAuthenticatorDialog
 */

public class URLDumpPane extends TextHistoryPane {
  private enum Library {
    UcarHttpservices, java
  }

  private enum Command {
    GET, PUT, HEAD, OPTIONS
  }

  private ComboBox<String> cb;
  private JComboBox<Library> implCB;
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

    JButton buttOpt = new JButton("Options");
    buttOpt.setToolTipText("Server options using HttpClient");
    buttOpt.addActionListener(e -> {
      String urlString = (String) cb.getSelectedItem();
      process(urlString, Command.OPTIONS);
      gotoTop();
      cb.addItem(urlString);
    });


    JButton buttPut = new JButton("Put");
    buttPut.setToolTipText("Put using HttpClient");
    buttPut.addActionListener(e -> {
      String urlString = (String) cb.getSelectedItem();
      process(urlString, Command.PUT);
      gotoTop();
      cb.addItem(urlString);
    });

    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    buttPanel.add(implCB);
    buttPanel.add(buttHead);
    buttPanel.add(buttRead);
    buttPanel.add(buttOpt);
    buttPanel.add(buttPut);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(new JLabel("URL:"), BorderLayout.WEST);
    topPanel.add(cb, BorderLayout.CENTER);
    topPanel.add(buttPanel, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);
  }

  private void process(String urlString, Command cmd) {
    clear();

    Library impl = (Library) implCB.getSelectedItem();
    if (impl == Library.UcarHttpservices) {
      openWithUcarHttpservices(urlString, cmd);
    } else if (impl == Library.java) {
      if (cmd == Command.GET)
        readURL(urlString);
      else if (cmd == Command.PUT)
        putURL(urlString);
      else
        openURL(urlString, cmd);
    }
  }

  ///////////////////////////////////////////////////////
  // Uses ucar.httpservices
  @Urlencoded
  private void openWithUcarHttpservices(String urlString, Command cmd) {
    HTTPMethod m;

    try (HTTPSession httpSession = HTTPFactory.newSession(urlString)) {
      /*
       * you might think this works, but it doesnt:
       * URI raw = new URI(urlString.trim());
       * appendLine("raw scheme= " + raw.getScheme() + "\n auth= " + raw.getRawAuthority() + "\n path= " +
       * raw.getRawPath() +
       * "\n query= " + raw.getRawQuery() + "\n fragment= " + raw.getRawFragment()+"\n");
       * 
       * URI url = new URI(raw.getScheme(), raw.getRawAuthority(),
       * URIUtil.encodePath(raw.getRawPath()),
       * URIUtil.encodeQuery(raw.getRawQuery()),
       * raw.getRawFragment());
       * appendLine("encoded scheme= " + url.getScheme() + "\n auth= " + url.getAuthority() + "\n path= " +
       * url.getPath() +
       * "\n query= " + url.getQuery() + "\n fragment= " + url.getFragment()+"\n");
       * urlString = url.toString();
       */

      // urlString = URLnaming.escapeQuery(urlString);
      if (cmd == Command.GET)
        m = HTTPFactory.Get(httpSession, urlString);
      else if (cmd == Command.HEAD)
        m = HTTPFactory.Head(httpSession, urlString);
      else if (cmd == Command.OPTIONS)
        m = HTTPFactory.Options(httpSession, urlString);
      else if (cmd == Command.PUT) {
        m = HTTPFactory.Put(httpSession, urlString);
        m.setRequestContent(new StringEntity(ta.getText()));
      } else {
        throw new RuntimeException("Unsupported command: " + cmd);
      }

      m.setCompression("gzip,deflate");

      appendLine("HTTPMethod " + m.getMethodKind() + " " + urlString);
      httpSession.getMergedSettings().forEach((k, v) -> appendLine(String.format("    %s = %s", k, v)));
      appendLine(" ");

      // TODO not handling chunked content, only gets first chunk.
      m.execute();

      printHeaders("Request Headers = ", m.getRequestHeaderValues());
      appendLine(" ");

      appendLine("Status Line = " + m.getStatusLine());
      printHeaders("Response Headers = ", m.getResponseHeaderValues());
      if (cmd == Command.GET) {
        appendLine("\nResponseBody---------------");

        String charset = m.getResponseCharSet();
        if (charset == null)
          charset = CDM.UTF8;

        // check for deflate and gzip compression
        Optional<String> encodingOpt = m.getResponseHeaderValue("content-encoding");
        if (encodingOpt.isPresent()) {
          String contents = null;
          String encoding = encodingOpt.get();

          if ("deflate".equals(encoding)) {
            byte[] body = m.getResponseAsBytes();
            if (body != null) {
              InputStream is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(body)), 10000);
              contents = IO.readContents(is, charset);
              double ratio = (double) contents.length() / body.length;
              appendLine("  deflate encoded=" + body.length + " decoded=" + contents.length() + " ratio= " + ratio);
            }

          } else if ("gzip".equals(encoding)) {
            byte[] body = m.getResponseAsBytes();
            if (body != null) {
              InputStream is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(body)), 10000);
              contents = IO.readContents(is, charset);
              double ratio = (double) contents.length() / body.length;
              appendLine("  gzip encoded=" + body.length + " decoded=" + contents.length() + " ratio= " + ratio);
            }

          } else {
            byte[] body = m.getResponseAsBytes(50 * 1000); // max 50 Kbytes
            contents = (body == null) ? "" : new String(body, charset);
          }
          if (contents != null) {
            if (contents.length() > 50 * 1000) // limit contents
              contents = contents.substring(0, 50 * 1000);
            appendLine(contents);
          }
        } else {
          byte[] body = m.getResponseAsBytes();
          appendLine("  contents length =" + body.length);
        }

      } else if (cmd == Command.OPTIONS) {
        printSet("AllowedMethods = ", HTTPFactory.getAllowedMethods());
      }

    } catch (IOException e) {
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      appendLine(sw.toString());
    }
  }

  private void printHeaders(String title, ImmutableSortedMap<String, ImmutableList<HttpNameValue>> headers) {
    appendLine(title);
    headers.forEach((key, value) -> {
      Formatter f = new Formatter();
      value.forEach(nv -> {
        if (nv.getValue() == null) {
          f.format("%s ", nv.getName());
        } else {
          f.format("%s=%s ", nv.getName(), nv.getValue());
        }
      });
      appendLine(String.format("  %s: %s", key, f.toString()));
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
    IO.HttpResult result = IO.putToURL(urlString, contents);
    javax.swing.JOptionPane.showMessageDialog(this, "Status code= " + result.statusCode + "\n" + result.message);
  }

  public void save() {
    cb.save();
  }

  public void append(String text) {
    ta.append(text);
  }
}
