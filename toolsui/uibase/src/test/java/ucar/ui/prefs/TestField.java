/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.prefs;

import java.awt.HeadlessException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import java.beans.*;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestField {

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  static {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  private static XMLStore xstore;
  private static PreferencesExt store;

  private int gotEvent1, gotEvent2;

  @BeforeClass
  public static void setUp() {
    try {
      xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    } catch (java.io.IOException e) {
    }
    store = xstore.getPreferences();
  }

  @Test
  public void testPropertyChange() {
    try {
      System.out.println("****TestField");
      PreferencesExt node = (PreferencesExt) store.node("test22");
      PrefPanel.Dialog d = new PrefPanel.Dialog(null, false, "title", node);
      PrefPanel pp = d.getPrefPanel();
      Field.Text tf = pp.addTextField("name", "name", "defValue");
      final Field.Int intf = pp.addIntField("int", "int", 66);

      tf.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          gotEvent1++;
          System.out.println("testField: (tf) PropertyChangeListener old val= <" + evt.getOldValue() + "> new val= <"
              + evt.getNewValue() + "> " + gotEvent1);
        }
      });

      intf.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          gotEvent2++;
          System.out.println("testField: (intf) PropertyChangeListener old val= <" + evt.getOldValue() + "> new val= <"
              + evt.getNewValue() + ">");
          System.out.println("   getInt = " + intf.getInt());
        }
      });

      d.finish();
      d.setVisible(true);

      assertThat(gotEvent1).isEqualTo(0);
      tf.setEditValue("better value");
      tf.accept(null);
      assertThat(gotEvent1).isEqualTo(1);
      node.put("name", "best value");
      // assertThat( gotEvent1).isEqualTo(2 : gotEvent1; race condition

      assertThat(gotEvent2).isEqualTo(0);
      intf.setInt(666);
      assertThat(gotEvent2).isEqualTo(0);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

  @Test
  public void testComboText() throws IOException {
    try {
      PreferencesExt node = (PreferencesExt) store.node("testCombo");
      PrefPanel pp = new PrefPanel("testCombo", node);
      Field.TextCombo fcb = pp.addTextComboField("datatypes", "Datatypes", DataType.getTypeNames(), 20, false);
      pp.finish();

      fcb.setText("newbie");
      String v = fcb.getText();
      assertThat(v).isEqualTo("newbie");

      pp.accept();
      xstore.save();
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

  @Test
  public void testComboObjects() throws IOException {
    try {
      PreferencesExt node = (PreferencesExt) store.node("testComboObjects");
      PrefPanel pp = new PrefPanel("testCombo", node);
      Field.TextCombo fcb = pp.addTextComboField("datatypes", "Datatypes", DataType.getTypes(), 20, false);
      pp.finish();

      fcb.setText("newbie");
      String v = fcb.getText();
      assertThat(v).isEqualTo("newbie");

      pp.accept();
      xstore.save();
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

  @Test
  public void testEnumCombo() throws IOException {
    try {
      PreferencesExt node = (PreferencesExt) store.node("testComboObjects");
      PrefPanel pp = new PrefPanel("testCombo", node);
      Field.EnumCombo fcb = pp.addEnumComboField("datatypes", "Datatypes", DataType.getTypes(), false);
      pp.finish();

      DataType t = DataType.FLOAT;
      fcb.setValue(t);
      Object vt = fcb.getValue();
      assertThat(vt).isEqualTo(t);

      pp.accept();
      xstore.save();
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

}
