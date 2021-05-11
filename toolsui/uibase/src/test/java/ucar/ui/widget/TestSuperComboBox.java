package ucar.ui.widget;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.junit.Test;

public class TestSuperComboBox {

  @Test
  public void testStuff() {
    try {
      JFrame frame = new JFrame("Test Combo Box");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });

      ArrayList a = new ArrayList(30);
      for (int i = 0; i < 30; i++)
        a.add("hifdsjflkjslfk " + i);
      SuperComboBox scb = new SuperComboBox(frame, "myTestdjdslkfjslkj", true, a.iterator());
      JComboBox cb = new JComboBox();
      for (Object o : a) {
        cb.addItem(o);
      }

      JPanel main = new JPanel(new FlowLayout());
      frame.getContentPane().add(main);
      main.setPreferredSize(new Dimension(200, 200));
      main.add(scb);
      main.add(cb);

      frame.pack();
      frame.setLocation(300, 300);
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

}
