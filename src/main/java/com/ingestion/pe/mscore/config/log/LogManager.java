package com.ingestion.pe.mscore.config.log;

import org.slf4j.MDC;

public class LogManager {

    public static void addCorrelationId(String correlationId) {
        MDC.put("correlationId", correlationId);
    }

    public static void addRequestId(String requestId) {
        MDC.put("requestId", requestId);
    }

    public static void addClientIp(String clientIp) {
        MDC.put("clientIp", clientIp);
    }

    public static void addPath(String path) {
        MDC.put("path", path);
    }

    public static void addApplicationName(String applicationName) {
        MDC.put("applicationName", applicationName);
    }

    public static void addUserId(String string) {
        MDC.put("userId", string);

    }

    public static String getRequestId() {
        return MDC.get("requestId");
    }

    public static void addCompanyId(String string) {
        MDC.put("companyId", string);
    }

    public static String getClientIp() {
        return MDC.get("clientIp");
    }
}
