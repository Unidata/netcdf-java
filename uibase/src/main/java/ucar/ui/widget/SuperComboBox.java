/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JWindow;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import ucar.ui.event.ActionSourceListener;
import ucar.ui.event.ActionValueEvent;
import ucar.ui.event.ActionValueListener;
import ucar.ui.table.JTableSorted;
import ucar.ui.table.TableRow;
import ucar.ui.table.TableRowAbstract;
import ucar.ui.util.NamedObject;
import ucar.ui.prefs.Field;
import ucar.ui.prefs.PrefPanel;

/**
 * SuperComboBox is a complete rewrite of JComboBox;
 * it does not extend JComboBox (!)
 *
 * Items added may implement NamedObject, in which case getName() is used as the row name,
 * and getDescription() is used as the tooltip. Otherwise, o.toString() is used as the row name,
 * and there is no tooltip. The row names must be unique.
 *
 * An ActionValueEvent is thrown when the selection is changed through manipulation of the widget.
 * The selection can be obtained from actionEvent.getValue(), which returns the the selection row name.
 *
 * The caller can change the selection by calling setSelectionByName(). In this case,
 * no event is thrown.
 *
 * @author John Caron
 */
public class SuperComboBox extends JPanel {
  private static final boolean debug = false;
  private static final boolean debugEvent = false;

  // data
  private ArrayList<TableRow> rows = new ArrayList<>(); // list of things in the combobox

  // UI stuff
  private final String name;
  private final MyTextField text; // this is what shows all the time
  private final JWindow pulldown; // pulldown menu
  private final JTableSorted table; // inside the pulldown
  private final IndependentWindow loopControl; // loop control

  private final ActionSourceListener actionSource;

  private boolean eventOK = true; // disallow events to prevent infinite loop
  private boolean sendExternalEvent = true; // disallow events to prevent infinite loop
  private boolean immediateMode; // used for looping

  /**
   * default is one column, with an iterator of NamedObjects
   * 
   * @param parent parent container
   * @param name column name
   * @param sortOK true allow sorting, column adding and removing
   * @param iter Iterator of objects inside the combobox.
   */
  public SuperComboBox(RootPaneContainer parent, String name, boolean sortOK, Iterator<Object> iter) {
    this.name = name;

    // create JLabel
    text = new MyTextField(" ");
    text.setToolTipText(name);

    // create arrow buttons
    JPanel seqPanel = new JPanel(new GridLayout(1, 2));
    JButton prev = makeButton(SpinIcon.TypeLeft);
    prev.setToolTipText("previous");
    seqPanel.add(prev);

    JButton next = makeButton(SpinIcon.TypeRight);
    next.setToolTipText("next");
    seqPanel.add(next);

    // JPanel bPanel = new JPanel(new GridLayout(2,1));
    JPanel bPanel = new JPanel(new BorderLayout());
    // arrow button
    JButton down = makeButton(SpinIcon.TypeDown);
    down.setToolTipText("show menu");
    bPanel.add(seqPanel, BorderLayout.NORTH);
    bPanel.add(down, BorderLayout.SOUTH);

    // the jtable
    String[] colNames = new String[1];
    colNames[0] = name;

    // LOOK JTableSorted needs TableRow
    table = new JTableSorted(colNames, rows);
    table.setSortOK(sortOK);
    if (iter != null)
      setCollection(iter);

    // the pulldown menu list
    JFrame parentComponent = (parent instanceof JFrame) ? (JFrame) parent : null;
    pulldown = new JWindow(parentComponent);
    pulldown.getContentPane().add(table);
    pulldown.pack();

    // put it together
    setBorder(new EtchedBorder(EtchedBorder.RAISED));
    setLayout(new BorderLayout());
    // add(seqPanel, BorderLayout.WEST);
    add(text, BorderLayout.CENTER);
    add(bPanel, BorderLayout.EAST);

    // add the listeners
    text.addMouseListener(new MyMouseAdapter() {
      public void click(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e))
          showPulldownMenu();
      }
    });

    // popup menu
    LoopControl popup = new LoopControl();
    text.addMouseListener(popup);
    loopControl = popup.getLoopControl();

    prev.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        table.incrSelected(false);
      }
    });
    next.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        table.incrSelected(true);
      }
    });
    down.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        showPulldownMenu();
      }
    });

    table.addListSelectionListener(e -> {
      if (debugEvent)
        System.out.println(" JTable event ");
      if (eventOK && !e.getValueIsAdjusting()) {
        setSelection();
        hidePulldownMenu();
      }
    });
    table.getTable().addMouseListener(new MyMouseAdapter() {
      public void click(MouseEvent e) {
        hidePulldownMenu();
      }
    });

    // event management
    actionSource = new ActionSourceListener(name) {
      public void actionPerformed(ActionValueEvent e) {
        if (debugEvent)
          System.out.println(" actionSource event " + e);
        setSelectedByName(e.getValue().toString());
      }
    };
  }

  /** add ActionValueListener listener */
  public void addActionValueListener(ActionValueListener l) {
    actionSource.addActionValueListener(l);
  }

  /** remove ActionValueListener listener */
  public void removeActionValueListener(ActionValueListener l) {
    actionSource.removeActionValueListener(l);
  }

  /** better way to do event management */
  public ActionSourceListener getActionSourceListener() {
    return actionSource;
  }

  /** get the LoopControl Window associated with this list */
  public IndependentWindow getLoopControl() {
    return loopControl;
  }

  /** get the name associated with this list */
  public String getName() {
    return name;
  }

  /**
   * Set the list of things to be selected, with keepIndex = false.
   * Iterator may return NamedObject, TableRow, or an Object that will be wrapped by SimpleRow()
   */
  public void setCollection(Iterator<? extends Object> iter) {
    setCollection(iter, false);
  }

  /**
   * Set the list of things to be selected..
   * 
   * @param iter iterator of NamedObject, TableRow, or an Object that will wrapped by SimpleRow().
   * @param keepIndex maintain the current selected index if possible.
   */
  public void setCollection(Iterator<? extends Object> iter, boolean keepIndex) {
    eventOK = false;
    rows = new ArrayList<>();

    if (iter != null) {
      FontMetrics fm = text.getFontMetrics(text.getFont());

      int width = 0;
      while (iter.hasNext()) {
        Object o = iter.next();
        TableRow row;
        if (o instanceof TableRow) {
          row = (TableRow) o;
        } else if (o instanceof NamedObject) {
          row = new NamedObjectRow((NamedObject) o);
        } else {
          row = new SimpleRow(o);
        }

        String s = row.getValueAt(0).toString().trim();
        int slen = fm.stringWidth(s);
        width = Math.max(width, slen);

        rows.add(row);
      }
      // resize
      Dimension d = text.getPreferredSize();
      d.width = width + 10;
      text.setPreferredSize(d);
      text.revalidate();
    }

    if (debugEvent) {
      System.out.println(" New collection set = ");
    }
    table.setRows(rows);
    if (rows.isEmpty())
      setLabel("none");
    else {
      int currIndex = getSelectedIndex();
      if (currIndex >= rows.size() || currIndex < 0 || !keepIndex)
        setSelectedByIndex(0);
    }
    eventOK = true;
  }

  /**
   * Set the displayed text. Typically its only used when the list is empty.
   */
  public void setLabel(String s) {
    text.setText(s);
  }

  /**
   * Get the currently selected object. May be null.
   */
  @Nullable
  public Object getSelectedObject() {
    TableRow selected = getSelectedRow();
    return (selected == null) ? null : selected.getUserObject();
  }

  /**
   * Get the index of the currently selected object in the list.
   * If sortOK, then this may not be the original index.
   * 
   * @return index of selected object, or -1 if none selected.
   */
  public int getSelectedIndex() {
    return table.getSelectedRowIndex();
  }

  /**
   * Set the currently selected object using its choice name.
   * Note that no event is sent due to this call.
   * 
   * @param choiceName name of object to match.
   * @return index of selection, or -1 if not found;
   */
  public int setSelectedByName(String choiceName) {
    for (int i = 0; i < rows.size(); i++) {
      TableRow row = rows.get(i);
      String value = row.getValueAt(0).toString();
      if (choiceName.equals(value)) {
        sendExternalEvent = false;
        setSelectedByIndex(i);
        sendExternalEvent = true;
        return i;
      }
    }
    // setSelectedByIndex(-1); // force it to 0 or none
    return -1;
  }

  public int setSelectedByValue(Object choice) {
    for (int i = 0; i < rows.size(); i++) {
      TableRow row = rows.get(i);
      if (choice.equals(row.getValueAt(0))) {
        sendExternalEvent = false;
        setSelectedByIndex(i);
        sendExternalEvent = true;
        return i;
      }
    }
    // setSelectedByIndex(-1); // force it to 0 or none
    return -1;
  }

  /**
   * Set the currently selected object using its index.
   * If sortOK, then this may not be the original index.
   * 
   * @param index of selected object.
   */
  public void setSelectedByIndex(int index) {
    if ((index >= 0) && (index < rows.size())) {
      table.setSelected(index);
      setSelection();
    } else if (!rows.isEmpty()) {
      table.setSelected(0);
      setSelection();
    } else
      setLabel("none");
  }

  ////////////////////////////////////////////////////

  private TableRow getSelectedRow() {
    return table.getSelected();
  }

  private void setSelection() {
    TableRow selected = getSelectedRow();
    if (debug)
      System.out.println(" setSelection = " + selected);
    if (selected != null) {
      Object selectedObject = selected.getValueAt(0);
      String selectedName = selectedObject.toString();
      text.setText(selectedName.trim());
      if ((selected instanceof NamedObjectRow) || (selected instanceof GeoGridRow)) {
        text.setToolTipText(((NamedObject) selected.getUserObject()).getDescription());
      }
      repaint();

      if (selectedObject instanceof GeoGridRow) {
        selectedObject = ((GeoGridRow) selectedObject).getUserObject();
      }

      if (sendExternalEvent) {
        if (debugEvent)
          System.out.println("--->SuperCombo send event " + selectedName);
        if (immediateMode)
          actionSource.fireActionValueEvent("redrawImmediate", selectedObject);
        else
          actionSource.fireActionValueEvent(ActionSourceListener.SELECTED, selectedObject);
      }
    }
  }

  private void showPulldownMenu() {
    if (pulldown.isShowing())
      pulldown.setVisible(false);
    else {
      int height = 222;
      Dimension d = new Dimension(getWidth(), height);
      pulldown.setSize(d);
      Point p = text.getLocationOnScreen();
      p.y += text.getHeight();
      pulldown.setLocation(p);
      pulldown.setVisible(true);
    }
  }

  private void hidePulldownMenu() {
    if (pulldown.isShowing()) {
      pulldown.setVisible(false);
    }
  }

  private JButton makeButton(SpinIcon.Type type) {
    SpinIcon icon = new SpinIcon(type);
    JButton butt = new JButton(icon);
    Insets i = new Insets(0, 0, 0, 0);
    butt.setMargin(i);
    butt.setBorderPainted(false);
    butt.setFocusPainted(false);
    butt.setPreferredSize(new Dimension(icon.getIconWidth() + 2, icon.getIconHeight() + 2));
    return butt;
  }


  private static class SimpleRow extends TableRowAbstract {
    Object o;

    SimpleRow(Object o) {
      this.o = o;
    }

    public Object getValueAt(int col) {
      return o;
    }

    public Object getUserObject() {
      return o;
    }
  }

  private static class NamedObjectRow extends TableRowAbstract implements NamedObject {
    NamedObject o;

    NamedObjectRow(NamedObject o) {
      this.o = o;
    }

    public Object getValueAt(int col) {
      return this;
    }

    public Object getUserObject() {
      return o;
    }

    public Object getValue() {
      return o;
    }

    public String getName() {
      return o.getName();
    }

    public String getDescription() {
      return o.getDescription();
    }

    public String toString() {
      return getName();
    }
  }

  private static class GeoGridRow extends TableRowAbstract implements NamedObject {
    NamedObject o;

    GeoGridRow(NamedObject o) {
      this.o = o;
    }

    public Object getValueAt(int col) {
      return this;
    }

    public Object getUserObject() {
      return o;
    }

    public Object getValue() {
      return o;
    }

    public String getName() {
      return o.getName();
    }

    public String getDescription() {
      return o.getDescription();
    }

    public String toString() {
      String desc = o.getDescription();
      if (desc == null || desc.isEmpty() || desc.trim().equals("-"))
        return o.getName();
      else
        return o.getName() + " == " + o.getDescription();
    }
  }

  private class MyTextField extends JLabel {
    private final int arrow_size = 4;
    private boolean wasDragged;
    private Rectangle b;
    private int nitems;
    private int currentItem;

    MyTextField(String name) {
      super(name);
      setOpaque(true);
      setBackground(Color.white);
      setForeground(Color.black);

      addMouseListener(new MyMouseListener());
      addMouseMotionListener(new MyMouseMotionListener());
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      g.setColor(Color.black);
      // g.setColor( component.isEnabled() ? MetalLookAndFeel.getControlInfo() :
      // MetalLookAndFeel.getControlShadow() );
      b = getBounds();
      nitems = rows.size(); // number of items

      int posx = getItemPos();
      int line = b.height - 1;
      for (int w2 = arrow_size; w2 >= 0; w2--) {
        g.drawLine(posx - w2, line, posx + w2, line);
        line--;
      }
    }

    // return slider indicator position for currently selected item
    protected int getItemPos() {
      if (nitems < 1)
        return -arrow_size; // dont show indicator
      else if (nitems == 1)
        return b.width / 2; // indicator in center

      int item = table.getSelectedRowIndex(); // selected item
      int eff_width = b.width - 2 * arrow_size; // effective width
      int pixel = (item * eff_width) / (nitems - 1); // divided into n-1 intervals
      return pixel + arrow_size;
    }

    // return item selected by this pixel position
    protected int getItem(int pixel) {
      if (nitems < 2)
        return 0;

      int eff_width = b.width - 2 * arrow_size; // effective width
      double fitem = ((double) (pixel - arrow_size) * (nitems - 1)) / eff_width;
      int item = (int) (fitem + .5);
      item = Math.max(Math.min(item, nitems - 1), 0);
      return item;
    }

    private class MyMouseListener extends MouseAdapter {
      public void mousePressed(MouseEvent anEvent) {
        sendExternalEvent = false;
        wasDragged = false;
        currentItem = table.getSelectedRowIndex();
      }

      public void mouseReleased(MouseEvent anEvent) {
        sendExternalEvent = true;
        if (wasDragged) {
          int item = getItem(anEvent.getX());
          if (item != currentItem) {
            setSelection();
          }
        }
        wasDragged = false;
      }
    }

    private class MyMouseMotionListener extends MouseMotionAdapter {

      public void mouseDragged(MouseEvent anEvent) {
        int item = getItem(anEvent.getX());
        table.setSelected(item);
        MyTextField.this.repaint();
        wasDragged = true;
      }
    }

  } // inner class MyTextField

  private class LoopControl extends ucar.ui.widget.PopupMenu.PopupTriggerListener {
    private final IndependentWindow iw;

    private final JPanel loopPanel;
    private final JButton moreOrLess;
    private AbstractButton loopButt, helpButt;
    private final PrefPanel ifPanel;
    private final Field.Int stepIF;
    private final Field.Text startIF;

    private final AbstractAction loopAct, helpAct;
    private boolean stopped, forward, first = true, continuous = true, less = true;
    private int step = 1, start = -1;

    LoopControl() {
      loopPanel = new JPanel();

      // create VCR buttons
      AbstractAction play = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          start(true);
        }
      };
      BAMutil.setActionProperties(play, "VCRPlay", "play", false, 'S', KeyEvent.VK_NUMPAD6);
      BAMutil.addActionToContainer(loopPanel, play);

      AbstractAction fastforward = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          setSelectedByIndex(rows.size() - 1);
        }
      };
      BAMutil.setActionProperties(fastforward, "VCRFastForward", "go to end", false, 'F', KeyEvent.VK_NUMPAD1);
      BAMutil.addActionToContainer(loopPanel, fastforward);

      AbstractAction next = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          int current = incr(true, false);
          if (current >= 0) {
            setSelectedByIndex(current);
            text.paintImmediately(text.getBounds());
          }
        }
      };
      BAMutil.setActionProperties(next, "VCRNextFrame", "Next frame", false, 'N', KeyEvent.VK_PAGE_DOWN);
      BAMutil.addActionToContainer(loopPanel, next);

      AbstractAction stop = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          stopped = true;
        }
      };
      BAMutil.setActionProperties(stop, "VCRStop", "stop", false, 'S', KeyEvent.VK_ESCAPE);
      BAMutil.addActionToContainer(loopPanel, stop);

      AbstractAction prev = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          int current = incr(false, false);
          if (current >= 0) {
            setSelectedByIndex(current);
            text.paintImmediately(text.getBounds());
          }
        }
      };
      BAMutil.setActionProperties(prev, "VCRPrevFrame", "Previous frame", false, 'P', KeyEvent.VK_PAGE_UP);
      BAMutil.addActionToContainer(loopPanel, prev);

      AbstractAction rewind = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          setSelectedByIndex(0);
        }
      };
      BAMutil.setActionProperties(rewind, "VCRRewind", "rewind", false, 'R', KeyEvent.VK_NUMPAD7);
      BAMutil.addActionToContainer(loopPanel, rewind);

      AbstractAction back = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          start(false);
        }
      };
      BAMutil.setActionProperties(back, "VCRBack", "play backwards", false, 'B', KeyEvent.VK_NUMPAD4);
      BAMutil.addActionToContainer(loopPanel, back);

      moreOrLess = new JButton(new SpinIcon(SpinIcon.TypeRight));
      moreOrLess.setBorder(BorderFactory.createEmptyBorder());
      moreOrLess.setMargin(new Insets(0, 0, 0, 0));
      moreOrLess.addActionListener(new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (less)
            makeMore();
          else
            makeLess();
        }
      });
      loopPanel.add(moreOrLess);

      iw = new IndependentWindow(name + " loop", null, loopPanel);
      iw.setResizable(false);

      // these arent added to the panel right away
      loopAct = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          continuous = (Boolean) getValue(BAMutil.STATE);
        }
      };
      BAMutil.setActionProperties(loopAct, "MovieLoop", "continuous loop", true, 'L', 0);

      helpAct = new AbstractAction() {
        public void actionPerformed(ActionEvent evt) {
          // faqo.apputil.Help.getDefaultHelp().gotoTarget("movieLoop");
        }
      };
      BAMutil.setActionProperties(helpAct, "Help", "online Help...", false, 'H', KeyEvent.VK_H);

      JSpinner stepSpinner = new JSpinner();
      stepSpinner.setToolTipText("step");

      ifPanel = new PrefPanel("loopControl", null);
      stepIF = ifPanel.addIntField("step", "step", 1);
      startIF = ifPanel.addTextField("start", "start", "    0");
      ifPanel.finish(false);
    }

    private void makeMore() {
      loopButt = BAMutil.addActionToContainer(loopPanel, loopAct);
      loopAct.putValue(BAMutil.STATE, continuous);
      helpButt = BAMutil.addActionToContainer(loopPanel, helpAct);
      loopPanel.add(ifPanel);

      moreOrLess.setIcon(new SpinIcon(SpinIcon.TypeLeft));
      moreOrLess.setToolTipText("less");

      loopPanel.revalidate();
      iw.pack();
      less = false;
    }

    private void makeLess() {
      loopPanel.remove(loopButt);
      loopPanel.remove(helpButt);
      loopPanel.remove(ifPanel);

      moreOrLess.setIcon(new SpinIcon(SpinIcon.TypeRight));
      moreOrLess.setToolTipText("more");

      loopPanel.revalidate();
      iw.pack();
      less = true;
    }

    IndependentWindow getLoopControl() {
      return iw;
    }

    public void showPopup(MouseEvent e) {
      if (first) {
        Point pt = new Point(0, 0);
        SwingUtilities.convertPointToScreen(pt, SuperComboBox.this);
        iw.setLocation(pt);
        first = false;
      }
      iw.show();
    }

    private void start(boolean forward) {
      this.forward = forward;
      eventOK = false;
      immediateMode = true;
      stopped = false;

      ifPanel.accept();
      step = Math.min(stepIF.getInt(), 1);

      start = -1;
      String startName = startIF.getText();
      if ((startName != null) && (!startName.isEmpty())) {
        start = setSelectedByName(startName);
      }
      if (debug)
        System.out.println(" start = " + start + " step = " + step);

      SwingUtilities.invokeLater(new RunLoop()); // execute on eventTread
    }


    private int incr(boolean forward, boolean continuous) {
      int current = getSelectedIndex();
      if (forward) {
        current += step;
      } else {
        current -= step;
      }
      if (!continuous && ((current < 0) || (current >= rows.size()))) {
        return -1;
      }

      if (current >= rows.size()) {
        current = Math.max(start, 0);
      }
      if (current < 0) {
        current = (start >= 0) ? start : rows.size() - 1;
      }
      return current;
    }

    class RunLoop implements Runnable {
      public void run() {
        loopPanel.repaint();
        if (stopped) {
          stop();
          return;
        }

        int current = incr(forward, continuous);
        if (current < 0) {
          stop();
          return;
        }

        // goto next
        setSelectedByIndex(current);
        text.paintImmediately(text.getBounds());
        // set another event
        SwingUtilities.invokeLater(new RunLoop());
      }

      private void stop() {
        immediateMode = false;
        eventOK = true;
      }

    }

  } // inner class LoopControl

}
