/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.time2.CalendarDate;
import ucar.nc2.time2.CalendarDateFormatter;
import ucar.nc2.time2.CalendarDateUnit;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.TimeUnit;
import ucar.ui.prefs.Debug;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import javax.swing.JButton;

public class UnitConvert extends OpPanel {
  private final TextHistoryPane ta;

  public UnitConvert(PreferencesExt prefs) {
    super(prefs, "unit:", false, false);

    ta = new TextHistoryPane(true);
    add(ta, BorderLayout.CENTER);

    JButton compareButton = new JButton("Compare");
    compareButton.addActionListener(e -> compare(cb.getSelectedItem()));
    buttPanel.add(compareButton);

    JButton dateButton = new JButton("UdunitDate");
    dateButton.addActionListener(e -> checkUdunits(cb.getSelectedItem()));
    buttPanel.add(dateButton);

    // JButton cdateButton = new JButton("CalendarDate");
    // cdateButton.addActionListener(e -> checkCalendarDate(cb.getSelectedItem()));
    // buttPanel.add(cdateButton);
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    try {
      SimpleUnit su = SimpleUnit.factoryWithExceptions(command);
      ta.setText("parse=" + command + "\n");
      ta.appendLine("SimpleUnit.toString()          =" + su + "\n");
      ta.appendLine("SimpleUnit.getCanonicalString  =" + su.getCanonicalString());
      ta.appendLine("SimpleUnit.getImplementingClass= " + su.getImplementingClass());
      ta.appendLine("SimpleUnit.isUnknownUnit       = " + su.isUnknownUnit());

      return true;
    } catch (Exception e) {
      if (Debug.isSet("Xdeveloper")) {
        StringWriter sw = new StringWriter(10000);
        e.printStackTrace(new PrintWriter(sw));
        ta.setText(sw.toString());
      } else {
        ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
      }
      return false;
    }
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }

  public void compare(Object o) {
    String command = (String) o;
    StringTokenizer stoke = new StringTokenizer(command);
    List<String> list = new ArrayList<>();

    while (stoke.hasMoreTokens()) {
      list.add(stoke.nextToken());
    }

    try {
      String unitS1 = list.get(0);
      String unitS2 = list.get(1);
      SimpleUnit su1 = SimpleUnit.factoryWithExceptions(unitS1);
      SimpleUnit su2 = SimpleUnit.factoryWithExceptions(unitS2);
      ta.setText(
          "<" + su1 + "> isConvertable to <" + su2 + ">=" + SimpleUnit.isCompatibleWithExceptions(unitS1, unitS2));

    } catch (Exception e) {
      if (Debug.isSet("Xdeveloper")) {
        StringWriter sw = new StringWriter(10000);
        e.printStackTrace(new PrintWriter(sw));
        ta.setText(sw.toString());
      } else {
        ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
      }
    }
  }

  private void checkUdunits(Object o) {
    String command = (String) o;

    boolean isDate = false;
    try {
      Formatter f2 = new Formatter();
      Optional<CalendarDateUnit> cduO = CalendarDateUnit.fromUdunitString(null, command);
      if (cduO.isEmpty()) {
        f2.format("CalendarDateUnit.fromUdunitString cant parse %s%n", command);
      } else {
        CalendarDateUnit cdu = cduO.get();
        f2.format("%nFrom udunits: '%s' CalendarDateUnit = '%s'%n", command, cdu);
        f2.format("getBaseCalendarDate = %s%n", CalendarDateFormatter.toDateTimeString(cdu.getBaseDateTime()));
      }

      Optional<CalendarDate> cdO = CalendarDate.fromUdunitIsoDate(null, command);
      if (cdO.isEmpty()) {
        f2.format("CalendarDate.fromUdunitIsoDate cant parse %s%n", command);
      } else {
        f2.format("CalendarDate.fromUdunitIsoDate = %s%n", CalendarDateFormatter.toDateTimeString(cdO.get()));
        isDate = true;
      }
      ta.appendLine(f2.toString());
    } catch (Exception e) {
      // ok to fall through
    }
    ta.appendLine("isDate = " + isDate);

    if (!isDate) {
      try {
        SimpleUnit su = SimpleUnit.factory(command);
        boolean isTime = su instanceof TimeUnit;
        ta.setText("<" + command + "> isTimeUnit= " + isTime);
        if (isTime) {
          TimeUnit du = (TimeUnit) su;
          ta.appendLine("\nTimeUnit = " + du);
        }
      } catch (Exception e) {
        if (Debug.isSet("Xdeveloper")) {
          StringWriter sw = new StringWriter(10000);
          e.printStackTrace(new PrintWriter(sw));
          ta.setText(sw.toString());
        } else {
          ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
        }
      }
    }
  }

  /*
   * private void checkCalendarDate(Object o) {
   * String command = (String) o;
   * 
   * try {
   * ta.setText("\nParse CalendarDate: <" + command + ">\n");
   * CalendarDate cd = CalendarDate.parseUdunits(null, command);
   * ta.appendLine("CalendarDate = " + cd);
   * } catch (Throwable t) {
   * ta.appendLine("not a CalendarDateUnit= " + t.getMessage());
   * }
   * 
   * try {
   * ta.appendLine("\nParse CalendarDateUnit: <" + command + ">\n");
   * 
   * CalendarDateUnit cdu = CalendarDateUnit.of(null, command);
   * ta.appendLine("CalendarDateUnit = " + cdu);
   * ta.appendLine(" Calendar        = " + cdu.getCalendar());
   * ta.appendLine(" PeriodField     = " + cdu.getCalendarPeriod().getField());
   * ta.appendLine(" PeriodValue     = " + cdu.getCalendarPeriod().getValue());
   * ta.appendLine(" Base            = " + cdu.getBaseCalendarDate());
   * ta.appendLine(" isCalendarField = " + cdu.isCalendarField());
   * } catch (Exception e) {
   * ta.appendLine("not a CalendarDateUnit= " + e.getMessage());
   * 
   * try {
   * String[] s = command.split("%");
   * if (s.length == 2) {
   * double val = Double.parseDouble(s[0].trim());
   * ta.appendLine("\nval= " + val + " unit=" + s[1]);
   * CalendarDateUnit cdu = CalendarDateUnit.of(null, s[1].trim());
   * ta.appendLine("CalendarDateUnit= " + cdu);
   * CalendarDate cd = cdu.makeCalendarDate(val);
   * ta.appendLine(" CalendarDate = " + cd);
   * Date d = cd.toDate();
   * ta.appendLine(" Date.toString() = " + d);
   * ta.appendLine(" DateFormatter= " + CalendarDateFormatter.toDateTimeString(cd.toDate()));
   * }
   * } catch (Exception ee) {
   * ta.appendLine("Failed on CalendarDateUnit " + ee.getMessage());
   * }
   * }
   * }
   */
}
