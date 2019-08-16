package ucar.ui.prefs;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.junit.Test;
import ucar.ui.prefs.LayoutM.Constraint;

public class TestLayoutM {

  @Test
  public void testStuff() {

    JFrame frame = new JFrame("Test LayoutM");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    JPanel main = new JPanel(new LayoutM("test"));
    frame.getContentPane().add(main);
    main.setPreferredSize(new Dimension(200, 200));

    JLabel lab1 = new JLabel( "test1:");
    main.add( lab1, new Constraint(null, 10, 10));
    JTextField f1 = new JTextField( "why dont you just");
    main.add( f1, new Constraint(lab1, 6, 0));

    JLabel lab2 = new JLabel( "test2:");
    main.add( lab2, new Constraint(lab1, 0, 10));
    JTextField f2 = new JTextField( "fade away?");
    main.add( f2, new Constraint(lab2, 6, 0));

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);

    Dimension r = f2.getSize();
    System.out.println("getPreferredSize "+ r);
    r.setSize( (int) (r.getWidth() + 50), (int) r.getHeight());
    f2.setPreferredSize( r);
    System.out.println("setPreferredSize "+ r);
    f2.revalidate();
  }

}
