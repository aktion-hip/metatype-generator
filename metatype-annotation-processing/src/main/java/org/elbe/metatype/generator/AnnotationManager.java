/**
 *
 */
package org.elbe.metatype.generator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Option;
import org.w3c.dom.Document;

/** Helper class managing all information about the class (i.e. annotation) annotated with
 * <code>@ObjectClassDefinition</code> and <code>@AttributeDefinition</code>.
 *
 * @author lbenno */
public class AnnotationManager {
    private static final String INDENT1 = String.format("%4s", " ");
    private static final String INDENT3 = String.format("%12s", " ");
    private static final String PREFIX_FLD = "PREFIX_";
    private static final String GETTER_TMPL = "\r\n    public %s get%s() {\r\n"
            + "        return %s;\r\n"
            + "    }";
    private static Map<String, AttributeType> typeMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<String, AttributeType>(String.class.getName(), AttributeType.STRING),
            new AbstractMap.SimpleEntry<String, AttributeType>("long", AttributeType.LONG),
            new AbstractMap.SimpleEntry<String, AttributeType>(Long.class.getName(), AttributeType.LONG),
            new AbstractMap.SimpleEntry<String, AttributeType>("int", AttributeType.INTEGER),
            new AbstractMap.SimpleEntry<String, AttributeType>(Integer.class.getName(), AttributeType.INTEGER),
            new AbstractMap.SimpleEntry<String, AttributeType>("short", AttributeType.SHORT),
            new AbstractMap.SimpleEntry<String, AttributeType>(Short.class.getName(), AttributeType.SHORT),
            new AbstractMap.SimpleEntry<String, AttributeType>("char", AttributeType.CHARACTER),
            new AbstractMap.SimpleEntry<String, AttributeType>(Character.class.getName(), AttributeType.CHARACTER),
            new AbstractMap.SimpleEntry<String, AttributeType>("byte", AttributeType.BYTE),
            new AbstractMap.SimpleEntry<String, AttributeType>(Byte.class.getName(), AttributeType.BYTE),
            new AbstractMap.SimpleEntry<String, AttributeType>("double", AttributeType.DOUBLE),
            new AbstractMap.SimpleEntry<String, AttributeType>(Double.class.getName(), AttributeType.DOUBLE),
            new AbstractMap.SimpleEntry<String, AttributeType>("float", AttributeType.FLOAT),
            new AbstractMap.SimpleEntry<String, AttributeType>(Float.class.getName(), AttributeType.FLOAT),
            new AbstractMap.SimpleEntry<String, AttributeType>("boolean", AttributeType.BOOLEAN),
            new AbstractMap.SimpleEntry<String, AttributeType>(Boolean.class.getName(), AttributeType.BOOLEAN));

    private final String className;
    private final List<MethodData> methods = new ArrayList<>();

    private final Types types;
    private final TypeMirror stringType;
    private String prefix = "";

    /** AnnotationManager constructor.
     *
     * @param annotatedClass {@link Element} the class to process
     * @param processingEnv {@link ProcessingEnvironment} */
    public AnnotationManager(final Element annotatedClass, final ProcessingEnvironment processingEnv) {
        this.types = processingEnv.getTypeUtils();
        this.stringType = processingEnv.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType();
        this.className = annotatedClass.getSimpleName().toString();

        final List<? extends Element> enclosed = annotatedClass.getEnclosedElements();
        for (final Element element : enclosed) {
            if (element.getKind().isField() && element.getSimpleName().toString().equals(PREFIX_FLD)) {
                this.prefix = processPrefix(element);
            } else if (element.getKind() == ElementKind.METHOD) {
                processMethod(element);
            }
        }
    }

    private boolean isSingleElementAnnotation() {
        if (this.methods.size() == 1) {
            return "value".equalsIgnoreCase(this.methods.get(0).name);
        }
        return false;
    }

    /** Process the methods annotated with <code>ttributeDefinition</code>.
     *
     * @param doc {@link Document}
     * @param parent {@link org.w3c.dom.Element} the parent to append the created <code>AD</code> elements */
    public void processAttributeDefinitions(final Document doc, final org.w3c.dom.Element parent) {
        for (final MethodData method : this.methods) {
            final org.w3c.dom.Element ad = doc.createElement("AD");
            parent.appendChild(ad);

            final String adId = this.prefix +
                    (isSingleElementAnnotation() ? IdUtil.createId(this.className) : IdUtil.toId(method.name));
            method.setId(adId);
            ad.setAttribute("id", adId);
            setAttributeChecked(ad, "name", method.ad.name());
            setAttributeChecked(ad, "description", method.ad.description());
            setAttributeChecked(ad, "default", arrayToString(method.ad.defaultValue()));
            setAttributeChecked(ad, "min", method.ad.min());
            setAttributeChecked(ad, "max", method.ad.max());
            if (!method.ad.required()) {
                setAttributeChecked(ad, "required", "false");
            }
            setAttributeChecked(ad, "cardinality", toString(method.ad.cardinality(), 0));
            if (isSpecialType(method)) {
                ad.setAttribute("type",
                        typeMap.getOrDefault(normalize(method.returnType), AttributeType.STRING).toString());
            } else {
                ad.setAttribute("type", method.ad.type().toString());
            }
            final Option[] options = method.ad.options();
            if (options.length > 0) {
                process(ad, options, doc);
            }
        }
    }

    private String normalize(final TypeMirror returnType) {
        return returnType.toString().replace("[]", "");
    }

    private boolean isSpecialType(final MethodData method) {
        if (method.ad.type() == AttributeType.STRING) {
            return !isString(method.returnType);
        }
        return false;
    }

    private boolean isString(final TypeMirror type) {
        return this.types.isSameType(type, this.stringType);
    }

    private void process(final org.w3c.dom.Element ad, final Option[] options, final Document doc) {
        for (final Option option : options) {
            final org.w3c.dom.Element opt = doc.createElement("Option");
            ad.appendChild(opt);
            opt.setAttribute("value", option.value());
            opt.setAttribute("label", option.label());
        }
    }

    private void setAttributeChecked(final org.w3c.dom.Element ad, final String name, final String value) {
        if (value != null && !value.isBlank()) {
            ad.setAttribute(name, value);
        }
    }

    private String arrayToString(final String[] values) {
        return Arrays.stream(values).collect(Collectors.joining(", "));
    }

    private String toString(final int value, final int dft) {
        return value == dft ? "" : String.valueOf(Math.abs(value));
    }

    private void processMethod(final Element element) {
        final AttributeDefinition adAnnotation = element.getAnnotation(AttributeDefinition.class);
        if (adAnnotation != null) {
            this.methods.add(new MethodData(element.getSimpleName().toString(), adAnnotation,
                    ((ExecutableElement) element).getReturnType()));
        }
    }

    private String processPrefix(final Element element) {
        if (element instanceof VariableElement) {
            return ((VariableElement) element).getConstantValue().toString();
        }
        return "";
    }

    /** Creates the part of the code for the <code>activate</code> method.
     *
     * @return String */
    public String getActivatePart() {
        return this.methods.stream()
                .map(m -> String.format("%sthis.%s = (%s) configuration.getOrDefault(\"%s\", null);", INDENT3,
                        IdUtil.toFieldName(m.name), toSimpleName(m.returnType), m.adId))
                .collect(Collectors.joining("\n"));
    }

    /** Creates the part of the code to define the fields.
     *
     * @return String */
    public String getFieldDefs() {
        return this.methods.stream()
                .map(m -> {
                    final String addition = isString(m.returnType) ? " = \"\"" : "";
                    return String.format("%sprivate %s %s%s;", INDENT1, toSimpleName(m.returnType),
                            IdUtil.toFieldName(m.name), addition);
                })
                .collect(Collectors.joining("\n"));
    }

    public String createGetters() {
        return this.methods.stream().map(m -> {
            final String name = IdUtil.toFieldName(m.name);
            return String.format(GETTER_TMPL, toSimpleName(m.returnType),
                    name.substring(0, 1).toUpperCase() + name.substring(1), name);
        }).collect(Collectors.joining("\n"));
    }

    private String toSimpleName(final TypeMirror type) {
        if (isString(type)) {
            return "String";
        }
        final String typeName = type.toString();
        return typeName.endsWith("String[]") ? "String[]" : typeName;
    }

    // ---

    private final static class MethodData {
        private final String name;
        private final AttributeDefinition ad;
        private final TypeMirror returnType;

        private String adId = "";

        protected MethodData(final String name, final AttributeDefinition ad, final TypeMirror returnType) {
            this.name = name;
            this.ad = ad;
            this.returnType = returnType;
        }

        protected void setId(final String adId) {
            this.adId = adId;
        }
    }

}
