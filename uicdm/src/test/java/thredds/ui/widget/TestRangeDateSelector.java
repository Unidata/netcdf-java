package thredds.ui.widget;

import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import org.junit.Test;
import ucar.nc2.ui.widget.RangeDateSelector;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;

public class TestRangeDateSelector {

  @Test
  public void testStuff() throws Exception {
    try {
      JFrame frame = new JFrame("Test Date Range Selector");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {System.exit(0);}
      });


      RangeDateSelector rs1 = new RangeDateSelector("Date Range", "1990-01-01T01:00:00", "1990-01-02T02:00:00",
          null, "15 minute", true, true, "i think im fallin", false);
  //    RangeDateSelector rs2 = new RangeDateSelector("Date", "1990-01-01", "1991-01-01", null, "1 day", false, true,
  //       "i think im fallin\n in love with youuuu ", false);
  //    RangeDateSelector rs3 = new RangeDateSelector("Date", "1990-01-01", "1991-01-01", null, "10 days", true, true,
  //       null, false);
  //    RangeDateSelector rs4 = new RangeDateSelector("Date", "1990-01-01", "1991-01-01", null, "10 days", false, false,
  //       null, true);
  //    RangeDateSelector rs5 = new RangeDateSelector("Date", null, "present", "10 days", "1 day", true, false,
  //       null, false);


      // simulate what we do in PointObsViewer
      DateRange range = null;
      try {
        range = new DateRange(); // phony
      } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      RangeDateSelector rs6 = new RangeDateSelector(null, range, false, true, null, false, false);
      DateUnit start = new DateUnit("0 secs since 2005-05-02 23:00:00");
      DateUnit end = new DateUnit("0 secs since 2005-05-02 23:59:59");
      rs6.setDateRange( new DateRange( start.getDate(), end.getDate()));

      Box main = new Box( BoxLayout.Y_AXIS);

      frame.getContentPane().add(main);
      //main.setPreferredSize(new Dimension(400, 200));
      //main.add( new JSlider(), BorderLayout.NORTH);
      main.add( rs1);
      /*main.add( rs2);
      main.add( rs3);
      main.add( rs4);
      main.add( rs5); */
      main.add( rs6);

      frame.pack();
      frame.setLocation(400, 300);
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }


}
