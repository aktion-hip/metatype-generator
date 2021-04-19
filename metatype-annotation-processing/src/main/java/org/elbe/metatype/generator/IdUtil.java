/**
 *
 */
package org.elbe.metatype.generator;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/** Helper class to convert the method name to a valid AD id.
 *
 * @author lbenno */
public final class IdUtil {
    private static final char LOW = '_';
    private static final char DOLLAR = '$';

    private enum State {
        IN_LOW, IN_DOLLAR, NORMAL;
    }

    private enum SEAState {
        IN_LOW, IN_UP;
    }

    private IdUtil() {
        // prevent instantiation
    }

    /** Converts a method name to the <code>AD</code> attribute's id.<br>
     * According to the OSGi specification, the conversion is done as follows:<br>
     * The {@code id} of this AttributeDefinition is generated from the name of the annotated method as follows:
     * <ul>
     * <li>A single dollar sign ({@code '$'} &#92;u0024) is removed unless it is followed by:
     * <ul>
     * <li>A low line ({@code '_'} &#92;u005F) and a dollar sign in which case the three consecutive characters (
     * {@code "$_$"}) are changed to a single hyphen-minus ({@code '-'} &#92;u002D).</li>
     * <li>Another dollar sign in which case the two consecutive dollar signs ( {@code "$$"}) are changed to a single
     * dollar sign.</li>
     * </ul>
     * </li>
     * <li>A low line ({@code '_'} &#92;u005F) is changed to a full stop ( {@code '.'} &#92;u002E) unless is it followed
     * by another low line in which case the two consecutive low lines ({@code "__"}) are changed to a single low
     * line.</li>
     * <li>All other characters are unchanged.</li>
     * <li>If the type declaring the method also declares a {@code PREFIX_} field whose value is a compile-time constant
     * String, then the id is prefixed with the value of the {@code PREFIX_} field.</li>
     * </ul>
     *
     * @param name String the method name
     * @return String the converted name to be used as <code>id</code> attribute */
    public static String toId(final String name) {
        final Cursor cursor = new Cursor();
        final char[] chars = name.toCharArray();
        final StringBuilder target = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            target.append(cursor.process(chars[i]));
        }
        return target.toString();
    }

    /** This method creates an id from a class name.<br>
     * This is needed in case of a <code>single value annotation</code>:<br>
     * However, if the type annotated by {@link ObjectClassDefinition} is a <em>single-element annotation</em>, then the
     * id for the {@code value} method is derived from the name of the annotation type rather than the name of the
     * method. In this case, the simple name of the annotation type, that is, the name of the class without any package
     * name or outer class name, if the annotation type is an inner class, must be converted to the {@code value}
     * method's id as follows:
     * <ul>
     * <li>When a lower case character is followed by an upper case character, a full stop ({@code '.'} &#92;u002E) is
     * inserted between them.</li>
     * <li>Each upper case character is converted to lower case.</li>
     * <li>All other characters are unchanged.</li> *
     *
     * @param className String
     * @return String */
    public static String createId(final String className) {
        final SEACursor cursor = new SEACursor();
        final char[] chars = className.toCharArray();
        final StringBuilder target = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            target.append(cursor.process(chars[i]));
        }
        return target.toString();
    }

    /** Converts an AD id to a suitable field name.<br>
     * First character is lowercase, a <code>_c</code> is converted to <code>C</code>.
     *
     * @param id String the id to convert
     * @return String the converted string */
    public static String toFieldName(final String id) {
        final String[] parts = id.split("_");
        final String capitalized = Arrays.stream(parts).map(p -> capialize(p)).collect(Collectors.joining());
        return capitalized.substring(0, 1).toLowerCase() + capitalized.substring(1);
    }

    private static String capialize(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // ---

    private static class SEACursor {
        private SEAState state = SEAState.IN_UP;

        protected String process(final char c) {
            if (this.state == SEAState.IN_UP) {
                return processUp(c);
            }
            return processLow(c);
        }

        private String processLow(final char c) {
            if (Character.isUpperCase(c)) {
                this.state = SEAState.IN_UP;
                return "." + Character.toLowerCase(c);
            }
            return String.valueOf(c);
        }

        private String processUp(final char c) {
            if (Character.isUpperCase(c)) {
                return String.valueOf(Character.toLowerCase(c));
            }
            this.state = SEAState.IN_LOW;
            return String.valueOf(c);
        }

    }

    private static class Cursor {
        private State state = State.NORMAL;
        private String memory = "";

        protected String process(final char c) {
            if (this.state == State.NORMAL) {
                return processNormal(c);
            } else if (this.state == State.IN_DOLLAR) {
                return processInDollar(c);
            } else if (this.state == State.IN_LOW) {
                return processInLow(c);
            }
            return String.valueOf(c);
        }

        private String processInLow(final char c) {
            this.state = State.NORMAL;
            this.memory = "";
            if (c == LOW) {
                return "_";
            }
            return "." + c;
        }

        private String processInDollar(final char c) {
            this.state = State.NORMAL;
            if (c == DOLLAR) {
                final String old_memory = this.memory;
                this.memory = "";
                return "$_".equals(old_memory) ? "-" : String.valueOf(c);
            } else if (c == LOW) {
                this.state = State.IN_DOLLAR;
                this.memory += c;
                return "";
            }
            this.memory = "";
            return String.valueOf(c);
        }

        private String processNormal(final char c) {
            if (c == DOLLAR) {
                this.state = State.IN_DOLLAR;
                this.memory += c;
                return "";
            } else if (c == LOW) {
                this.state = State.IN_LOW;
                this.memory += c;
                return "";
            }
            return String.valueOf(c);
        }

    }

}
