package com.apigateway.apiservice.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    //  List of endpoints that DO NOT need a token
    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/google",   //  Google OAuth — no JWT needed
            "/api/v1/auth/refresh",
            "/uploads",              // Product images — no JWT needed (browser img tags)
            "/eureka",
            "/v3/api-docs",
            "/auth/v3/api-docs",
            "/product/v3/api-docs",
            "/warehouse/v3/api-docs",
            "/supplier/v3/api-docs",
            "/movement/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                boolean secured = openApiEndpoints
                        .stream()
                        .noneMatch(uri -> path.contains(uri));
                System.out.println("Path: " + path + " | Secured: " + secured);
                return secured;
            };

}
