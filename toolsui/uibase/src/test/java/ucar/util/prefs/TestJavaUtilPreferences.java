/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.util.Misc;
import java.io.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestJavaUtilPreferences {
  // only works if System.setProperty called before anything else
  @Before
  public void testWho() {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");

    // this makes PreferencesExt the SPI
    Preferences userRoot = Preferences.userRoot();
    assertThat(userRoot).isInstanceOf(PreferencesExt.class);
  }

  @Test
  public void testPutGet() {
    Preferences userRoot = Preferences.userRoot();
    assertThat(userRoot).isInstanceOf(PreferencesExt.class);

    userRoot.putDouble("testD", 3.14157);
    double d = userRoot.getDouble("testD", 0.0);
    assertThat(Misc.nearlyEquals(d, 3.14157)).isTrue();

    userRoot.putFloat("testF", 1.23456F);
    float f = userRoot.getFloat("testF", 0.0F);
    assertThat(Misc.nearlyEquals(f, 1.23456F)).isTrue();

    userRoot.putLong("testL", 12345678900L);
    long ll = userRoot.getLong("testL", 0);
    assertThat(ll).isEqualTo(12345678900L);

    userRoot.putInt("testI", 123456789);
    int ii = userRoot.getInt("testI", 0);
    assertThat(ii).isEqualTo(123456789);

    userRoot.put("testS", "youdBeDeadbyNow");
    String s = userRoot.get("testS", "");
    assertThat(s).isEqualTo("youdBeDeadbyNow");

    userRoot.putBoolean("testB", true);
    boolean b = userRoot.getBoolean("testB", false);
    assertThat(b).isTrue();

    byte[] barr = new byte[3];
    byte[] barr2 = new byte[3];
    barr[0] = 1;
    barr[1] = 2;
    barr[2] = 3;
    userRoot.putByteArray("testBA", barr);
    byte[] ba = userRoot.getByteArray("testBA", barr2);
    for (int i = 0; i < 3; i++)
      assertThat(ba[i]).isEqualTo(barr[i]);
  }

  @Test
  public void testSubnode() throws BackingStoreException {
    Preferences userRoot = Preferences.userRoot();
    Preferences subNode = userRoot.node("SemperUbi");
    assertThat(subNode).isInstanceOf(PreferencesExt.class);

    String[] keys = userRoot.keys();
    for (String key : keys) {
      String value = userRoot.get(key, "failed");
      subNode.put(key, value);
    }

    float f = subNode.getFloat("testF", 0.0F);
    assertThat(Misc.nearlyEquals(f, 1.23456F)).isTrue();

    long ll = subNode.getLong("testL", 0);
    assertThat(ll).isEqualTo(12345678900L);

    int ii = subNode.getInt("testI", 0);
    assertThat(ii).isEqualTo(123456789);

    String s = subNode.get("testS", "");
    assertThat(s).isEqualTo("youdBeDeadbyNow");

    boolean b = subNode.getBoolean("testB", false);
    assertThat(b).isTrue();

    byte[] barr = new byte[3];
    byte[] barr2 = new byte[3];
    barr[0] = 1;
    barr[1] = 2;
    barr[2] = 3;
    byte[] ba = subNode.getByteArray("testBA", barr2);
    for (int i = 0; i < 3; i++)
      assertThat(ba[i]).isEqualTo(barr[i]);
  }

  @Test
  public void testExport() throws Exception {
    Preferences userRoot = Preferences.userRoot();
    Preferences fromNode = userRoot.node("SemperUbi");
    assertThat(fromNode).isInstanceOf(PreferencesExt.class);

    ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
    fromNode.exportNode(os);
    String xml = os.toString();
    xml = substitute(xml, "SemperUbi", "SubUbi");
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
    Preferences.importPreferences(is);
    userRoot.exportSubtree(System.out);
  }

  @Test
  public void testRemove() {
    Preferences userRoot = Preferences.userRoot();
    Preferences subNode = userRoot.node("SemperUbi");
    assertThat(subNode).isInstanceOf(PreferencesExt.class);

    long ll;
    subNode.putLong("testL", 12345678900L);
    ll = subNode.getLong("testL", 0);
    assertThat(ll).isEqualTo(12345678900L);

    subNode.remove("testL");
    ll = subNode.getLong("testL", 0);
    assertThat(ll).isEqualTo(0);
  }

  @Test
  public void testRemoveNode() throws BackingStoreException {
    Preferences userRoot = Preferences.userRoot();
    Preferences subNode = userRoot.node("SemperUbi");
    Preferences newNode = subNode.node("SubSemperUbi");
    assertThat(newNode).isInstanceOf(PreferencesExt.class);

    newNode.putLong("testL", 12345678900L);
    long ll = subNode.getLong("testL", 0);
    assertThat(ll).isEqualTo(0);

    newNode.removeNode();
    String[] kidName = subNode.childrenNames();

    for (String aKidName : kidName) {
      assertThat(aKidName).isNotEqualTo("SubSemperUbi");
    }

    newNode = subNode.node("SubSemperUbi");
    ll = newNode.getLong("testL", 123L);
    assertThat(ll).isEqualTo(123);
  }

  /**
   * Find all occurrences of the "match" in original, and substitute the "subst" string
   *
   * @param original: starting string
   * @param match: string to match
   * @param subst: string to substitute
   * @return a new string with substitutions
   */
  String substitute(String original, String match, String subst) {
    String s = original;
    int pos;
    while (0 <= (pos = s.indexOf(match))) {
      StringBuilder sb = new StringBuilder(s);
      s = sb.replace(pos, pos + match.length(), subst).toString();
    }

    return s;
  }

}
