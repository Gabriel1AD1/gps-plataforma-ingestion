package com.ingestion.pe.mscore.commons.libs;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.*;

@Slf4j
public class EngineJexl {
    private static final JexlEngine JEXL_ENGINE_SCRIPT = new JexlBuilder().strict(true).silent(false).safe(false)
            .cache(120).create();
    private static final JexlEngine JEXL_ENGINE_EVALUATE_BOOLEAN = new JexlBuilder()
            .strict(false)
            .silent(true) // evita lanzar excepción por variables ausentes; devuelve null en su lugar
            .cache(120)
            .create();

    private EngineJexl() {
    }

    public static JexlEngine getEngine() {
        return JEXL_ENGINE_SCRIPT;
    }

    /**
     * error → false - null
     * → false - No boolean → false
     */
    public static boolean evaluateBooleanExpression(
            String jexlScript, Map<String, Object> attributes) {

        if (jexlScript == null || jexlScript.isBlank()) {
            return false;
        }

        try {
            JexlExpression expression = JEXL_ENGINE_EVALUATE_BOOLEAN.createExpression(jexlScript);

            JexlContext context = new MapContext(attributes);

            Object result = expression.evaluate(context);

            if (result instanceof Boolean bool) {
                return bool;
            }

            return false;

        } catch (RuntimeException e) {
            return false;
        }
    }

    public static Map<String, Object> overrideAttributes(
            Map<String, Object> attributes, String jexlScript) {

        if (jexlScript == null || jexlScript.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, Object> result = new HashMap<>(attributes);
            JexlContext context = new MapContext(result);

            JexlScript script = JEXL_ENGINE_SCRIPT.createScript(jexlScript);
            script.execute(context);
            result.remove("messageError");
            return result;

        } catch (JexlException e) {
            log.error("Error executing JEXL script: {}", jexlScript);
            // fail-safe
            var response = new HashMap<>(attributes);
            response.put("messageError", buildDetailedError(e, attributes, jexlScript));
            return response;
        }
    }

    private static Map<String, Object> buildDetailedError(
            JexlException e, Map<String, Object> attributes, String script) {
        Map<String, Object> error = new HashMap<>();

        error.put("errorType", e.getClass().getSimpleName());
        error.put("message", e.getMessage());
        error.put("script", script);

        if (e.getInfo() != null) {
            error.put("line", e.getInfo().getLine());
            error.put("column", e.getInfo().getColumn());
        }

        if (e.getCause() != null) {
            error.put("rootCause", e.getCause().toString());
        }

        String suspect = extractSuspectVariable(script);
        if (suspect != null && attributes.containsKey(suspect)) {
            error.put("suspectVariable", suspect);
            error.put("suspectValue", attributes.get(suspect));
        }

        error.put(
                "userMessage",
                "Error al ejecutar script JEXL. Revise que las variables sean numéricas y el formato sea válido.");

        return error;
    }

    private static String extractSuspectVariable(String script) {
        for (String token : script.split("\\W+")) {
            if (!token.isBlank()
                    && !token.equals("var")
                    && !token.equals("if")
                    && !Character.isDigit(token.charAt(0))) {
                return token;
            }
        }
        return null;
    }
}
