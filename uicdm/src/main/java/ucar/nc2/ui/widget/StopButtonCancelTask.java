package ucar.nc2.ui.widget;

import ucar.nc2.util.CancelTask;
import ucar.ui.widget.StopButton;

public class StopButtonCancelTask extends StopButton implements CancelTask {
  private boolean done = false;

  @Override
  public boolean isDone() {
    return done;
  }

  @Override
  public void setDone(boolean done) {
    this.done = done;
  }
}
