package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.junit.Test;

public class TestRangeSelector {

  @Test
  public void testStuff() {
    try {
      JFrame frame = new JFrame("Test Range Selector");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {System.exit(0);}
      });

      RangeSelector rs1 = new RangeSelector(null, ".07", ".09", ".0025", "dongles", true, null, false);
      RangeSelector rs2 = new RangeSelector("Time:", "100.0", "1000.0", null, "megaPancakes", true,
          "I need somebody, help, not just anybody HELP!", false);
      RangeSelector rs3 = new RangeSelector("Longitude:", "0", "1000", "10", "megaPancakes", false,
          null, false);
      RangeSelector rs4 = new RangeSelector("Flatulence:", "200", "1000", "17", "megaFauna", false,
          null, true);

      JPanel main = new JPanel( new FlowLayout());

      frame.getContentPane().add(main);
      //main.setPreferredSize(new Dimension(400, 200));
      //main.add( new JSlider(), BorderLayout.NORTH);
      main.add( rs1, BorderLayout.NORTH);
      main.add( rs2, BorderLayout.CENTER);
      main.add( rs3, BorderLayout.SOUTH);
      main.add( rs4, BorderLayout.SOUTH);
      //main.add( ri);

      frame.pack();
      frame.setLocation(400, 300);
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }
}
