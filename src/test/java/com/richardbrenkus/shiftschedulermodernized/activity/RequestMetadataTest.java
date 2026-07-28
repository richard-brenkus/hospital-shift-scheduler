package com.richardbrenkus.shiftschedulermodernized.activity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMetadataTest {

    @Test
    void system_shouldReturnAllFieldsSetToSystemLiteral() {
        RequestMetadata metadata = RequestMetadata.system();

        assertThat(metadata.requestMethod()).isEqualTo("SYSTEM");
        assertThat(metadata.requestPath()).isEqualTo("SYSTEM");
        assertThat(metadata.clientIp()).isEqualTo("SYSTEM");
    }

    @Test
    void system_shouldReturnEqualValuesOnRepeatedCalls() {
        assertThat(RequestMetadata.system()).isEqualTo(RequestMetadata.system());
    }
}
