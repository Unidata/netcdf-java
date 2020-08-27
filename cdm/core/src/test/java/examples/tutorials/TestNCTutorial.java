package examples.tutorials;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class TestNCTutorial {

  // path to a netcdf file
  private static String testDataPathStr = "../../cdm-test/src/test/data/thredds/public/testdata/testData.nc";
  // path to java file with tutorial code snippets
  private static String testClass = "src/test/java/examples/tutorials/NCTutorial.java";

  @Test
  public void testOpenNCFileTutorial() {
    // test open success
    NCTutorial.logger.clearLog();
    NCTutorial.openNCFileTutorial(testDataPathStr);
    assertThat(NCTutorial.logger.getLogSize()).isEqualTo(0);

    // test open fail
    NCTutorial.openNCFileTutorial("");
    assertThat(NCTutorial.logger.getLastLogMsg()).isEqualTo(NCTutorial.yourOpenNetCdfFileErrorMsgTxt);
  }

  /**
   * Test that codes snippets included in documentation compile without errors or warnings
   */
  @Test
  public void testCompileNCTutorial() {
    // get compiler and diagnostics to check for compiler warnings
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    // get file to compile
    StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, null, null);
    Iterable<String> filenames = Arrays.asList(testClass);
    Iterable<? extends JavaFileObject> fileObjects = stdFileManager.getJavaFileObjectsFromStrings(filenames);

    // create file manager that writes compile class in memory
    ForwardingJavaFileManager fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(stdFileManager) {
      @Override
      public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
          FileObject sibling) throws IOException {
        return new JavaByteObject();
      }
    };

    // test successful compile
    assertThat(compiler.getTask(null, fileManager, diagnostics, null, null, null).call()).isEqualTo(true);

    // test no warnings on compile
    List<Diagnostic<? extends JavaFileObject>> compileOutput = diagnostics.getDiagnostics();
    for (Diagnostic d : compileOutput) {
      System.out.println(d.getKind() + ": " + d.getMessage(null));
    }
    assertThat(compileOutput.size()).isEqualTo(0);
  }

  /**
   * ByteArray object for the JavaCompiler to write compiled code in memory
   */
  private class JavaByteObject extends SimpleJavaFileObject {
    private ByteArrayOutputStream outputStream;

    protected JavaByteObject() {
      super(null, null);
      outputStream = new ByteArrayOutputStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
      return outputStream;
    }
  }
}
