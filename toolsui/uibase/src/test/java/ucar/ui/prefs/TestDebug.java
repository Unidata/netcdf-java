/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestDebug {
  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private static PreferencesExt store;
  private static XMLStore xstore;

  @BeforeClass
  public static void setup() throws IOException {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");

    xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    store = xstore.getPreferences();
    // store = new PreferencesExt(null,"");
    Debug.setStore(store.node("Debug"));
  }

  @Test
  public void testDebug() {
    Debug.set("testit", true);
    assertThat(Debug.isSet("testit")).isTrue();

    Debug.set("fart/allow", true);
    // assert( Debug.isSet("fart.allow"));
    assertThat(Debug.isSet("fart/allow")).isTrue();

    Debug.set("fart/allow", false);
    assertThat(Debug.isSet("fart/allow")).isFalse();

    assertThat(Debug.isSet("fart/notSet")).isFalse();
    try {
      assertThat(store.nodeExists("fart")).isFalse();
      assertThat(store.nodeExists("/Debug/fart")).isTrue();
      assertThat(store.nodeExists("Debug/fart")).isTrue();
    } catch (Exception e) {
      assertThat(false);
    }
  }

  @Test
  public void testMenu() {
    Debug.constructMenu(new javax.swing.JMenu());
    try {
      xstore.save();
    } catch (java.io.IOException e) {
      assertThat(false);
    }
  }
}
