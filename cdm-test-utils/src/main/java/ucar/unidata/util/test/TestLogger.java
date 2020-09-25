package ucar.unidata.util.test;

import java.util.Stack;

public class TestLogger {
  private Stack<String> logMessages;

  public TestLogger() {
    this.logMessages = new Stack<String>();
  }

  public void log(String msg) {
    this.logMessages.push(msg);
  }

  public void log(String msg, Throwable t) {
    this.logMessages.push(msg);
  }

  public String getLastLogMsg() {
    return this.logMessages.peek();
  }

  public int getLogSize() {
    return this.logMessages.size();
  }

  public void clearLog() {
    this.logMessages.clear();
  }



  public static class TestLoggerFactory {
    public static TestLogger getLogger() {
      return new TestLogger();
    }
  }

}
