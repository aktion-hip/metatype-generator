# Metatype Generator

Helper project to generate the metatype XML file from an annotated interface (using `org.osgi.service.metatype.annotations`).

Steps:

1. Clone this project.
1. Create a configuration interface (`public @interface Configuration {}`) somewhere in `/metatype-configuration/src/main/java`.
1. Annotate your configuration interface with `@ObjectClassDefinition` and `@AttributeDefinition`s.
1. Run `mvn clean install`.

This will generated both the metatype XML file in `/metatype-configuration/OSGI-INF/metatype` and a Java component 
referencing this metatype configuration. The generated Java class is placed beside of your configuration interface.

## Example:

The configuration interface *org.elbe.meta.example.Configuration*:

```
@ObjectClassDefinition(id = "org.elbe.metatype.config", name = "Example Configuration", description = "An OSGi metatype configuration example.")
public @interface Configuration {
    public static final String PREFIX_ = "org.elbe.";

    @AttributeDefinition(name = "User name", description = "The type will be derived from the return type.")
    String user();

    @AttributeDefinition(name = "Password", description = "For a password field, we have to specify the type!", type = AttributeType.PASSWORD)
    String user_password();

    @AttributeDefinition(name = "Language", description = "Some options to choose one.", options = {
            @Option(label = "english", value = "en"),
            @Option(label = "deutsch", value = "de") })
    String frontend_language();

    @AttributeDefinition(name = "Values", description = "This configuration returns an array of integers.", cardinality = 5)
    int[] someValues();

    @AttributeDefinition(name = "Endpoints", description = "To configure an array of endpoints.", cardinality = 10)
    String[] endpoints();
}
```

will generate the metatype XML */OSGI-INF/metatype/configuration.xml*:

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<metatype:MetaData xmlns:metatype="http://www.osgi.org/xmlns/metatype/v1.4.0">
    <OCD description="An OSGi metatype configuration example." id="org.elbe.metatype.config" name="Example Configuration">
        <AD description="The type will be derived from the return type." id="org.elbe.user" name="User name" type="String"/>
        <AD description="For a password field, we have to specify the type!" id="org.elbe.user.password" name="Password" type="Password"/>
        <AD description="Some options to choose one." id="org.elbe.frontend.language" name="Language" type="String">
            <Option label="english" value="en"/>
            <Option label="deutsch" value="de"/>
        </AD>
        <AD cardinality="5" description="This configuration returns an array of integers." id="org.elbe.someValues" name="Values" type="Integer"/>
        <AD cardinality="10" description="To configure an array of endpoints." id="org.elbe.endpoints" name="Endpoints" type="String"/>
    </OCD>
    <Designate pid="org.elbe.metatype.config">
        <Object ocdref="org.elbe.metatype.config"/>
    </Designate>
</metatype:MetaData>
```

and the Java class *org.elbe.meta.example.AppConfiguration*:

```
package org.elbe.meta.example;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/** Generated */
@Component(immediate = true, configurationPid = AppConfiguration.CONFIG_PID, service = { AppConfiguration.class })
public class AppConfiguration {
    public static final String CONFIG_PID = "org.elbe.metatype.config";

    private String user = "";
    private String userPassword = "";
    private String frontendLanguage = "";
    private int[] someValues;
    private String[] endpoints;

    @Activate
    @Modified
    protected void activate(final Map<String, Object> configuration) {
        if (configuration != null) {
            this.user = (String) configuration.getOrDefault("org.elbe.user", null);
            this.userPassword = (String) configuration.getOrDefault("org.elbe.user.password", null);
            this.frontendLanguage = (String) configuration.getOrDefault("org.elbe.frontend.language", null);
            this.someValues = (int[]) configuration.getOrDefault("org.elbe.someValues", null);
            this.endpoints = (String[]) configuration.getOrDefault("org.elbe.endpoints", null);
        }
    }

    public String getUser() {
        return user;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getFrontendLanguage() {
        return frontendLanguage;
    }

    public int[] getSomeValues() {
        return someValues;
    }

    public String[] getEndpoints() {
        return endpoints;
    }
}
```

