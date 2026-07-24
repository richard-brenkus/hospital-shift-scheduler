package com.richardbrenkus.shiftschedulermodernized.activity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class RequestMetadataProvider {

    private static final String UNKNOWN_VALUE = "UNKNOWN";

    public RequestMetadata current() {

        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return RequestMetadata.system();
            }

            HttpServletRequest request = attributes.getRequest();

            return new RequestMetadata(
                    safeMethod(request),
                    safeUri(request),
                    safeClientIp(request)
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Could not determine request metadata. Falling back to SYSTEM metadata.",
                    exception
            );

            return RequestMetadata.system();
        }
    }

    private String safeMethod(HttpServletRequest request) {

        try {
            String method = request.getMethod();

            return method == null || method.isBlank()
                    ? UNKNOWN_VALUE
                    : method;

        } catch (RuntimeException exception) {
            return UNKNOWN_VALUE;
        }
    }

    private String safeUri(HttpServletRequest request) {

        try {
            String uri = request.getRequestURI();

            return uri == null || uri.isBlank()
                    ? UNKNOWN_VALUE
                    : uri;

        } catch (RuntimeException exception) {
            return UNKNOWN_VALUE;
        }
    }

    private String safeClientIp(HttpServletRequest request) {

        try {
            /*
             * Safe default: use the address seen directly by the application.
             *
             * Do not trust X-Forwarded-For unless the application is deployed
             * behind a trusted reverse proxy that sanitizes forwarded headers.
             */
            String ip = request.getRemoteAddr();

            return ip == null || ip.isBlank()
                    ? UNKNOWN_VALUE
                    : ip;

        } catch (RuntimeException exception) {
            return UNKNOWN_VALUE;
        }
    }
}