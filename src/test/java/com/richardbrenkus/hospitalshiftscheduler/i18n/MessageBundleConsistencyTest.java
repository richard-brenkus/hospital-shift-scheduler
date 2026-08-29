package com.richardbrenkus.hospitalshiftscheduler.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.richardbrenkus.hospitalshiftscheduler.i18n.PropertyBundleTestSupport.assertNoBlankValues;
import static com.richardbrenkus.hospitalshiftscheduler.i18n.PropertyBundleTestSupport.assertSameKeys;

class MessageBundleConsistencyTest {

    private static final String BASE = "language/messages.properties";

    private static final List<String> LOCALIZED_BUNDLES = List.of(
            "language/messages_cs.properties",
            "language/messages_de.properties",
            "language/messages_hu.properties"
    );

    @Test
    void shouldContainExactlyTheSameKeysInEveryLocalizedMessageBundle() {
        LOCALIZED_BUNDLES.forEach(bundle -> assertSameKeys(BASE, bundle));
    }

    @Test
    void shouldNotContainBlankMessageValues() {
        assertNoBlankValues(BASE);
        LOCALIZED_BUNDLES.forEach(PropertyBundleTestSupport::assertNoBlankValues);
    }
}
