/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import org.junit.Test;
import ucar.array.ArrayType;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link IospArrayHelper} */
public class TestIospArrayHelper {

  @Test
  public void testMakePrimitiveArray() {
    Object sresult = IospArrayHelper.makePrimitiveArray(10, ArrayType.STRING);
    String[] stringFill = (String[]) sresult;
    for (String s : stringFill) {
      assertThat(s).isEqualTo(null);
    }
  }

  @Test
  public void testMakePrimitiveArrayWithFill() {
    assertThat(IospArrayHelper.makePrimitiveArray(11, ArrayType.OPAQUE, 22l)).isEqualTo(new byte[11]);

    Object sresult = IospArrayHelper.makePrimitiveArray(10, ArrayType.STRING, "fill");
    String[] stringFill = (String[]) sresult;
    for (String s : stringFill) {
      assertThat(s).isEqualTo("fill");
    }

    Object stresult = IospArrayHelper.makePrimitiveArray(10, ArrayType.STRUCTURE, new byte[] {1, 2, 3});
    byte[] fill = (byte[]) stresult;
    for (int i = 0; i < 10; i++) {
      assertThat(fill[i]).isEqualTo(i < 3 ? i + 1 : 0);
    }
  }

}
