/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.prefs;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.lang.invoke.MethodHandles;
import javax.swing.*;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestFieldInput {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private static XMLStore xstore;
  private static PreferencesExt store;

  private Field tf, intf, d1, d2;

  @BeforeClass
  public static void setUp() {
    try {
      xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    } catch (java.io.IOException e) {
    }
    store = xstore.getPreferences();
  }

  @Test
  public void testFieldInput() {
    try {
      PreferencesExt node = (PreferencesExt) store.node("test22");
      PrefPanel.Dialog d = new PrefPanel.Dialog(null, false, "title", null);
      PrefPanel pp = d.getPrefPanel();

      tf = pp.addTextField("text", "name", "defValue");
      tf.addPropertyChangeListener(new PropertyChangeListener() {
        int count = 0;

        public void propertyChange(PropertyChangeEvent evt) {
          System.out.println(
              "Text TestFieldInput: old val= <" + evt.getOldValue() + "> new val= <" + evt.getNewValue() + ">");
          if (count == 0) {
            assertThat(evt.getOldValue()).equals("defValue");
            assertThat(evt.getNewValue()).equals("better value");
          } else if (count == 1) {
            assertThat(evt.getOldValue()).equals("better value");
            assertThat(evt.getNewValue()).equals("best value");
          }
          count++;
        }
      });
      ((Field.Text) tf).setText("better value");

      intf = pp.addIntField("int", "int", 66);
      intf.addPropertyChangeListener(new PropertyChangeListener() {
        int count = 0;

        public void propertyChange(PropertyChangeEvent evt) {
          System.out.println(
              "Int TestFieldInput:  old val= <" + evt.getOldValue() + "> new val= <" + evt.getNewValue() + ">");
          if (count == 0) {
            assertThat(evt.getOldValue()).isInstanceOf(Integer.class);
            assertThat((Integer) evt.getOldValue()).isEqualTo(66);
            assertThat((Integer) evt.getNewValue()).isEqualTo(666);
          }
          if (count == 1) {
            assertThat(evt.getOldValue()).isInstanceOf(Number.class);
            assertThat(evt.getNewValue()).isInstanceOf(Number.class);
            assertThat((Number) evt.getOldValue()).isEqualTo(666);
            assertThat((Number) evt.getNewValue()).isEqualTo(6666);
          }
          count++;
          // System.out.println(" getInt = "+ ((Field.Int)intf).getInt());
        }
      });
      ((Field.Int) intf).setInt(666);

      d1 = pp.addDoubleField("min", "min", -999.99);
      d1.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          System.out.println(
              "Double 1 TestFieldInput: old val= <" + evt.getOldValue() + "> new val= <" + evt.getNewValue() + ">");
        }
      });

      d2 = pp.addDoubleField("max", "max", 10000.0);
      d2.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          System.out.println(
              "Double 2: TestFieldInput: old val= <" + evt.getOldValue() + "> new val= <" + evt.getNewValue() + ">");
        }
      });
      d.finish();
      d.setVisible(true);

      node.put("text", "best value");
      node.putInt("int", 6666);
      node.putDouble("min", 0.0);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

  @Test
  public void testit() {
    try {

      try {
        xstore = XMLStore.createFromFile(File.createTempFile("foo", "bar").getAbsolutePath(), null);
      } catch (java.io.IOException e) {
      }
      store = xstore.getPreferences();

      new TestFieldInput().testFieldInput();

      JFrame frame = new JFrame("TestFieldInput");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });

      JPanel main = new JPanel(new BorderLayout()); // new FlowLayout());
      frame.getContentPane().add(main);
      JFormattedTextField d1 = new JFormattedTextField();
      d1.setValue(123.987);
      JFormattedTextField d2 = new JFormattedTextField();
      d2.setValue(999.123);
      main.add(d1, BorderLayout.NORTH);
      main.add(d2, BorderLayout.SOUTH);

      frame.pack();
      frame.setLocation(400, 300);
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }
}

