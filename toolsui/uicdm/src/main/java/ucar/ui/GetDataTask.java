/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui;

import ucar.nc2.util.CancelTask;
import ucar.ui.widget.ProgressMonitorTask;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class GetDataTask extends ProgressMonitorTask implements CancelTask {
  private final GetDataRunnable getData;
  private final Object o;
  private final String name;
  private String errMsg;

  public GetDataTask(GetDataRunnable getData, String name, Object o) {
    this.getData = getData;
    this.name = name;
    this.o = o;
  }

  public void run() {
    try {
      getData.run(o);
    } catch (FileNotFoundException ioe) {
      errMsg = ("Cant open " + name + " " + ioe.getMessage());
      // ioe.printStackTrace();
      success = false;
      done = true;
      return;
    } catch (Exception e) {
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      errMsg = sw.toString();
      success = false;
      done = true;
      return;
    }

    success = true;
    done = true;
  }

  public String getErrorMessage() {
    return errMsg;
  }
}
