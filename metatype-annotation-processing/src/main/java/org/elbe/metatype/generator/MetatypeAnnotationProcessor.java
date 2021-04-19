/**
 *
 */
package org.elbe.metatype.generator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.w3c.dom.Document;

import com.google.auto.service.AutoService;

/** The processor for the <code>org.osgi.service.metatype.annotations</code> annotations.
 *
 * @see http://hannesdorfmann.com/annotation-processing/annotationprocessing101/
 * @author lbenno */
@SupportedAnnotationTypes("org.osgi.service.metatype.annotations.ObjectClassDefinition")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class MetatypeAnnotationProcessor extends AbstractProcessor {
    private static final Logger LOG = Logger.getLogger(MetatypeAnnotationProcessor.class.getName());

    private ProcessingEnvironment processingEnv;
    private NioHandler root;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.root = new NioHandler();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (final TypeElement annotation : annotations) {
            final Element configuration = getConfiguration(roundEnv.getElementsAnnotatedWith(annotation));
            if (configuration != null) {
                try {
                    final String className = configuration.getSimpleName().toString();
                    final AnnotationManager manager = new AnnotationManager(configuration, this.processingEnv);

                    // create metatype.xml
                    final XmlHandler xml = new XmlHandler();
                    final ObjectClassDefinition ocd = configuration.getAnnotation(ObjectClassDefinition.class);
                    final Document metatypeXML = xml.process(ocd,
                            roundEnv.getElementsAnnotatedWith(AttributeDefinition.class), manager);
                    if (metatypeXML != null) {
                        xml.write(metatypeXML, this.root.getParent(), className);
                    }

                    // create java class (@Component) using the metatype.xml
                    final FileObject builderFile = this.processingEnv.getFiler()
                            .createResource(StandardLocation.SOURCE_OUTPUT, "",
                                    Constants.TARGET_TMP_NAME + Constants.JAVA_EXT);
                    try (PrintWriter srcWriter = new PrintWriter(builderFile.openWriter())) {
                        final SrcManager src = new SrcManager(manager);
                        srcWriter.print(src
                                .create(this.processingEnv.getElementUtils().getPackageOf(configuration).toString(),
                                        ocd.id()));
                    }
                    this.root.copyCode((TypeElement) configuration);
                    return true;
                } catch (final ParserConfigurationException | TransformerException | IOException exc) {
                    LOG.log(Level.SEVERE, "Unable to create the metadata XML!", exc);
                }
            }
        }
        return false;
    }

    private Element getConfiguration(final Set<? extends Element> configurations) {
        if (configurations.size() == 1) {
            return configurations.iterator().next();
        }
        return null;
    }

}
