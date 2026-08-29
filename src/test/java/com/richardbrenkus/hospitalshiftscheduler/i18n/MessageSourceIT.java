package com.richardbrenkus.hospitalshiftscheduler.i18n;

import com.richardbrenkus.hospitalshiftscheduler.service.MvcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static com.richardbrenkus.hospitalshiftscheduler.i18n.PropertyBundleTestSupport.loadProperties;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@ContextConfiguration(classes = MvcConfig.class)
class MessageSourceIT {

    private static final String BASE = "language/messages.properties";
    private static final String CZECH = "language/messages_cs.properties";
    private static final String GERMAN = "language/messages_de.properties";
    private static final String HUNGARIAN = "language/messages_hu.properties";

    @Autowired
    private MessageSource messageSource;

    @Test
    void shouldResolveEveryApplicationMessageForEverySupportedLocale() {
        List<LocaleExpectation> expectations = List.of(
                new LocaleExpectation(Locale.ENGLISH, BASE),
                new LocaleExpectation(Locale.US, BASE),
                new LocaleExpectation(Locale.of("cs"), CZECH),
                new LocaleExpectation(Locale.of("cs", "CZ"), CZECH),
                new LocaleExpectation(Locale.GERMAN, GERMAN),
                new LocaleExpectation(Locale.of("hu"), HUNGARIAN)
        );

        expectations.forEach(this::assertEveryMessageResolvesExactly);
    }

    @Test
    void shouldFallbackToBaseEnglishMessagesForUsLocale() {
        assertThat(messageSource.getMessage("login.header", null, Locale.US)).isEqualTo(loadProperties(BASE).getProperty("login.header"));
    }

    @Test
    void shouldFallbackToCzechLanguageBundleForCzechRepublicLocale() {
        assertThat(messageSource.getMessage("login.header", null, Locale.of("cs", "CZ"))).isEqualTo(loadProperties(CZECH).getProperty("login.header"));
    }

    private void assertEveryMessageResolvesExactly(LocaleExpectation expectation) {
        Properties expectedProperties = loadProperties(expectation.bundleResource());

        expectedProperties.stringPropertyNames().forEach(key -> {
            String expected = expectedProperties.getProperty(key);
            String actual = messageSource.getMessage(key, null, expectation.locale());

            assertThat(actual)
                    .as("Message key '%s' for locale '%s'", key, expectation.locale())
                    .isEqualTo(expected);
        });
    }

    private record LocaleExpectation(Locale locale, String bundleResource) {
    }
}
