package com.family.finance.ledger.csv;

import com.family.finance.common.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public final class CsvCodec {
    private CsvCodec() {}

    public static List<List<String>> parse(String input) {
        if (input == null) return List.of();
        String text = input.startsWith("\uFEFF") ? input.substring(1) : input;
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (quoted) {
                if (current == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"'); i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == '"' && field.isEmpty()) {
                quoted = true;
            } else if (current == ',') {
                row.add(field.toString()); field.setLength(0);
            } else if (current == '\n' || current == '\r') {
                if (current == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); field.setLength(0);
                if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(List.copyOf(row));
                row.clear();
            } else {
                field.append(current);
            }
        }
        if (quoted) throw new ApiException(HttpStatus.BAD_REQUEST, "CSV_INVALID", "CSV 中存在未闭合的引号");
        if (!field.isEmpty() || !row.isEmpty()) {
            row.add(field.toString());
            if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(List.copyOf(row));
        }
        return rows;
    }

    public static String line(List<String> fields) {
        return fields.stream().map(CsvCodec::escape).reduce((left, right) -> left + "," + right).orElse("") + "\r\n";
    }

    /** Prevent spreadsheet applications from evaluating imported text as formulas. */
    public static String spreadsheetSafe(String raw) {
        if (raw == null || raw.isEmpty()) return raw == null ? "" : raw;
        char first = raw.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' ? "'" + raw : raw;
    }

    private static String escape(String raw) {
        String value = raw == null ? "" : raw;
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
