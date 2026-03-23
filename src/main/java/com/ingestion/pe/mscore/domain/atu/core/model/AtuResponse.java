package com.ingestion.pe.mscore.domain.atu.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AtuResponse {

    @JsonProperty("Código")
    private String code;

    @JsonProperty("Identifier")
    private String identifier;

    @JsonProperty("Timestamp")
    private String timestamp;

    public boolean isSuccess() {
        return "00".equals(code);
    }
    
    public String getMessage() {
        if (code == null) return "Código desconocido";
        return switch (code) {
            case "00" -> "Se recepcionó la trama correctamente";
            case "01" -> "verificar el formato de la trama enviada";
            case "03" -> "InvalidToken";
            case "05" -> "El identificador de la trama está vacío";
            case "06" -> "IMEI inválido, solo se permiten letras y números y debe tener 15 caracteres";
            case "07" -> "Placa inválida: máximo 7 caracteres alfanuméricos, puede incluir guiones";
            case "08" -> "Coordenadas inválidas (latitud fuera de -90 a 90, longitud fuera de -180 a 180)";
            case "09" -> "Velocidad inválida (debe ser 0- 999.99 km/h)";
            case "10" -> "Operador inválido: contiene caracteres no permitidos";
            case "11" -> "Identificador inválido: solo se permiten letras y números, máximo 50 caracteres";
            case "12" -> "ID de ruta inválido: contiene caracteres no permitidos, máximo 10 caracteres";
            case "13" -> "ID de dirección inválido: debe ser 0 (ida) o 1 (regreso)";
            case "14" -> "ID de conductor inválido: contiene caracteres no permitidos, máximo 20 caracteres";
            default -> "Código no documentado por ATU: " + code;
        };
    }
}
