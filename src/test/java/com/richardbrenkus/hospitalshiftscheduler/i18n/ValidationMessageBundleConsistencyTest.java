package com.richardbrenkus.hospitalshiftscheduler.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.richardbrenkus.hospitalshiftscheduler.i18n.PropertyBundleTestSupport.assertNoBlankValues;
import static com.richardbrenkus.hospitalshiftscheduler.i18n.PropertyBundleTestSupport.assertResourceDoesNotExist;
import static com.richardbrenkus.hospitalshiftscheduler.i18n.PropertyBundleTestSupport.assertSameKeys;

class ValidationMessageBundleConsistencyTest {

    private static final String BASE = "ValidationMessages.properties";

    private static final List<String> LOCALIZED_BUNDLES = List.of(
            "ValidationMessages_cs.properties",
            "ValidationMessages_de.properties",
            "ValidationMessages_hu.properties"
    );

    @Test
    void shouldContainExactlyTheSameKeysInEveryLocalizedValidationBundle() {
        LOCALIZED_BUNDLES.forEach(bundle -> assertSameKeys(BASE, bundle));
    }

    @Test
    void shouldNotContainBlankValidationMessageValues() {
        assertNoBlankValues(BASE);
        LOCALIZED_BUNDLES.forEach(PropertyBundleTestSupport::assertNoBlankValues);
    }

    @Test
    void shouldNotRequireRedundantEnglishValidationBundle() {
        assertResourceDoesNotExist("ValidationMessages_en.properties");
    }
}
