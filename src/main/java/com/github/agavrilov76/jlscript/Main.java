package com.github.agavrilov76.jlscript;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class Main {
  public static void main(String[] args)
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InvocationTargetException,
          IllegalAccessException {
    final var tmp = Files.createTempDirectory("out");
    final var sourcePath = tmp.resolve("Generated.java");

    final var template =
        """
      package com.github.agavrilov76.jlscript.generated;

      public class Generated {
        public static void eval(String value) {
         System.out.println("%s -- " + value);
        }
      }
      """;

    final var source = template.formatted("The time is: " + Instant.now());
    Files.writeString(sourcePath, source);

    final var compiler = ToolProvider.getSystemJavaCompiler();

    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(tmp.toFile()));
      fileManager.setLocationFromPaths(
          StandardLocation.CLASS_PATH,
          List.of(
              Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())));

      final var sources = fileManager.getJavaFileObjectsFromFiles(List.of(sourcePath.toFile()));
      final var writer = new StringWriter();
      final var task = compiler.getTask(writer, fileManager, null, null, null, sources);

      final var ok = task.call();
      if (!ok) {
        throw new RuntimeException("Compilation failed");
      }

      try (var classLoader =
          new URLClassLoader(new URL[] {tmp.toUri().toURL()}, Main.class.getClassLoader())) {
        final var compiledType =
            Class.forName("com.github.agavrilov76.jlscript.generated.Generated", true, classLoader);
        final var method = compiledType.getMethod("eval", String.class);
        method.invoke(null, "!!!!");
      }
    } finally {
      // TODO: delete directtmp
    }
  }
}
