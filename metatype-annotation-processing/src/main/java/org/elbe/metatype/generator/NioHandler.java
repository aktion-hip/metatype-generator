/**
 *
 */
package org.elbe.metatype.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/** File and directory handling.
 *
 * @author lbenno */
public class NioHandler {
    private static final Logger LOG = Logger.getLogger(NioHandler.class.getName());

    private static final String FILE_SEP = System.getProperty("file.separator");
    private static final String CONFIG_PROJ = "metatype-configuration";
    private static final String TARGET = "OSGI-INF/metatype";
    private static final String GENERATED = "target/generated-sources/annotations";
    private static final String SRC = "src/main/java";

    private final Optional<Path> parent;
    private final Path generated;
    private final Path src;

    /** NioHandler constructor. */
    public NioHandler() {
        final Path root = Paths.get("").toAbsolutePath();
        final Path proj = root.resolve(CONFIG_PROJ);
        this.parent = prepare(proj);
        this.generated = proj.resolve(GENERATED);
        this.src = proj.resolve(SRC);
    }

    /** @return {@link File} the parent directory where the metatype XML should be created */
    public File getParent() {
        return this.parent.orElseGet(() -> Paths.get("").toAbsolutePath()).toFile();
    }

    private Optional<Path> prepare(final Path root) {
        final Path targetPath = root.resolve(TARGET);
        if (Files.exists(targetPath)) {
            try {
                if (deleteContent(targetPath)) {
                    Files.deleteIfExists(targetPath);
                    return Optional.ofNullable(Files.createDirectories(targetPath));
                }
            } catch (final IOException exc) {
                LOG.log(Level.SEVERE, "Unable to delete the target directory!", exc);
            }
        } else {
            try {
                return Optional.ofNullable(Files.createDirectories(targetPath));
            } catch (final IOException exc) {
                LOG.log(Level.SEVERE, "Unable to create the target directory!", exc);
            }
        }
        return Optional.empty();
    }

    private static boolean deleteContent(final Path targetPath) throws IOException {
        try (Stream<Path> walk = Files.walk(targetPath)) {
            walk.map(Path::toFile).forEach(File::delete);
        }
        return true;
    }

    /** Copy the code of the generated class to the source code directory.
     *
     * @param javaGenerated String the name of the generated java file */
    public void copyCode(final TypeElement annotated) {
        final Element pkg = annotated.getEnclosingElement();
        if (pkg instanceof PackageElement) {
            final String qualified = ((PackageElement) pkg).getQualifiedName().toString().replace(".", FILE_SEP)
                    + FILE_SEP + Constants.TARGET_CLASS_NAME + Constants.JAVA_EXT;
            final Path source = this.generated.resolve(Constants.TARGET_TMP_NAME + Constants.JAVA_EXT);
            try {
                Files.copy(source, this.src.resolve(qualified), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException exc) {
                LOG.log(Level.SEVERE, "Unable to copy the generated java class!", exc);
            }
        }
    }

}
