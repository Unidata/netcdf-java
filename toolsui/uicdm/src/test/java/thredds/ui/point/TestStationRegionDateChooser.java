package thredds.ui.point;

import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import org.junit.Test;
import ucar.nc2.ui.point.StationRegionDateChooser;

public class TestStationRegionDateChooser {

  @Test
  public void testStuff() {
    try {
      StationRegionDateChooser slm = new StationRegionDateChooser();
      slm.setBounds(new Rectangle(10, 10, 400, 200));

      JFrame frame = new JFrame("StationRegionChooser Test");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });
      frame.getContentPane().add(slm);
      frame.pack();
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

}
