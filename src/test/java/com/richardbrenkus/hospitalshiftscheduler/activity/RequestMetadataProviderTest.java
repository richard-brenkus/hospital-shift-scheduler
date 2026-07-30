package com.richardbrenkus.hospitalshiftscheduler.activity;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestMetadataProviderTest {

    private final RequestMetadataProvider provider = new RequestMetadataProvider();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void current_shouldReturnSystem_whenNoRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        RequestMetadata metadata = provider.current();

        assertThat(metadata).isEqualTo(RequestMetadata.system());
    }

    @Test
    void current_shouldReturnRequestMethodPathAndClientIp_whenRequestContextIsBound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/admin/users");
        request.setRemoteAddr("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestMetadata metadata = provider.current();

        assertThat(metadata.requestMethod()).isEqualTo("POST");
        assertThat(metadata.requestPath()).isEqualTo("/admin/users");
        assertThat(metadata.clientIp()).isEqualTo("10.0.0.5");
    }

    @Test
    void current_shouldReturnUnknownForEachField_whenRequestReturnsBlankOrNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("   ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestMetadata metadata = provider.current();

        assertThat(metadata.requestMethod()).isEqualTo("UNKNOWN");
        assertThat(metadata.requestPath()).isEqualTo("UNKNOWN");
        assertThat(metadata.clientIp()).isEqualTo("UNKNOWN");
    }

    @Test
    void current_shouldReturnUnknown_whenAccessorsThrow() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenThrow(new RuntimeException("boom"));
        when(request.getRequestURI()).thenThrow(new RuntimeException("boom"));
        when(request.getRemoteAddr()).thenThrow(new RuntimeException("boom"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestMetadata metadata = provider.current();

        assertThat(metadata.requestMethod()).isEqualTo("UNKNOWN");
        assertThat(metadata.requestPath()).isEqualTo("UNKNOWN");
        assertThat(metadata.clientIp()).isEqualTo("UNKNOWN");
    }
}
