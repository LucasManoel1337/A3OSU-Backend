package project.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class LongListConverter implements AttributeConverter<List<Long>, String> {

    private static final String SEPARADOR = ",";

    @Override
    public String convertToDatabaseColumn(List<Long> lista) {
        if (lista == null || lista.isEmpty()) {
            return "";
        }
        return lista.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(SEPARADOR));
    }

    @Override
    public List<Long> convertToEntityAttribute(String dadosDoBanco) {
        if (dadosDoBanco == null || dadosDoBanco.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dadosDoBanco.split(SEPARADOR))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}