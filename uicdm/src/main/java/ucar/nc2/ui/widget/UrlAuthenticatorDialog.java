/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.widget;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import ucar.httpservices.HTTPAuthUtil;
import ucar.httpservices.HTTPCredentialsProvider;
import ucar.httpservices.HTTPSession;
import ucar.ui.prefs.Field;
import ucar.ui.prefs.PrefPanel;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import ucar.ui.widget.IndependentDialog;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This can be used both for java.net authentication:
 * java.net.Authenticator.setDefault(new thredds.ui.UrlAuthenticatorDialog(frame));
 * <p>
 * or for org.apache.http authentication:
 * httpclient.getParams().setParameter( CredentialsProvider.PROVIDER, new UrlAuthenticatorDialog( null));
 *
 * @author John Caron
 *         Modified 12/30/2015 by DMH to utilize an internal map so it
 *         can support multiple AuthScopes to handle e.g. proxies like
 *         BasicCredentialsProvider
 */

/*
 * 
 * UrlAuthenticatorDialog was repeatedly prompting for credentials
 * every time getCredentials() was called. Additionally, it was
 * locking up if a bad password was provided.
 * 
 * The repeated popup problem was occuring because
 * UrlAuthenticatorDialog had been modified at one time to cache
 * credentials that it generated so that repeated calls would
 * return the cached credentials. This caching is required by
 * Apache httpclient as of at least 4.5.x. It is used, for
 * example, to store proxy credentials. The canonical code example
 * is to look at the code for BasicCredendentialsProvider. In any
 * case, someone removed this code at some point. No idea why.
 * The caching has been restored.
 * 
 * With respect to the problem of a bad username+password,
 * it turns out that the
 * AuthCache object in the HttpContext object is notified when authorization
 * fails. The AuthCache.remove method is called with the HttpHost object
 * defining the host for which authorization failed. It turns out that
 * AuthCache does not, by default, notify the CredentialsProvider object
 * of the failure (a serious flaw IMO).
 * 
 * This has been fixed in HTTPSession by using a new class -- HTTPAuthCache --
 * to forward remove requests to a set of CredentialsProvider objects
 * using one of two methods:
 * 
 * 1. If the CredentialsProvider implements a new Interface --
 * HTTPCredentialsProvider -- then the remove() method of the
 * HTTPCredentialsProvider API is called. This provided the
 * ability to carry out a finer grain removal of bad
 * credentials.
 * 2. Otherwise, the clear() method of the CredentialsProvider is
 * called; this latter case is provide backward compatibility.
 * 
 * UrlAuthenticatorDialog has been modified to support
 * the HTTPCredentialsProvider.remove method.
 */

public class UrlAuthenticatorDialog extends Authenticator implements HTTPCredentialsProvider {

  private IndependentDialog dialog;
  private UsernamePasswordCredentials pwa = null;
  private Field.Text serverF, realmF, userF;
  private Field.Password passwF;
  private boolean debug;

  public HTTPSession session = null;

  // Track credentials stored via setCredentials
  private Map<AuthScope, Credentials> setcache = new ConcurrentHashMap<>();

  // Track credentials stored via dialog
  private Map<AuthScope, Credentials> dialogcache = new ConcurrentHashMap<>();

  /**
   * constructor
   *
   * @param parent JFrame
   */
  public UrlAuthenticatorDialog(javax.swing.JFrame parent) {
    PrefPanel pp = new PrefPanel("UrlAuthenticatorDialog", null);
    serverF = pp.addTextField("server", "Server", "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww");
    realmF = pp.addTextField("realm", "Realm", "");
    serverF.setEditable(false);
    realmF.setEditable(false);

    userF = pp.addTextField("user", "User", "");
    passwF = pp.addPasswordField("password", "Password", "");
    pp.addActionListener(e -> {
      char[] pw = passwF.getPassword();
      if (pw == null)
        return;
      pwa = new UsernamePasswordCredentials(userF.getText(), new String(pw));
      dialog.setVisible(false);
    });
    // button to dismiss
    JButton cancel = new JButton("Cancel");
    pp.addButton(cancel);
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        pwa = null;
        dialog.setVisible(false);
      }
    });
    pp.finish();

    dialog = new IndependentDialog(parent, true, "HTTP Authentication", pp);
    dialog.setLocationRelativeTo(parent);
    dialog.setLocation(100, 100);
  }

  public void clear() {
    this.dialogcache.clear();
  }

  // Extra method in HTTPCredentialProvider to provide finer grain
  // removal vs clear()
  public void remove(HttpHost host) {
    // Convert host to an AuthScope
    AuthScope hostscope = HTTPAuthUtil.hostToAuthScope(host);
    // Find match in dialogcache
    AuthScope match = HTTPAuthUtil.bestmatch(hostscope, dialogcache.keySet());
    // remove
    if (match != null)
      dialogcache.remove(match);
  }

  public void setCredentials(AuthScope scope, Credentials cred) {
    setcache.put(scope, cred);
  }

  // java.net calls this:
  protected PasswordAuthentication getPasswordAuthentication() {
    if (pwa == null)
      throw new IllegalStateException();

    if (debug) {
      System.out.println("site= " + getRequestingSite());
      System.out.println("port= " + getRequestingPort());
      System.out.println("protocol= " + getRequestingProtocol());
      System.out.println("prompt= " + getRequestingPrompt());
      System.out.println("scheme= " + getRequestingScheme());
    }

    serverF.setText(getRequestingHost() + ":" + getRequestingPort());
    realmF.setText(getRequestingPrompt());
    dialog.setVisible(true);

    if (debug) {
      System.out.println("user= (" + pwa.getUserName() + ")");
      System.out.println("password= (" + pwa.getPassword() + ")");
    }

    return new PasswordAuthentication(pwa.getUserName(), pwa.getPassword().toCharArray());
  }

  // http client calls this:
  public Credentials getCredentials(AuthScope scope) {
    // Search dialogcache first
    Credentials creds = null;
    AuthScope bestMatch = null;
    bestMatch = HTTPAuthUtil.bestmatch(scope, dialogcache.keySet());
    if (bestMatch != null) {
      creds = dialogcache.get(bestMatch);
    }
    if (creds == null) {
      // try the set cache
      bestMatch = HTTPAuthUtil.bestmatch(scope, setcache.keySet());
      if (bestMatch != null) {
        creds = setcache.get(bestMatch);
      }
    }
    if (creds != null)
      return creds;
    // No cached creds, so ask user
    serverF.setText(scope.getHost() + ":" + scope.getPort());
    realmF.setText(scope.getRealm());
    dialog.setVisible(true);
    if (pwa == null)
      throw new IllegalStateException();
    if (debug) {
      System.out.println("user= (" + pwa.getUserName() + ")");
      System.out.println("password= (" + pwa.getPassword() + ")");
    }
    // Is this really necessary?
    UsernamePasswordCredentials upc = new UsernamePasswordCredentials(pwa.getUserName(), pwa.getPassword());
    dialogcache.put(scope, upc);
    return upc;
  }
}

