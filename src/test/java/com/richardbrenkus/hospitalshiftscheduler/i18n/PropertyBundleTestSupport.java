package com.richardbrenkus.hospitalshiftscheduler.i18n;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

final class PropertyBundleTestSupport {

    private PropertyBundleTestSupport() {
    }

    static Properties loadProperties(String classpathResource) {
        try (var inputStream = PropertyBundleTestSupport.class.getClassLoader().getResourceAsStream(classpathResource)) {

            assertThat(inputStream).as("Classpath resource '%s' must exist", classpathResource).isNotNull();

            Properties properties = new Properties();

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load classpath resource: " + classpathResource, exception);
        }
    }

    static void assertSameKeys(String baseResource, String localizedResource) {
        Properties base = loadProperties(baseResource);
        Properties localized = loadProperties(localizedResource);

        assertThat(localized.stringPropertyNames())
                .as("Keys in '%s' must exactly match '%s'", localizedResource, baseResource)
                .containsExactlyInAnyOrderElementsOf(base.stringPropertyNames());
    }

    static void assertNoBlankValues(String resource) {
        Properties properties = loadProperties(resource);

        assertThat(properties.stringPropertyNames())
                .as("Bundle '%s' must not contain blank values", resource)
                .allSatisfy(key -> assertThat(properties.getProperty(key)).as("Value of key '%s' in '%s'", key, resource).isNotBlank());
    }

    static String expectedValue(String resource, String key) {
        Properties properties = loadProperties(resource);

        assertThat(properties).as("Bundle '%s' must contain key '%s'", resource, key).containsKey(key);

        return properties.getProperty(key);
    }

    static void assertResourceDoesNotExist(String classpathResource) {
        assertThat(PropertyBundleTestSupport.class.getClassLoader().getResource(classpathResource))
                .as("Redundant resource '%s' should not exist", classpathResource)
                .isNull();
    }
}
