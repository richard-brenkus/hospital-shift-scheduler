package com.richardbrenkus.hospitalshiftscheduler.i18n;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ValidationConstants;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.UserRegisterForm;
import com.richardbrenkus.hospitalshiftscheduler.service.MvcConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.richardbrenkus.hospitalshiftscheduler.i18n.PropertyBundleTestSupport.expectedValue;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@ContextConfiguration(classes = MvcConfig.class)
class ValidationMessageResolutionIT {

    private static final String BASE = "ValidationMessages.properties";
    private static final String CZECH = "ValidationMessages_cs.properties";
    private static final String GERMAN = "ValidationMessages_de.properties";
    private static final String HUNGARIAN = "ValidationMessages_hu.properties";

    @Autowired
    private Validator validator;

    @AfterEach
    void resetLocaleContext() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldResolveNotBlankValidationMessageForEverySupportedLocale() {
        List<LocaleExpectation> expectations = List.of(
                new LocaleExpectation(Locale.ENGLISH, BASE),
                new LocaleExpectation(Locale.US, BASE),
                new LocaleExpectation(Locale.of("cs"), CZECH),
                new LocaleExpectation(Locale.of("cs", "CZ"), CZECH),
                new LocaleExpectation(Locale.GERMAN, GERMAN),
                new LocaleExpectation(Locale.of("hu"), HUNGARIAN)
        );

        expectations.forEach(expectation -> {
            LocaleContextHolder.setLocale(expectation.locale());

            UserRegisterForm form = validForm();
            form.setName("");

            ConstraintViolation<UserRegisterForm> violation = findViolation(form, "name", NotBlank.class);

            assertThat(violation.getMessage())
                    .as("NotBlank message for locale '%s'", expectation.locale())
                    .isEqualTo(expectedValue(expectation.bundleResource(),"user.name.NotBlank"));
        });
    }

    @Test
    void shouldResolveAndInterpolateSizeValidationMessageForEverySupportedLocale() {
        List<LocaleExpectation> expectations = List.of(
                new LocaleExpectation(Locale.ENGLISH, BASE),
                new LocaleExpectation(Locale.US, BASE),
                new LocaleExpectation(Locale.of("cs"), CZECH),
                new LocaleExpectation(Locale.of("cs", "CZ"), CZECH),
                new LocaleExpectation(Locale.GERMAN, GERMAN),
                new LocaleExpectation(Locale.of("hu"), HUNGARIAN)
        );

        expectations.forEach(expectation -> {
            LocaleContextHolder.setLocale(expectation.locale());

            UserRegisterForm form = validForm();
            form.setName("A");

            ConstraintViolation<UserRegisterForm> violation = findViolation(form, "name", Size.class);

            String expected = expectedValue(expectation.bundleResource(), "user.name.size")
                    .replace("{min}", String.valueOf(ValidationConstants.NAME_MIN_LENGTH))
                    .replace("{max}", String.valueOf(ValidationConstants.NAME_MAX_LENGTH));

            assertThat(violation.getMessage())
                    .as("Size message for locale '%s'", expectation.locale())
                    .isEqualTo(expected);
        });
    }

    private ConstraintViolation<UserRegisterForm> findViolation(UserRegisterForm form, String propertyName, Class<? extends Annotation> annotationType) {
        Set<ConstraintViolation<UserRegisterForm>> violations = validator.validate(form);

        return violations.stream()
                .filter(violation ->
                        violation.getPropertyPath().toString().equals(propertyName))
                .filter(violation ->
                        violation.getConstraintDescriptor()
                                .getAnnotation()
                                .annotationType()
                                .equals(annotationType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected " + annotationType.getSimpleName() + " violation for property '" + propertyName + "'"));
    }

    private UserRegisterForm validForm() {
        UserRegisterForm form = new UserRegisterForm();
        form.setName("Valid Name");
        form.setUsername("valid.username");
        form.setEmail("valid@example.com");
        form.setPassword("ValidPassword1");
        return form;
    }

    private record LocaleExpectation(Locale locale, String bundleResource) {
    }
}
