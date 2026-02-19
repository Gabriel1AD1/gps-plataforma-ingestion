package com.ingestion.pe.mscore.commons.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public abstract class EnumAttributeConverter<E extends Enum<E>>
        implements AttributeConverter<E, String> {

    private final Class<E> enumClass;

    protected EnumAttributeConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Enum.valueOf(enumClass, dbData);
    }
}
