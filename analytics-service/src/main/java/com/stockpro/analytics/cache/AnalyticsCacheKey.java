package com.stockpro.analytics.cache;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("analyticsCacheKey")
public class AnalyticsCacheKey {

    public String scope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }

        String role = auth.getAuthorities() == null
                ? "unknown"
                : auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("unknown");

        String department = auth.getDetails() instanceof String ? (String) auth.getDetails() : "global";
        return role + ":" + department;
    }
}
