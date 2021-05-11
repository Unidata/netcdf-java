package ucar.ui.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;

public class TestMyMouseAdapter {

  @Test
  public void testStuff() {
    try {
      JFrame frame = new JFrame("Test MyMouseAdapter");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });

      JLabel comp = new JLabel("test  sdfk sdf ks;dflk ;sdlkf ldsk lk");
      comp.setOpaque(true);
      comp.setBackground(Color.white);
      comp.setForeground(Color.black);

      comp.addMouseListener(new MyMouseAdapter());

      JPanel main = new JPanel(new FlowLayout());
      frame.getContentPane().add(main);
      main.setPreferredSize(new Dimension(200, 200));
      main.add(comp);

      frame.pack();
      frame.setLocation(300, 300);
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

}
