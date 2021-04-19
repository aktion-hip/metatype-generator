/**
 *
 */
package org.elbe.metatype.generator;

import java.io.File;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** This class creates the XML document <code>configuration.xml</code>.
 *
 * @author lbenno */
public class XmlHandler {
    private static final String NS = "http://www.osgi.org/xmlns/metatype/v1.4.0";

    private final Document doc;

    /** XmlHandler constructor.
     *
     * @throws ParserConfigurationException */
    public XmlHandler() throws ParserConfigurationException {
        this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    /** Writes the specified XML to the passed location.
     *
     * @param doc {@link Document}
     * @param parent File the location in the file system
     * @param name String the file name
     * @return boolean
     * @throws TransformerException */
    public boolean write(final Document doc, final File parent, final String name) throws TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(createTarget(parent, name)));
        return true;
    }

    private File createTarget(final File parent, final String name) {
        return new File(parent, String.format("%s.xml", name.toLowerCase()));
    }

    /** Processes the passed annotations and returns the XML.
     *
     * @param ocdAnnotation {@link ObjectClassDefinition}
     * @param methods Set&lt;javax.lang.model.element.Element> the annotated methods
     * @param className String the name of the configuration class/interface
     * @return {@link Document} the created XML */
    public Document process(final ObjectClassDefinition ocdAnnotation,
            final Set<? extends javax.lang.model.element.Element> methods, final AnnotationManager data) {
        final Element root = this.doc.createElementNS(NS, "metatype:MetaData");
        this.doc.appendChild(root);

        final Element ocd = this.doc.createElement("OCD");
        root.appendChild(ocd);
        ocd.setAttribute("id", ocdAnnotation.id());
        ocd.setAttribute("name", ocdAnnotation.name());
        ocd.setAttribute("description", ocdAnnotation.description());

        data.processAttributeDefinitions(this.doc, ocd);
        addDesignate(root, ocdAnnotation.id());
        return this.doc;
    }

    private void addDesignate(final Element root, final String id) {
        final Element designate = this.doc.createElement("Designate");
        root.appendChild(designate);
        designate.setAttribute("pid", id);

        final Element objEl = this.doc.createElement("Object");
        designate.appendChild(objEl);
        objEl.setAttribute("ocdref", id);
    }

}
