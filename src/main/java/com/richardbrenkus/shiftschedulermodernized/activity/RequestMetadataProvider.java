package com.richardbrenkus.shiftschedulermodernized.activity;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestMetadataProvider {

    public RequestMetadata current() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return RequestMetadata.system();
        }

        HttpServletRequest request = attributes.getRequest();

        return new RequestMetadata(request.getMethod(), request.getRequestURI(), resolveClientIp(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        /*
         * Safe default: use the address seen directly by the application.
         *
         * Do not trust X-Forwarded-For directly unless the application is
         * deployed behind a controlled reverse proxy that removes untrusted
         * forwarded headers.
         */
        return request.getRemoteAddr();
    }
}
