package com.ingestion.pe.mscore.config.security;

public class EndpointSecurityConstant {
  public static final String[] ENDPOINT_PUBLIC = {
    "/api/public/**",
    "/h2-console/**",
    "/connection/**",
    "/mnt/api/v1/monitoring/health" // Health check remains public
  };

  public static final String[] ENDPOINT_PRIVATE = {
    "/rts/api/v1/**", // Routes
    "/mnt/api/v1/monitoring/**" // Monitoring (except health)
  };
  
  public static final String[] ENDPOINT_SWAGGER = {
    "/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**"
  };
}
