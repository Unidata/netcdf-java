/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provide an extended form of URI parser with the following features:
 * 1. can parse the query and fragment parts.
 * 2. supports multiple protocols
 * 3. supports modification of the URI path.
 * 4. supports url -> string as controlled by flags
 */

public class XURI {

  //////////////////////////////////////////////////
  // Constants
  static final char QUERYSEP = '&';
  static final char FRAGMENTSEP = QUERYSEP;
  static final char ESCAPECHAR = '\\';
  static final char PAIRSEP = '=';

  // static final String DRIVELETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  static protected final Pattern drivelettertest = Pattern.compile("^([/]?)[a-zA-Z][:]");
  static protected final Pattern filedrivelettertest = Pattern.compile("^(file://)([a-zA-Z][:].*)");

  // Define assembly flags

  public static enum Parts {
    SCHEME, // base protocol(s)
    PWD, // including user
    HOST, // including port
    PATH, QUERY, FRAG;
  }

  // Mnemonics
  public static final EnumSet<Parts> URLONLY = EnumSet.of(Parts.SCHEME, Parts.PWD, Parts.HOST, Parts.PATH);
  public static final EnumSet<Parts> URLALL =
      EnumSet.of(Parts.SCHEME, Parts.PWD, Parts.HOST, Parts.PATH, Parts.QUERY, Parts.FRAG);
  public static final EnumSet<Parts> URLQUERY =
      EnumSet.of(Parts.SCHEME, Parts.PWD, Parts.HOST, Parts.PATH, Parts.QUERY);

  //////////////////////////////////////////////////
  // Instance variables

  // Unfortunately, URI is final and so cannot be used as super class
  // Rather we use delegation.
  URI parent; // Does the bulk of the parsing

  // protected String originaluri = null;
  protected boolean isfile = false;

  protected List<String> nonleadschemes = new ArrayList<>(); // all schemes after the first

  protected Map<String, String> queryfields // decomposed query
      = null;
  protected Map<String, String> fragfields // decomposed fragment
      = null;

  //////////////////////////////////////////////////
  // Constructor(s)

  public XURI(String xu) throws URISyntaxException {
    xu = XURI.canonical(xu);
    if (xu == null)
      throw new URISyntaxException("null", "null valued URI string");
    xu = fixURI(xu);
    parent = new URI(xu); // this should be correct
    this.isfile = "file".equalsIgnoreCase(getScheme());
  }

  /**
   * This makes an incoming url parseable by URI
   * Fix various issues such as:
   * -- multiple schemes
   * -- windows drive letters in path
   */
  String fixURI(String xu) throws URISyntaxException {
    // Extract all schemes
    String[] schemes = allSchemes(xu);
    if (schemes.length == 0)
      return xu; // no schemes at all
    int iremainder = xu.indexOf("//");
    xu = schemes[0] + ":" + xu.substring(iremainder); // remove all but lead scheme
    nonleadschemes.addAll(Arrays.asList(schemes).subList(1, schemes.length));
    // Look for windows drive letter
    Matcher m = filedrivelettertest.matcher(xu);
    if (m.lookingAt()) {
      String prefix = m.group(1);
      String pathplus = m.group(2);
      // rebuild
      xu = prefix + hideDriveLetter(pathplus);
    }
    return xu;
  }

  // Pull off all the schemes into schemelist
  // return the uri string with just the leading scheme
  String parseSchemes(String xu, List<String> schemelist) throws URISyntaxException {
    int iremainder = xu.indexOf("//");
    if (iremainder <= 0)
      throw new URISyntaxException(xu, "URI has no scheme");
    String[] schemes = xu.substring(0, iremainder).split(":");
    String lead = schemes[0];
    xu = lead + ":" + xu.substring(iremainder, xu.length());
    schemelist.clear();
    for (int i = 1; i < schemes.length; i++)
      schemelist.add(schemes[i]);
    return xu;
  }

  public XURI(URL xu) throws URISyntaxException {
    this.parent = new URI(xu.getProtocol().toLowerCase(), xu.getUserInfo(), xu.getHost(), xu.getPort(), xu.getFile(),
        xu.getQuery(), xu.getRef());
    // this.originaluri = this.toString(); // save the original uri
  }

  public XURI(URI xu) throws URISyntaxException {
    this.parent = new URI(xu.getScheme().toLowerCase(), xu.getUserInfo(), xu.getHost(), xu.getPort(), xu.getPath(),
        xu.getQuery(), xu.getFragment());
    // this.originaluri = this.toString(); // save the original uri
  }

  //////////////////////////////////////////////////
  // Delegation

  // Getters
  public String getUserInfo() {
    return parent.getUserInfo();
  }

  public String getHost() {
    return parent.getHost();
  }

  public int getPort() {
    return parent.getPort();
  }

  public String getPath() {
    return parent.getPath();
  }

  /**
   * Return a path that can be used to open a file
   *
   * @return
   */
  public String getRealPath() {
    return truePath(getPath());
  }

  public String getQuery() {
    return parent.getQuery();
  }

  public String getFragment() {
    return parent.getFragment();
  }

  public String getScheme() {
    return parent.getScheme();
  } // return lead protocol

  public List<String> getSchemes() { // return lead scheme + nonleadschemes
    if (parent.getScheme() == null)
      return null;
    List<String> allschemes = new ArrayList<>();
    allschemes.add(parent.getScheme());
    allschemes.addAll(nonleadschemes);
    return allschemes; // return lead scheme
  }

  // Setters
  public void setScheme(String xscheme) {
    try {
      this.parent = new URI(xscheme.toLowerCase(), parent.getUserInfo(), parent.getHost(), parent.getPort(),
          parent.getPath(), parent.getQuery(), parent.getFragment());
    } catch (URISyntaxException use) {
      throw new AssertionError("URI.setScheme: Internal error: malformed URI", use);
    }
  }

  public void setUserInfo(String xuserinfo) {
    try {
      this.parent = new URI(parent.getScheme(), xuserinfo, parent.getHost(), parent.getPort(), parent.getPath(),
          parent.getQuery(), parent.getFragment());
    } catch (URISyntaxException use) {
      throw new AssertionError("URI.setUserInfo: Internal error: malformed URI", use);
    }
  }

  public void setHost(String xhost) {
    try {
      this.parent = new URI(parent.getScheme(), parent.getUserInfo(), xhost, parent.getPort(), parent.getPath(),
          parent.getQuery(), parent.getFragment());
    } catch (URISyntaxException use) {
      throw new AssertionError("URI.setHost: Internal error: malformed URI", use);
    }
  }

  public void setPort(int xport) {
    try {
      this.parent = new URI(parent.getScheme(), parent.getUserInfo(), parent.getHost(), xport, parent.getPath(),
          parent.getQuery(), parent.getFragment());
    } catch (URISyntaxException use) {
      throw new AssertionError("URI.setPort: Internal error: malformed URI", use);
    }
  }

  public void setPath(String xpath) {
    try {
      if (hasDriveLetter(xpath) && xpath.charAt(0) != '/')
        xpath = "/" + xpath; // make it URI parseable
      this.parent = new URI(parent.getScheme(), parent.getUserInfo(), parent.getHost(), parent.getPort(), xpath,
          parent.getQuery(), parent.getFragment());
    } catch (URISyntaxException use) {
      throw new AssertionError("URI.setPath: Internal error: malformed URI", use);
    }
  }

  public void setQuery(String xquery) {
    try {
      this.parent = new URI(parent.getScheme(), parent.getUserInfo(), parent.getHost(), parent.getPort(),
          parent.getPath(), xquery, parent.getFragment());
      this.queryfields = null;
    } catch (URISyntaxException use) {
      throw new AssertionError("URI.setQuery: Internal error: malformed URI", use);
    }
  }

  public void setFragment(String xfragment) {
    try {
      this.parent = new URI(parent.getScheme(), parent.getUserInfo(), parent.getHost(), parent.getPort(),
          parent.getPath(), parent.getQuery(), xfragment);
      this.fragfields = null;
    } catch (URISyntaxException use) {
      throw new AssertionError("URI.setFragment: Internal error: malformed URI", use);
    }
  }

  public String toString() {
    return assemble(URLALL);
  }

  /**
   * Allow queryfields to be inserted
   *
   * @param key
   * @param newval
   * @return previous value or this value if key not set
   */
  public void insertQueryField(String key, String newval) {
    // Watch out in case there is no query
    this.queryfields = insertAmpField(key, newval, parent.getQuery());
    rebuildQuery();
  }

  /**
   * Allow queryfields to be removed
   *
   * @param key
   * @return previous value (may be null)
   */
  public void removeQueryField(String key) {
    // Watch out in case there is no query
    this.queryfields = removeAmpField(key, parent.getQuery());
    rebuildQuery();
  }

  /**
   * Rebuild query from current queryfields
   */
  protected void rebuildQuery() {
    if (queryfields == null)
      return;
    StringBuffer query = new StringBuffer();
    boolean first = true;
    for (Map.Entry entry : queryfields.entrySet()) {
      if (!first)
        query.append("&");
      query.append(Escape.urlEncodeQuery((String) entry.getKey()));
      query.append("=");
      query.append(Escape.urlEncodeQuery((String) entry.getValue()));
      first = false;
    }
    String newquery = null;
    if (query.length() > 0)
      newquery = query.toString();
    setQuery(newquery);
  }

  //////////////////////////////////////////////////
  // Accessors (other than delegations)

  // public String getOriginal() {
  // return originaluri;
  // }

  public boolean isFile() {
    return this.isfile;
  }

  public Map<String, String> getQueryFields() {
    if (this.queryfields == null)
      parseQuery();
    return this.queryfields;
  }

  public Map<String, String> getFragFields() {
    if (this.fragfields == null)
      parseFragment();
    return this.fragfields;
  }

  protected void parseQuery() {
    this.queryfields = parseAmpList(getQuery(), QUERYSEP, ESCAPECHAR);
  }

  protected void parseFragment() {
    this.fragfields = parseAmpList(getFragment(), FRAGMENTSEP, ESCAPECHAR);
  }

  protected Map<String, String> insertAmpField(String key, String value, String query) {
    Map<String, String> fields = parseAmpList(query, '&', '\\');
    fields.put(key, value);
    return fields;
  }

  protected Map<String, String> removeAmpField(String key, String query) {
    Map<String, String> fields = parseAmpList(query, '&', '\\');
    fields.remove(key);
    return fields;
  }

  protected Map<String, String> parseAmpList(String s, char sep, char escape) {
    Map<String, String> map = new HashMap<>();
    List<String> pieces;
    if (s == null)
      s = "";
    pieces = escapedsplit(s, sep, escape);
    for (String piece : pieces) {
      int plen = piece.length();
      // Split on first '='
      int index = findunescaped(piece, 0, PAIRSEP, escape, plen);
      if (index < 0)
        index = plen;
      String key = piece.substring(0, index);
      String value = (index >= plen ? "" : piece.substring(index + 1, plen));
      key = Escape.urlDecode(key);
      key = key.toLowerCase(); // for consistent lookup
      value = Escape.urlDecode(value);
      map.put(key, value);
    }
    return map;
  }

  /*
   * Split s into pieces as separated by separator and taking
   * escape characters into account.
   */
  protected List<String> escapedsplit(String s, char sep, char escape) {
    List<String> pieces = new ArrayList<>();
    int len = s.length();
    int from = 0;
    while (from < len) {
      int index = findunescaped(s, from, sep, escape, len);
      if (index < 0)
        index = len;
      pieces.add(s.substring(from, index));
      from = index + 1;
    }
    return pieces;
  }

  /*
   * It is probably possible to do this with regexp patterns,
   * but I do not know how.
   *
   * The goal is to find next instance of unescaped separator character.
   * Leave any escape characters in place.
   * Return index of next separator starting at start.
   * If not found, then return -1;
   */
  protected int findunescaped(String s, int start, char sep, char escape, int len) {
    int i;
    for (i = start; i < len; i++) {
      char c = s.charAt(i);
      if (c == escape) {
        i++;
      } else if (c == sep) {
        return i;
      }
    }
    return -1; /* not found */
  }

  //////////////////////////////////////////////////
  // API

  /**
   * Reassemble the url using the specified parts
   *
   * @param parts to include
   * @return the assembled uri
   */

  public String assemble(EnumSet<Parts> parts) {
    StringBuilder uri = new StringBuilder();
    if (parts.contains(Parts.SCHEME) && this.getScheme() != null) {
      uri.append(this.getScheme());
      for (int i = 0; i < nonleadschemes.size(); i++) {
        uri.append(':');
        uri.append(nonleadschemes.get(i));
      }
      uri.append("://");
    }
    if (parts.contains(Parts.PWD) && this.getUserInfo() != null) {
      uri.append(this.getUserInfo());
      uri.append("@");
    }
    if (parts.contains(Parts.HOST) && this.getHost() != null) {
      uri.append(this.getHost());
      if (this.getPort() > 0) {
        uri.append(":");
        uri.append(this.getPort());
      }
    }
    if (parts.contains(Parts.PATH) && this.getPath() != null) {
      uri.append(this.getPath());
    }
    if (parts.contains(Parts.QUERY) && this.getQuery() != null) {
      uri.append("?");
      uri.append(this.getQuery());
    }
    if (parts.contains(Parts.FRAG) && this.getFragment() != null) {
      uri.append("#");
      uri.append(this.getFragment());
    }
    return uri.toString();
  }


  // if the url is file:// and the path part has a windows drive letter, then the parent URL will not recognize this
  // correctly.
  public String fixdriveletter(URI uri) throws URISyntaxException {
    if (!uri.getScheme().equalsIgnoreCase("file"))
      return uri.toString();
    String tmp = uri.getHost(); // we use host because this is how URI parses such urls
    if (!hasDriveLetter(tmp))
      return uri.toString(); // Original file url did not a have Windows drive letter
    // rebuild the original URI (ugh!)
    StringBuilder newxu = new StringBuilder();
    newxu.append(uri.getScheme());
    newxu.append("://");
    if (uri.getUserInfo() != null) {
      newxu.append(uri.getUserInfo());
      newxu.append("@");
    }
    newxu.append("/");
    newxu.append(uri.getHost()); // that's where the drive letter ends up
    newxu.append(":");
    newxu.append(uri.getPath());
    if (uri.getQuery() != null) {
      newxu.append("?");
      newxu.append(uri.getQuery());
    }
    if (uri.getFragment() != null) {
      newxu.append("#");
      newxu.append(uri.getFragment());
    }
    return newxu.toString();
  }

  ////////////////////////////////////////////////
  /**
   * Canonicalize a part of a URL
   *
   * @param s part of the url
   */
  static public String canonical(String s) {
    if (s != null) {
      s = s.trim();
      if (s.length() == 0)
        s = null;
    }
    return s;
  }

  /**
   * return true if this path appears to start with a windows drive letter
   * including those hidden by leading '/'
   *
   * @param path
   * @return true, if path has drive letter
   */
  static public boolean hasDriveLetter(String path) {
    Matcher m = drivelettertest.matcher(path);
    return m.lookingAt();
  }

  /**
   * Return a path with possible windows drive letter hidden by '/'.
   * Repair => convert "x:/" to "/x:/" where x is a drive letter
   *
   * @return repaired path
   */
  static public String hideDriveLetter(String path) {
    Matcher m = drivelettertest.matcher(path);
    if (m.lookingAt()) {
      if (m.group(1).equals(""))
        path = "/" + path;
    }
    return path;
  }

  /**
   * Extract all schemes at front of a url.
   *
   * @parem xu url string
   * @return vector of schemes
   */
  static public String[] allSchemes(String xu) {
    int endschemes = xu.indexOf("//");
    if (endschemes < 0)
      return new String[0];
    String[] schemes = xu.substring(0, endschemes).split(":");
    return schemes;
  }

  /**
   * return repaired path; if this path appears to start with a windows drive letter
   * hidden by leading '/', then the leading '/' is removed
   *
   * @param path
   * @return repaired path
   */
  static public String truePath(String path) {
    // Check for a windows drive and repair
    Matcher m = drivelettertest.matcher(path);
    if (m.lookingAt() && m.group(1) != null)
      path = path.substring(1);
    return path;
  }
}
