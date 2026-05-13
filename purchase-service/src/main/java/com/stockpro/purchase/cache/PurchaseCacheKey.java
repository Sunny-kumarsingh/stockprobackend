package com.stockpro.purchase.cache;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("purchaseCacheKey")
public class PurchaseCacheKey {

    public String scope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }

        String roles = auth.getAuthorities() == null
                ? "unknown"
                : auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("unknown");

        String department = auth.getDetails() instanceof String ? (String) auth.getDetails() : "global";
        return roles + ":" + department;
    }
}
