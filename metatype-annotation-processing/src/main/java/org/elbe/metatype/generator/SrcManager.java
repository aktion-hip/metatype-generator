/**
 *
 */
package org.elbe.metatype.generator;

/**
 * @author lbenno
 *
 */
public class SrcManager {
    private static final String TMPL = "package %2$s;\r\n"
            + "\r\n"
            + "import java.util.Map;\r\n"
            + "\r\n"
            + "import org.osgi.service.component.annotations.Activate;\r\n"
            + "import org.osgi.service.component.annotations.Component;\r\n"
            + "import org.osgi.service.component.annotations.Modified;\r\n"
            + "\r\n"
            + "/** Generated */\r\n"
            + "@Component(immediate = true, configurationPid = %1$s.CONFIG_PID, service = { %1$s.class })\r\n"
            + "public class %1$s {\r\n"
            + "    public static final String CONFIG_PID = \"%3$s\";\r\n"
            + "\r\n"
            + "%5$s\r\n"
            + "\r\n"
            + "    @Activate\r\n"
            + "    @Modified\r\n"
            + "    protected void activate(final Map<String, Object> configuration) {\r\n"
            + "        if (configuration != null) {\r\n"
            + "%4$s\r\n"
            + "        }\r\n"
            + "    }"
            + "\r\n"
            + "%6$s\r\n"
            + "\r\n"
            + "}";

    private final AnnotationManager manager;

    /** @param manager */
    public SrcManager(final AnnotationManager manager) {
        this.manager = manager;
    }

    /** Creates the content of the generated configuration component.
     *
     * @param pkgName String
     * @param pid String
     * @return String */
    public String create(final String pkgName, final String pid) {
        return String.format(TMPL, Constants.TARGET_CLASS_NAME, pkgName, pid, this.manager.getActivatePart(),
                this.manager.getFieldDefs(), this.manager.createGetters());
    }

}
