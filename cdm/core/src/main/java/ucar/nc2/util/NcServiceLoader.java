package ucar.nc2.util;

import java.util.ServiceLoader;

/**
 * Centralized access to the jdk's built in {@link ServiceLoader} utility to support using the NetCDF library in
 * applications with non-trivial class loading requirements.
 */
public class NcServiceLoader {

  private static ClassLoader classLoader;

  public static <S> ServiceLoader<S> load(Class<S> service) {
    ClassLoader useClassLoader;

    if (classLoader == null) {
      useClassLoader = Thread.currentThread().getContextClassLoader();
    } else {
      useClassLoader = classLoader;
    }

    return ServiceLoader.load(service, useClassLoader);
  }

  /**
   * Services will be loaded using whatever happens to be the current thread's context class loader at the time
   * services are loaded. This is the default behaviour and will work for applications that use the NetCDF library
   * without any special class-loading requirements.
   */
  public static void useContextClassLoader() {
    classLoader = null;
  }

  /**
   * Services will be loaded using the same classloader that loaded this class, assumed to be
   * the same one that will load the rest of the netcdf library. This option is suitable for applications that
   * use only the built-in functionality, i.e they do not extend the library via the {@link ServiceLoader} mechanism
   */
  public static void usePackageClassLoader() {
    classLoader = NcServiceLoader.class.getClassLoader();
  }

  /**
   * Services will be loaded using a specific custom class loader. This class loader should 'descend' from the one that
   * loaded the NetCDF library code, otherwise any services loaded are likely to throw {@link ClassCastException}s
   */
  public static void useCustomClassLoader(ClassLoader useClassLoader) {
    classLoader = useClassLoader;
  }
}
