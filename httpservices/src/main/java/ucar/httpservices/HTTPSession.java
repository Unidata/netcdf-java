/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

import com.google.common.collect.ImmutableMap;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import javax.annotation.concurrent.ThreadSafe;
import javax.print.attribute.UnmodifiableSetException;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

/**
 * A session is encapsulated in an instance of the class HTTPSession.
 * The encapsulation is with respect to a specific HttpHost "realm",
 * where the important part is is host+port. This means that once a
 * session is specified, it is tied permanently to that realm.
 * <p>
 * A Session encapsulate a number of other objects:
 * <ul>
 * <li>An instance of an Apache HttpClient.
 * <li>A http session id
 * <li>A RequestContext object; this also includes authentication:
 * specifically a credentials provider.
 * </ul>
 * <p>
 * As a rule, if the client gives an HTTPSession object to the "create method"
 * procedures of HTTPFactory (e.g. HTTPFactory.Get or HTTPFactory.Post)
 * then that creation call must specify a url that is "compatible" with the
 * scope of the session. The method url is <it>compatible</i> if its
 * host+port is the same as the session's host+port (=scope) and its scheme is
 * compatible, where e.g. http is compatible with https
 * (see HTTPAuthUtil.httphostCompatible)
 * <p>
 * If the HTTPFactory method creation call does not specify a session
 * object, then one is created (and destroyed) behind the scenes
 * along with the method.
 * <p>
 * Note that the term legalurl in the following code means that the url has
 * reserved characters within identifiers in escaped form. This is
 * particularly an issue for queries. Especially square brackets
 * (e.g. ?x[0:5]) are an issue. Recently (2018) Apache Tomcat stopped
 * accepting square brackets (and maybe other characters) as ok
 * when left unencoded. So, now we need to be aware of this
 * and cause queries encoding to include square brackets.
 * <p>
 * As of the move to Apache Httpclient 4.4 and later, the underlying
 * HttpClient objects are generally immutable. This means that at least
 * this class (HTTPSession) and the HTTPMethod class must store the
 * relevant info and create the HttpClient and HttpMethod objects
 * dynamically. This also means that when a parameter is changed (Agent,
 * for example), any existing cached HttpClient must be thrown away and
 * reconstructed using the change. As a rule, the HttpClient object will be
 * created at the last minute so that multiple parameter changes can be
 * effected without have to re-create the HttpClient for each parameter
 * change. Also note that the immutable objects will be cached and reused
 * if no parameters are changed.
 * <p>
 * <em>Authorization</em>
 * We assume that the session supports two CredentialsProvider instances:
 * one global to all HTTPSession objects and one specific to each
 * HTTPSession object. The global one is used unless a local one was specified.
 * <p>
 * As an aside, authentication is a bit tricky because some
 * authorization schemes use redirection. That is, the initial request
 * is made to server X, but X says: goto to server Y" to get, say, and
 * authorization token. Then Y says: return to X with this token and
 * proceed.
 * <p>
 * <em>SSL</em>
 * TBD.
 *
 * Notes:
 * 
 * 1. Setting of credentials via HTTPSession is no longer supported.
 * Instead, the user must use CredentialsProvider.setCredentials
 * 2. CredentialsProviders are all assumed to be caching (see
 * the code for BasicCredentialsProvider for the canonical case.
 * This has consequences for proxy because an attempt
 * will be made to insert them into the chosen CredentialsProvider.
 * 3. The new class HTTPAuthCache is used instead of BasicAuthCache
 * so that when authorization fails, the AuthCache.remove method
 * is forwarded to the CredentialsProvider.clear or the
 * HTTPCredentialsProvider.remove method.
 */

@ThreadSafe
public class HTTPSession implements Closeable {
  //////////////////////////////////////////////////
  // Constants

  // only used when testing flag is set
  public static boolean TESTING = false; // set to true during testing, should be false otherwise

  /**
   * Determine wether to use a Pooling connection manager
   * or to manage a bunch of individual connections.
   */
  protected static final boolean USEPOOL = true;

  // Define all the legal properties
  // Previously taken from class AllClientPNames, but that is now
  // deprecated, so just use an enum

  /* package */ enum Prop {
    ALLOW_CIRCULAR_REDIRECTS, HANDLE_REDIRECTS, HANDLE_AUTHENTICATION, MAX_REDIRECTS, MAX_CONNECTIONS, SO_TIMEOUT, CONN_TIMEOUT, CONN_REQ_TIMEOUT, USER_AGENT, COOKIE_STORE, RETRIES, UNAVAILRETRIES, COMPRESSION, CREDENTIALS, USESESSIONS,
  }

  // Header names
  // from: http://en.wikipedia.org/wiki/List_of_HTTP_header_fields
  public static final String HEADER_USERAGENT = "User-Agent";
  public static final String ACCEPT_ENCODING = "Accept-Encoding";

  static final int DFALTREDIRECTS = 25;
  static final int DFALTCONNTIMEOUT = 1 * 60 * 1000; // 1 minutes (60000 milliseconds)
  static final int DFALTCONNREQTIMEOUT = DFALTCONNTIMEOUT;
  static final int DFALTSOTIMEOUT = 5 * 60 * 1000; // 5 minutes (300000 milliseconds)

  static final int DFALTMAXCONNS = 20;
  static final int DFALTRETRIES = 1;
  static final int DFALTUNAVAILRETRIES = 1;
  static final int DFALTUNAVAILINTERVAL = 3000; // 3 seconds
  static final String DFALTUSERAGENT = "/NetcdfJava/HttpClient4.4";

  static final String[] KNOWNCOMPRESSORS = {"gzip", "deflate"};

  // Define -Dflags for various properties
  static final String DCONNTIMEOUT = "tds.http.conntimeout";
  static final String DSOTIMEOUT = "tds.http.sotimeout";
  static final String DMAXCONNS = "tds.http.maxconns";

  //////////////////////////////////////////////////////////////////////////
  // Type Declaration(s)

  // Define property keys for selected authorization related properties
  /* package */ enum AuthProp {
    KEYSTORE, KEYPASSWORD, TRUSTSTORE, TRUSTPASSWORD, SSLFACTORY, HTTPPROXY, HTTPSPROXY, PROXYUSER, PROXYPWD,
  }

  // Capture the value of various properties
  // Control read-only state; primarily for debugging

  static class AuthControls extends ConcurrentHashMap<AuthProp, Object> {
    protected boolean readonly = false;

    public AuthControls() {
      super();
    }

    public AuthControls setReadOnly(boolean tf) {
      this.readonly = tf;
      return this;
    }

    // Override to make map (but not contents) read only
    public Set<Map.Entry<AuthProp, Object>> entrySet() {
      throw new UnsupportedOperationException();
    }

    public Object put(AuthProp key, Object val) {
      if (readonly)
        throw new UnmodifiableSetException();
      if (val != null)
        super.put(key, val);
      return val;
    }

    public Object replace(AuthProp key, Object val) {
      if (readonly)
        throw new UnmodifiableSetException();
      return super.replace(key, val);
    }

    public void clear() {
      if (readonly)
        throw new UnmodifiableSetException();
      super.clear();
    }

  }


  // Support loose certificate acceptance
  static class LooseTrustStrategy extends TrustSelfSignedStrategy {
    @Override
    public boolean isTrusted(final X509Certificate[] chain, String authType) throws CertificateException {
      try {
        if (super.isTrusted(chain, authType))
          return true;
        // check expiration dates
        for (X509Certificate x5 : chain) {
          try {
            x5.checkValidity();
          } catch (CertificateExpiredException | CertificateNotYetValidException ce) {
            return true;
          }
        }
      } catch (CertificateException e) {
        return true; // temporary
      }
      return false;
    }
  }

  // For communication between HTTPSession.execute and HTTPMethod.execute.
  /*
   * static package class ExecState
   * {
   * public HttpRequestBase request = null;
   * public HttpResponse response = null;
   * }
   */
  public enum Methods {
    Get("get"), Head("head"), Put("put"), Post("post"), Options("options");
    private final String name;

    Methods(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  static class GZIPResponseInterceptor implements HttpResponseInterceptor {
    public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        Header ceheader = entity.getContentEncoding();
        if (ceheader != null) {
          HeaderElement[] codecs = ceheader.getElements();
          for (HeaderElement h : codecs) {
            if (h.getName().equalsIgnoreCase("gzip")) {
              response.setEntity(new GzipDecompressingEntity(response.getEntity()));
              return;
            }
          }
        }
      }
    }
  }

  static class DeflateResponseInterceptor implements HttpResponseInterceptor {
    public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        Header ceheader = entity.getContentEncoding();
        if (ceheader != null) {
          HeaderElement[] codecs = ceheader.getElements();
          for (HeaderElement h : codecs) {
            if (h.getName().equalsIgnoreCase("deflate")) {
              response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
              return;
            }
          }
        }
      }
    }
  }

  static class ZipStreamFactory implements InputStreamFactory {
    // InputStreamFactory methods
    @Override
    public InputStream create(InputStream instream) throws IOException {
      return new ZipInputStream(instream, HTTPUtil.UTF8);
    }
  }

  static class GZIPStreamFactory implements InputStreamFactory {
    // InputStreamFactory methods
    @Override
    public InputStream create(InputStream instream) throws IOException {
      return new GZIPInputStream(instream);
    }
  }

  ////////////////////////////////////////////////////////////////////////
  // Static variables

  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HTTPSession.class);

  // Define a settings object to hold all the
  // settable values; there will be one
  // instance for global and one for local.

  static Map<Prop, Object> globalsettings;

  static HttpRequestRetryHandler globalretryhandler = null;

  // Define a single global (default) credentials provider.
  // User is responsible for its contents via setCredentials
  static CredentialsProvider globalprovider = null;

  // Define interceptor instances; use copy on write for thread safety
  static List<HttpRequestInterceptor> reqintercepts = new CopyOnWriteArrayList<HttpRequestInterceptor>();
  static List<HttpResponseInterceptor> rspintercepts = new CopyOnWriteArrayList<HttpResponseInterceptor>();

  // This is a hack to suppress content-encoding headers from request
  // Effectively final because its set in the static initializer and otherwise
  // read only.
  protected static HttpResponseInterceptor CEKILL;

  // Debug Header interceptors
  protected static List<HttpRequestInterceptor> dbgreq = new CopyOnWriteArrayList<>();
  protected static List<HttpResponseInterceptor> dbgrsp = new CopyOnWriteArrayList<>();

  protected static HTTPConnections connmgr;

  protected static Map<String, InputStreamFactory> contentDecoderMap;

  // public final HttpClientBuilder setContentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap)

  // As taken from the command line
  protected static AuthControls authcontrols;

  static { // watch out: order is important for these initializers
    if (USEPOOL)
      connmgr = new HTTPConnectionPool();
    else
      connmgr = new HTTPConnectionSimple();
    CEKILL = new HTTPUtil.ContentEncodingInterceptor();
    contentDecoderMap = new HashMap<String, InputStreamFactory>();
    contentDecoderMap.put("zip", new ZipStreamFactory());
    contentDecoderMap.put("gzip", new GZIPStreamFactory());
    globalsettings = new ConcurrentHashMap<Prop, Object>();
    setDefaults(globalsettings);
    authcontrols = new AuthControls();
    authcontrols.setReadOnly(false);
    buildproxy(authcontrols);
    buildkeystores(authcontrols);
    buildsslfactory(authcontrols);
    authcontrols.setReadOnly(true);
    processDFlags(); // Other than the auth flags
    connmgr.addProtocol("https", (ConnectionSocketFactory) authcontrols.get(AuthProp.SSLFACTORY));
  }

  protected static int getDPropInt(String key) {
    String p = System.getProperty(key);
    if (p == null)
      return -1;
    try {
      int i = Integer.parseInt(p);
      return i;
    } catch (NumberFormatException nfe) {
      return -1;
    }
  }

  //////////////////////////////////////////////////////////////////////////
  // Static Initialization

  // Provide defaults for a settings map
  protected static synchronized void setDefaults(Map<Prop, Object> props) {
    if (false) {// turn off for now
      props.put(Prop.HANDLE_AUTHENTICATION, Boolean.TRUE);
    }
    props.put(Prop.HANDLE_REDIRECTS, Boolean.TRUE);
    props.put(Prop.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
    props.put(Prop.MAX_REDIRECTS, (Integer) DFALTREDIRECTS);
    props.put(Prop.SO_TIMEOUT, (Integer) DFALTSOTIMEOUT);
    props.put(Prop.CONN_TIMEOUT, (Integer) DFALTCONNTIMEOUT);
    props.put(Prop.CONN_REQ_TIMEOUT, (Integer) DFALTCONNREQTIMEOUT);
    props.put(Prop.USER_AGENT, DFALTUSERAGENT);
  }

  static void buildsslfactory(AuthControls authcontrols) {
    KeyStore keystore = (KeyStore) authcontrols.get(AuthProp.KEYSTORE);
    String keypass = (String) authcontrols.get(AuthProp.KEYPASSWORD);
    KeyStore truststore = (KeyStore) authcontrols.get(AuthProp.TRUSTSTORE);
    buildsslfactory(authcontrols, truststore, keystore, keypass);
  }

  static void buildkeystores(AuthControls authcontrols) {
    // SSL flags
    String keypath = cleanproperty("keystore");
    String keypassword = cleanproperty("keystorepassword");
    String trustpath = cleanproperty("truststore");
    String trustpassword = cleanproperty("truststorepassword");
    KeyStore truststore = buildkeystore(trustpath, trustpassword);
    KeyStore keystore = buildkeystore(keypath, keypassword);

    authcontrols.put(AuthProp.KEYSTORE, keystore);
    authcontrols.put(AuthProp.KEYPASSWORD, keypassword);
    authcontrols.put(AuthProp.TRUSTSTORE, truststore);
    authcontrols.put(AuthProp.TRUSTPASSWORD, trustpassword);
  }

  protected static KeyStore buildkeystore(String keypath, String keypassword) {
    KeyStore keystore;
    try {
      if (keypath != null && keypassword != null) {
        keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream instream = new FileInputStream(new File(keypath))) {
          keystore.load(instream, keypassword.toCharArray());
        }
      } else
        keystore = null;
    } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
      keystore = null;
    }
    return keystore;
  }

  static void buildproxy(AuthControls ac) {
    // Proxy flags
    String proxyurl = getproxyurl();
    if (proxyurl == null)
      return;
    URI uri;
    try {
      uri = HTTPUtil.parseToURI(proxyurl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Bad proxy URL: " + proxyurl);
    }
    HttpHost httpproxy = null;
    HttpHost httpsproxy = null;
    String user = null;
    String pwd = null;
    if (uri.getScheme().equals("http")) {
      httpproxy = new HttpHost(uri.getHost(), uri.getPort(), "http");
    } else if (uri.getScheme().equals("https")) {
      httpsproxy = new HttpHost(uri.getHost(), uri.getPort(), "https");
    }
    // User info may contain encoded characters (such as a username containing
    // the @ symbol), and we want to preserve those, so use getRawAuthority()
    // here.
    String upw = uri.getRawUserInfo();
    if (upw != null) {
      String[] pieces = upw.split("[:]");
      if (pieces.length == 2 && HTTPUtil.nullify(pieces[0]) != null && HTTPUtil.nullify(pieces[1]) != null) {
        user = pieces[0];
        pwd = pieces[1];
      } else {
        throw new IllegalArgumentException("Bad userinfo: " + proxyurl);
      }
    }
    ac.put(AuthProp.HTTPPROXY, httpproxy);
    ac.put(AuthProp.HTTPSPROXY, httpsproxy);
    ac.put(AuthProp.PROXYUSER, user);
    ac.put(AuthProp.PROXYPWD, pwd);
  }

  static String getproxyurl() {
    String proxyurl = cleanproperty("proxyurl");
    if (proxyurl == null) {
      // Check the java.net flags
      String proxyhost = cleanproperty("https.proxyHost");
      if (proxyhost != null) {
        StringBuilder buf = new StringBuilder();
        buf.append("https://");
        buf.append(proxyhost);
        String proxyport = cleanproperty("https.proxyPort");
        if (proxyport != null) {
          buf.append(":");
          buf.append(proxyport);
        }
        proxyurl = buf.toString();
      }
    }
    return proxyurl;

  }

  static void buildsslfactory(AuthControls authcontrols, KeyStore truststore, KeyStore keystore, String keypassword) {
    SSLConnectionSocketFactory globalsslfactory;
    try {
      // set up the context
      SSLContextBuilder sslbuilder = SSLContexts.custom();
      TrustStrategy strat = new LooseTrustStrategy();
      if (truststore != null)
        sslbuilder.loadTrustMaterial(truststore, strat);
      else
        sslbuilder.loadTrustMaterial(strat);
      sslbuilder.loadTrustMaterial(truststore, new LooseTrustStrategy());
      if (keystore != null)
        sslbuilder.loadKeyMaterial(keystore, keypassword.toCharArray());
      globalsslfactory = new SSLConnectionSocketFactory(sslbuilder.build(), new NoopHostnameVerifier());
    } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableEntryException e) {
      log.error("Failed to set key/trust store(s): " + e.getMessage());
      globalsslfactory = null;
    }
    if (globalsslfactory != null)
      authcontrols.put(AuthProp.SSLFACTORY, globalsslfactory);
  }

  /*
   * Original code with IGNORECERTS which was removed to prevent security hole.
   * jlcaron 8/15/2019
   * 
   * static void
   * buildsslfactory(AuthControls authcontrols, KeyStore truststore, KeyStore keystore, String keypassword)
   * {
   * SSLConnectionSocketFactory globalsslfactory;
   * try {
   * // set up the context
   * SSLContext scxt = null;
   * if(IGNORECERTS) {
   * scxt = SSLContext.getInstance("TLS");
   * TrustManager[] trust_mgr = new TrustManager[]{
   * new X509TrustManager()
   * {
   * public X509Certificate[] getAcceptedIssuers()
   * {
   * return null;
   * }
   * 
   * public void checkClientTrusted(X509Certificate[] certs, String t)
   * {
   * }
   * 
   * public void checkServerTrusted(X509Certificate[] certs, String t)
   * {
   * }
   * }};
   * scxt.init(null, // key manager
   * trust_mgr, // trust manager
   * new SecureRandom()); // random number generator
   * } else {
   * SSLContextBuilder sslbuilder = SSLContexts.custom();
   * TrustStrategy strat = new LooseTrustStrategy();
   * if(truststore != null)
   * sslbuilder.loadTrustMaterial(truststore, strat);
   * else
   * sslbuilder.loadTrustMaterial(strat);
   * sslbuilder.loadTrustMaterial(truststore, new LooseTrustStrategy());
   * if(keystore != null)
   * sslbuilder.loadKeyMaterial(keystore, keypassword.toCharArray());
   * scxt = sslbuilder.build();
   * }
   * globalsslfactory = new SSLConnectionSocketFactory(scxt, new NoopHostnameVerifier());
   * } catch (KeyStoreException
   * | NoSuchAlgorithmException
   * | KeyManagementException
   * | UnrecoverableEntryException e) {
   * log.error("Failed to set key/trust store(s): " + e.getMessage());
   * globalsslfactory = null;
   * }
   * if(globalsslfactory != null)
   * authcontrols.put(AuthProp.SSLFACTORY, globalsslfactory);
   * }
   */

  static synchronized void processDFlags() {
    // Pull overrides from command line
    int seconds = getDPropInt(DCONNTIMEOUT);
    if (seconds > 0)
      setGlobalConnectionTimeout(seconds * 1000);
    seconds = getDPropInt(DSOTIMEOUT);
    if (seconds > 0)
      setGlobalSoTimeout(seconds * 1000);
    int conns = getDPropInt(DMAXCONNS);
    if (conns > 0)
      setGlobalMaxConnections(conns);
  }

  //////////////////////////////////////////////////////////////////////////
  // Static Methods (Mostly global accessors)
  // Synchronized as a rule.

  public static synchronized void setGlobalUserAgent(String userAgent) {
    globalsettings.put(Prop.USER_AGENT, userAgent);
  }

  public static synchronized String getGlobalUserAgent() {
    return (String) globalsettings.get(Prop.USER_AGENT);
  }

  public static synchronized void setGlobalMaxConnections(int n) {
    globalsettings.put(Prop.MAX_CONNECTIONS, n);
    HTTPConnections.setDefaultMaxConections(n);
    if (connmgr != null)
      connmgr.setMaxConnections(n);
  }

  public static synchronized int getGlobalMaxConnection() {
    return (Integer) globalsettings.get(Prop.MAX_CONNECTIONS);
  }

  // Timeouts

  public static synchronized void setGlobalConnectionTimeout(int timeout) {
    if (timeout >= 0) {
      globalsettings.put(Prop.CONN_TIMEOUT, (Integer) timeout);
      globalsettings.put(Prop.CONN_REQ_TIMEOUT, (Integer) timeout);
    }
  }

  public static synchronized void setGlobalSoTimeout(int timeout) {
    if (timeout >= 0)
      globalsettings.put(Prop.SO_TIMEOUT, (Integer) timeout);
  }

  /**
   * Enable/disable redirection following
   * Default is yes.
   */
  public static synchronized void setGlobalFollowRedirects(boolean tf) {
    globalsettings.put(Prop.HANDLE_REDIRECTS, (Boolean) tf);
  }


  /**
   * Set the max number of redirects to follow
   *
   * @param n
   */
  public static synchronized void setGlobalMaxRedirects(int n) {
    if (n < 0) // validate
      throw new IllegalArgumentException("setMaxRedirects");
    globalsettings.put(Prop.MAX_REDIRECTS, n);
  }

  public static synchronized Object getGlobalSetting(String key) {
    return globalsettings.get(key);
  }

  //////////////////////////////////////////////////
  // Compression

  public static synchronized void setGlobalCompression(String compressors) {
    if (globalsettings.get(Prop.COMPRESSION) != null)
      removeGlobalCompression();
    String compresslist = checkCompressors(compressors);
    if (HTTPUtil.nullify(compresslist) == null)
      throw new IllegalArgumentException("Bad compressors: " + compressors);
    globalsettings.put(Prop.COMPRESSION, compresslist);
    HttpResponseInterceptor hrsi;
    if (compresslist.contains("gzip")) {
      hrsi = new GZIPResponseInterceptor();
      rspintercepts.add(hrsi);
    }
    if (compresslist.contains("deflate")) {
      hrsi = new DeflateResponseInterceptor();
      rspintercepts.add(hrsi);
    }
  }

  public static synchronized void removeGlobalCompression() {
    if (globalsettings.remove(Prop.COMPRESSION) != null) {
      for (int i = rspintercepts.size() - 1; i >= 0; i--) { // walk backwards
        HttpResponseInterceptor hrsi = rspintercepts.get(i);
        if (hrsi instanceof GZIPResponseInterceptor || hrsi instanceof DeflateResponseInterceptor)
          rspintercepts.remove(i);
      }
    }
  }

  protected static synchronized String checkCompressors(String compressors) {
    // Syntactic check of compressors
    Set<String> cset = new HashSet<>();
    compressors = compressors.replace(',', ' ');
    compressors = compressors.replace('\t', ' ');
    String[] pieces = compressors.split("[ ]+");
    for (String p : pieces) {
      for (String c : KNOWNCOMPRESSORS) {
        if (p.equalsIgnoreCase(c)) {
          cset.add(c);
          break;
        }
      }
    }
    StringBuilder buf = new StringBuilder();
    for (String s : cset) {
      if (buf.length() > 0)
        buf.append(",");
      buf.append(s);
    }
    return buf.toString();
  }

  //////////////////////////////////////////////////
  // Authorization
  // global

  /**
   * @param provider the credentials provider
   * @throws HTTPException
   */
  public static void setGlobalCredentialsProvider(CredentialsProvider provider) throws HTTPException {
    if (provider == null)
      throw new NullPointerException("HTTPSession");
    globalprovider = provider;
  }

  //////////////////////////////////////////////////
  // Miscellaneous

  public static synchronized void setGlobalRetryCount(int n) {
    if (n < 0) // validate
      throw new IllegalArgumentException("setGlobalRetryCount");
    globalsettings.put(Prop.RETRIES, n);
    globalretryhandler = new DefaultHttpRequestRetryHandler(n, false);
  }

  //////////////////////////////////////////////////
  // Instance variables

  // Currently, the granularity of authorization is host+port.
  protected String sessionURI = null; // This is either a real url
  // or one constructed from an AuthScope
  protected URI scopeURI = null; // constructed
  protected AuthScope scope = null;
  protected boolean closed = false;

  // Store the per-session credentials provider, if any
  protected CredentialsProvider sessionprovider = null;

  protected ConcurrentSkipListSet<HTTPMethod> methods = new ConcurrentSkipListSet<>();
  protected String identifier = "Session";
  protected Map<Prop, Object> localsettings = new ConcurrentHashMap<Prop, Object>();

  // We currently only allow the use of global interceptors
  // protected List<Object> intercepts = new ArrayList<Object>(); // current set of interceptors;

  // This context is re-used over all method executions so that we maintain
  // cookies, credentials, etc.
  // In theory this also supports credentials cache clearing.
  protected HttpClientContext sessioncontext = HttpClientContext.create();
  protected HTTPAuthCache sessioncache = new HTTPAuthCache();

  protected URI requestURI = null; // full uri from the HTTPMethod call

  // cached and recreated as needed
  protected boolean cachevalid = false; // Are cached items up-to-date?
  protected RequestConfig cachedconfig = null;

  //////////////////////////////////////////////////
  // Constructor(s)
  // All are package level so that only HTTPFactory can be used externally

  protected HTTPSession() throws HTTPException {}

  HTTPSession(String host, int port) throws HTTPException {
    init(new AuthScope(host, port, null, null), null);
  }

  HTTPSession(String uri) throws HTTPException {
    init(HTTPAuthUtil.uriToAuthScope(uri), uri);
  }

  HTTPSession(HttpHost httphost) throws HTTPException {
    init(HTTPAuthUtil.hostToAuthScope(httphost), null);
  }

  protected void init(AuthScope scope, String actualurl) throws HTTPException {
    assert (scope != null);
    if (actualurl != null)
      this.sessionURI = actualurl;
    else
      this.sessionURI = HTTPAuthUtil.authscopeToURI(scope).toString();
    this.scope = scope;
    this.scopeURI = HTTPAuthUtil.authscopeToURI(scope);
    this.cachevalid = false; // Force build on first use
    this.sessioncontext.setCookieStore(new BasicCookieStore());
    this.sessioncache = new HTTPAuthCache();
    this.sessioncontext.setAuthCache(sessioncache);
  }

  //////////////////////////////////////////////////
  // Interceptors: Only supported at global level

  protected static void setInterceptors(HttpClientBuilder cb) {
    for (HttpRequestInterceptor hrq : reqintercepts) {
      cb.addInterceptorLast(hrq);
    }
    for (HttpResponseInterceptor hrs : rspintercepts) {
      cb.addInterceptorLast(hrs);
    }
    // Add debug interceptors
    for (HttpRequestInterceptor hrq : dbgreq) {
      cb.addInterceptorFirst(hrq);
    }
    for (HttpResponseInterceptor hrs : dbgrsp) {
      cb.addInterceptorFirst(hrs);
    }
    // Hack: add Content-Encoding suppressor
    cb.addInterceptorFirst(CEKILL);
  }

  //////////////////////////////////////////////////
  // Accessor(s)

  public AuthScope getAuthScope() {
    return this.scope;
  }

  public String getSessionURI() {
    return this.sessionURI;

  }

  /**
   * Extract the sessionid cookie value
   *
   * @return sessionid string
   */
  public String getSessionID() {
    String sid = null;
    String jsid = null;
    List<Cookie> cookies = this.sessioncontext.getCookieStore().getCookies();
    for (Cookie cookie : cookies) {
      if (cookie.getName().equalsIgnoreCase("sessionid"))
        sid = cookie.getValue();
      if (cookie.getName().equalsIgnoreCase("jsessionid"))
        jsid = cookie.getValue();
    }
    return (sid == null ? jsid : sid);
  }

  public HTTPSession setUserAgent(String agent) {
    if (agent == null || agent.length() == 0)
      throw new IllegalArgumentException("null argument");
    localsettings.put(Prop.USER_AGENT, agent);
    this.cachevalid = false;
    return this;
  }

  public HTTPSession setSoTimeout(int timeout) {
    if (timeout <= 0)
      throw new IllegalArgumentException("setSoTimeout");
    localsettings.put(Prop.SO_TIMEOUT, timeout);
    this.cachevalid = false;
    return this;
  }

  public HTTPSession setConnectionTimeout(int timeout) {
    if (timeout <= 0)
      throw new IllegalArgumentException("setConnectionTImeout");
    localsettings.put(Prop.CONN_TIMEOUT, timeout);
    localsettings.put(Prop.CONN_REQ_TIMEOUT, timeout);
    this.cachevalid = false;
    return this;
  }

  /**
   * Set the max number of redirects to follow
   *
   * @param n
   */
  public HTTPSession setMaxRedirects(int n) {
    if (n < 0) // validate
      throw new IllegalArgumentException("setMaxRedirects");
    localsettings.put(Prop.MAX_REDIRECTS, n);
    this.cachevalid = false;
    return this;
  }

  /**
   * Enable/disable redirection following
   * Default is yes.
   */
  public HTTPSession setFollowRedirects(boolean tf) {
    localsettings.put(Prop.HANDLE_REDIRECTS, (Boolean) tf);
    this.cachevalid = false;
    return this;
  }

  /**
   * Should we use sessionid's?
   *
   * @param tf
   */
  public HTTPSession setUseSessions(boolean tf) {
    localsettings.put(Prop.USESESSIONS, (Boolean) tf);
    this.cachevalid = false;
    return this;
  }

  public List<Cookie> getCookies() {
    if (this.sessioncontext == null)
      return null;
    List<Cookie> cookies = this.sessioncontext.getCookieStore().getCookies();
    return cookies;
  }

  public HTTPSession clearCookies() {
    BasicCookieStore cookies = (BasicCookieStore) this.sessioncontext.getCookieStore();
    if (cookies != null)
      cookies.clear();
    return this;
  }

  // make package specific

  HttpClientContext getExecutionContext() {
    return this.sessioncontext;
  }

  public Object getSetting(String key) {
    return localsettings.get(key);
  }

  //////////////////////////////////////////////////

  /**
   * Close the session. This implies closing
   * any open methods.
   */

  public synchronized void close() {
    if (this.closed)
      return; // multiple calls ok
    closed = true;
    for (HTTPMethod m : this.methods) {
      m.close(); // forcibly close; will invoke removemethod().
    }
    methods.clear();
  }

  synchronized HTTPSession addMethod(HTTPMethod m) {
    if (!this.methods.contains(m))
      this.methods.add(m);
    return this;
  }

  synchronized HTTPSession removeMethod(HTTPMethod m) {
    this.methods.remove(m);
    connmgr.freeManager(m);
    return this;
  }

  //////////////////////////////////////////////////
  // Authorization
  // per-session versions of the global accessors

  /**
   * @param provider the credentials provider
   * @throws HTTPException
   */
  public HTTPSession setCredentialsProvider(CredentialsProvider provider) throws HTTPException {
    if (provider == null)
      throw new NullPointerException(this.getClass().getName());
    sessionprovider = provider;
    return this;
  }

  //////////////////////////////////////////////////
  // Called only by HTTPMethod.execute to get relevant session state.

  /**
   * This is used by HTTPMethod to sclear the credentials provider.
   * in theory, this should be done automatically, but apparently not.
   */
  protected synchronized void clearProvider() {
    if (sessionprovider != null)
      sessionprovider.clear();
    else if (globalprovider != null)
      globalprovider.clear();
  }

  /**
   * This is used by HTTPMethod to set the in-url name+pwd into
   * the credentials provider, if defined. If a CredentialsProvider
   * is not defined for the session, create a new BasicCredentialsProvider
   */
  protected synchronized void setCredentials(URI uri) throws HTTPException {
    // create a BasicCredentialsProvider if current session
    // does not have a provider
    if (sessionprovider == null) {
      sessionprovider = new BasicCredentialsProvider();
    }

    if (sessionprovider != null) {
      // User info may contain encoded characters (such as a username containing
      // the @ symbol), and we want to preserve those, so use getRawUserInfo()
      // here.
      String userinfo = HTTPUtil.nullify(uri.getRawUserInfo());
      if (userinfo != null) {
        // Construct an AuthScope from the uri
        AuthScope scope = HTTPAuthUtil.uriToAuthScope(uri);
        // Save the credentials
        sessionprovider.setCredentials(scope, new UsernamePasswordCredentials(userinfo));
      }
    } else {
      log.warn(
          "Cannot store credentials as no CredientialsProvier can be found for session. Connection will fail if authentication / authorization required");
    }
  }

  /**
   * Handle authentication and Proxy'ing
   *
   * @param cb
   * @throws HTTPException
   */

  protected synchronized void setAuthenticationAndProxy(HttpClientBuilder cb) throws HTTPException {
    // First, setup the ssl factory
    cb.setSSLSocketFactory((SSLConnectionSocketFactory) authcontrols.get(AuthProp.SSLFACTORY));

    // Second, Store the proxy credentials into the current credentials
    // provider, either the global or local credentials; local overrides global

    // Build the proxy credentials and AuthScope
    Credentials proxycreds = null;
    AuthScope proxyscope = null;
    CredentialsProvider cp = sessionprovider;
    if (cp == null)
      cp = globalprovider;
    // Notify the AuthCache
    if (sessioncache != null)
      sessioncache.addProvider(cp);
    String user = (String) authcontrols.get(AuthProp.PROXYUSER);
    String pwd = (String) authcontrols.get(AuthProp.PROXYPWD);
    HttpHost httpproxy = (HttpHost) authcontrols.get(AuthProp.HTTPPROXY);
    HttpHost httpsproxy = (HttpHost) authcontrols.get(AuthProp.HTTPSPROXY);
    if (user != null && (httpproxy != null || httpsproxy != null)) {
      if (httpproxy != null)
        proxyscope = HTTPAuthUtil.hostToAuthScope(httpproxy);
      else // httpsproxy != null
        proxyscope = HTTPAuthUtil.hostToAuthScope(httpsproxy);
      proxycreds = new UsernamePasswordCredentials(user, pwd);
    }
    if (cp == null && proxycreds != null && proxyscope != null) {
      // If client provider is null and proxycreds are not,
      // then use proxycreds alone
      cp = new BasicCredentialsProvider();
    }
    // add proxycreds to the client provider
    if (cp != null && proxycreds != null && proxyscope != null) {
      cp.setCredentials(proxyscope, proxycreds);
    }
    this.sessioncontext.setCredentialsProvider(cp);
  }

  /* package */
  void setRetryHandler(HttpClientBuilder cb) throws HTTPException {
    if (globalretryhandler != null) {
      cb.setRetryHandler(globalretryhandler);
    }
  }

  /* package */
  void setContentDecoderRegistry(HttpClientBuilder cb) {
    cb.setContentDecoderRegistry(contentDecoderMap);
  }

  /* package */
  void setClientManager(HttpClientBuilder cb, HTTPMethod m) {
    connmgr.setClientManager(cb, m);
  }

  /* package */
  HttpClientContext getContext() {
    return this.sessioncontext;
  }

  /* package */
  AuthScope getSessionScope() {
    return this.scope;
  }

  /* package scope */
  public Map<Prop, Object> mergedSettings() {
    Map<Prop, Object> merged;
    synchronized (this) {// keep coverity happy
      // Merge Settings;
      merged = HTTPUtil.merge(globalsettings, localsettings);
    }
    return Collections.unmodifiableMap(merged);
  }

  public ImmutableMap<String, String> getMergedSettings() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    mergedSettings().forEach((key, value) -> builder.put(key.name(), value.toString()));
    return builder.build();
  }

  //////////////////////////////////////////////////
  // Utilities

  static String getCanonicalURL(String legalurl) {
    if (legalurl == null)
      return null;
    int index = legalurl.indexOf('?');
    if (index >= 0)
      legalurl = legalurl.substring(0, index);
    // remove any trailing extension
    // index = legalurl.lastIndexOf('.');
    // if(index >= 0) legalurl = legalurl.substring(0,index);
    return HTTPUtil.canonicalpath(legalurl);
  }


  static String getUrlAsString(String url) throws HTTPException {
    try (HTTPMethod m = HTTPFactory.Get(url)) {
      int status = m.execute();
      String content = null;
      if (status == 200) {
        content = m.getResponseAsString();
      }
      return content;
    }
  }

  static int putUrlAsString(String content, String url) throws HTTPException {
    int status = 0;
    try {
      try (HTTPMethod m = HTTPFactory.Put(url)) {
        m.setRequestContent(new StringEntity(content, ContentType.create("application/text", "UTF-8")));
        status = m.execute();
      }
    } catch (UnsupportedCharsetException uce) {
      throw new HTTPException(uce);
    }
    return status;
  }

  static String getstorepath(String prefix) {
    String path = System.getProperty(prefix + "store");
    if (path != null) {
      path = path.trim();
      if (path.length() == 0)
        path = null;
    }
    return path;
  }

  static String getpassword(String prefix) {
    String password = System.getProperty(prefix + "storepassword");
    if (password != null) {
      password = password.trim();
      if (password.length() == 0)
        password = null;
    }
    return password;
  }

  static String cleanproperty(String property) {
    String value = System.getProperty(property);
    if (value != null) {
      value = value.trim();
      if (value.length() == 0)
        value = null;
    }
    return value;
  }

  //////////////////////////////////////////////////
  // Testing support

  // Expose the state for testing purposes
  public synchronized boolean isClosed() {
    return this.closed;
  }

  public synchronized int getMethodcount() {
    return methods.size();
  }

  //////////////////////////////////////////////////
  // Debug interface

  // Provide a way to kill everything at the end of a Test

  // When testing, we need to be able to clean up
  // all existing sessions because JUnit can run all
  // test within a single jvm.
  static Set<HTTPSession> sessionList = null; // List of all HTTPSession instances

  // If we are testing, then track the sessions for kill
  protected static synchronized void track(HTTPSession session) {
    if (!TESTING)
      throw new UnsupportedOperationException();
    if (sessionList == null)
      sessionList = new ConcurrentSkipListSet<HTTPSession>();
    sessionList.add(session);
  }

  public static synchronized void setInterceptors(boolean print) {
    if (!TESTING)
      throw new UnsupportedOperationException();
    HTTPUtil.InterceptRequest rq = new HTTPUtil.InterceptRequest();
    HTTPUtil.InterceptResponse rs = new HTTPUtil.InterceptResponse();
    rq.setPrint(print);
    rs.setPrint(print);
    /* remove any previous */
    for (int i = reqintercepts.size() - 1; i >= 0; i--) {
      HttpRequestInterceptor hr = reqintercepts.get(i);
      if (hr instanceof HTTPUtil.InterceptCommon)
        reqintercepts.remove(i);
    }
    for (int i = rspintercepts.size() - 1; i >= 0; i--) {
      HttpResponseInterceptor hr = rspintercepts.get(i);
      if (hr instanceof HTTPUtil.InterceptCommon)
        rspintercepts.remove(i);
    }
    reqintercepts.add(rq);
    rspintercepts.add(rs);
  }

  public static void resetInterceptors() {
    if (!TESTING)
      throw new UnsupportedOperationException();
    for (HttpRequestInterceptor hri : reqintercepts) {
      if (hri instanceof HTTPUtil.InterceptCommon)
        ((HTTPUtil.InterceptCommon) hri).clear();
    }
  }

  public static HTTPUtil.InterceptRequest debugRequestInterceptor() {
    if (!TESTING)
      throw new UnsupportedOperationException();
    for (HttpRequestInterceptor hri : reqintercepts) {
      if (hri instanceof HTTPUtil.InterceptRequest)
        return ((HTTPUtil.InterceptRequest) hri);
    }
    return null;
  }

  public static HTTPUtil.InterceptResponse debugResponseInterceptor() {
    if (!TESTING)
      throw new UnsupportedOperationException();
    for (HttpResponseInterceptor hri : rspintercepts) {
      if (hri instanceof HTTPUtil.InterceptResponse)
        return ((HTTPUtil.InterceptResponse) hri);
    }
    return null;
  }


  /* Only allow if debugging */
  public static void clearkeystore() {
    if (!TESTING)
      throw new UnsupportedOperationException();
    authcontrols.setReadOnly(false);
    authcontrols.remove(AuthProp.KEYSTORE);
    authcontrols.remove(AuthProp.KEYPASSWORD);
    authcontrols.remove(AuthProp.TRUSTSTORE);
    authcontrols.remove(AuthProp.TRUSTPASSWORD);
    authcontrols.setReadOnly(true);
  }

  /* Only allow if debugging */
  public static void rebuildkeystore(String path, String pwd) {
    if (!TESTING)
      throw new UnsupportedOperationException();
    KeyStore newks = buildkeystore(path, pwd);
    authcontrols.setReadOnly(false);
    authcontrols.put(AuthProp.KEYSTORE, newks);
    authcontrols.setReadOnly(true);
  }

  //////////////////////////////////////////////////
  // Deprecated, but here for back compatibility

  @Deprecated
  public static void setGlobalCredentialsProvider(AuthScope scope, CredentialsProvider provider) throws HTTPException {
    setGlobalCredentialsProvider(provider); // ignore scope
  }

  @Deprecated
  public static void setGlobalCredentialsProvider(String url, CredentialsProvider provider) throws HTTPException {
    setGlobalCredentialsProvider(provider);
  }

  @Deprecated
  public HTTPSession setCredentialsProvider(String url, CredentialsProvider provider) throws HTTPException {
    return setCredentialsProvider(provider);
  }

  @Deprecated
  public HTTPSession setCredentialsProvider(AuthScope scope, CredentialsProvider provider) throws HTTPException {
    return setCredentialsProvider(provider);
  }

  @Deprecated
  public static int getRetryCount() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public static void setGlobalCompression() {
    setGlobalCompression("gzip,deflate");
  }

  @Deprecated
  public static void setGlobalProxy(String host, int port) {
    throw new UnsupportedOperationException("setGlobalProxy: use -D flags");
  }

  @Deprecated
  public void setProxy(String host, int port) {
    setGlobalProxy(host, port);
  }


  @Deprecated
  public static void setGlobalCredentialsProvider(CredentialsProvider provider, String scheme) throws HTTPException {
    setGlobalCredentialsProvider(provider);
  }

  @Deprecated
  public void clearState() {
    // no-op
  }

  @Deprecated
  public String getSessionURL() {
    return getSessionURI();
  }

  // Obsolete
  // make package private as only needed for testing
  static void validatestate() {
    connmgr.validate();
  }
}
