package ucar.ui.table;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.table.DefaultTableModel;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by cwardgar on 1/9/14.
 */
public class HidableTableColumnModelTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testTableModelResize() {
    DefaultTableModel model = new DefaultTableModel(6, 6);
    HidableTableColumnModel tcm = new HidableTableColumnModel(model);

    // At start numAllColumns == numVisibleColumns.
    assertThat(tcm.getColumnCount(false)).isEqualTo(tcm.getColumnCount(true));

    tcm.setColumnVisible(tcm.getColumn(1, false), false); // Remove column at modelIndex 1.
    tcm.setColumnVisible(tcm.getColumn(4, false), false); // Remove column at modelIndex 4.

    // We've removed 2 columns.
    assertThat(tcm.getColumnCount(false) - 2).isEqualTo(tcm.getColumnCount(true));

    model.setColumnCount(10);
    assertThat(10).isEqualTo(tcm.getColumnCount(true));

    /*
     * This assertion failed in the original source code of XTableColumnModel.
     * From http://www.stephenkelvin.de/XTableColumnModel/:
     * "There is one gotcha with this design: If you currently have invisible columns and change your table
     * model the JTable will recreate columns, but will fail to remove any invisible columns."
     */
    assertThat(10).isEqualTo(tcm.getColumnCount(false));
  }
}
