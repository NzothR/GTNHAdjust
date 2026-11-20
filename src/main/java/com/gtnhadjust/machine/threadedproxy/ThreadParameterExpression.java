package com.gtnhadjust.machine.threadedproxy;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gregtech.api.enums.GTValues;

/** Small arithmetic grammar shared by the server and GUI, with voltage and EU/t literals. */
final class ThreadParameterExpression {

    private static final Pattern VOLTAGE = Pattern
        .compile("(?i)(\\d+(?:\\.\\d+)?)?\\s*A?\\s*(ULV|LV|MV|HV|EV|IV|LUV|ZPM|UV|UHV|UEV|UIV|UMV|UXV|MAX)\\b");
    private final String text;
    private int position;

    private ThreadParameterExpression(String text) {
        this.text = text;
    }

    /** Invalid user expressions return NaN and leave the existing setting intact. */
    static double parse(String expression) {
        Matcher matcher = VOLTAGE.matcher(expression.toUpperCase(Locale.ROOT));
        StringBuffer converted = new StringBuffer();
        while (matcher.find()) {
            int tier = 0;
            while (tier < GTValues.VN.length && !GTValues.VN[tier].equalsIgnoreCase(matcher.group(2))) tier++;
            if (tier == GTValues.VN.length) return Double.NaN;
            matcher.appendReplacement(
                converted,
                "(" + (matcher.group(1) == null ? "1" : matcher.group(1)) + "*" + GTValues.V[tier] + ")");
        }
        matcher.appendTail(converted);
        String normalized = converted.toString()
            .replaceAll("(?i)EU(?:/T)?", "")
            .replaceAll("\\s+", "");
        ThreadParameterExpression parser = new ThreadParameterExpression(normalized);
        try {
            double value = parser.sum();
            return parser.position == normalized.length() && Double.isFinite(value) ? value : Double.NaN;
        } catch (IllegalArgumentException invalid) {
            return Double.NaN;
        }
    }

    private double sum() {
        double value = product();
        while (position < text.length()) {
            char op = text.charAt(position);
            if (op != '+' && op != '-') break;
            position++;
            double right = product();
            value = op == '+' ? value + right : value - right;
        }
        return value;
    }

    private double product() {
        double value = atom();
        while (position < text.length()) {
            char op = text.charAt(position);
            if (op != '*' && op != '/') break;
            position++;
            double right = atom();
            value = op == '*' ? value * right : value / right;
        }
        return value;
    }

    private double atom() {
        if (position >= text.length()) throw new IllegalArgumentException();
        char first = text.charAt(position);
        if (first == '+' || first == '-') {
            position++;
            return (first == '-' ? -1 : 1) * atom();
        }
        if (first == '(') {
            position++;
            double value = sum();
            if (position >= text.length() || text.charAt(position++) != ')') throw new IllegalArgumentException();
            return value;
        }
        int start = position;
        while (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.'))
            position++;
        if (position < text.length() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
            position++;
            if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) position++;
            while (position < text.length() && Character.isDigit(text.charAt(position))) position++;
        }
        double value = Double.parseDouble(text.substring(start, position));
        if (position < text.length()) {
            char suffix = Character.toLowerCase(text.charAt(position));
            int power = suffix == 'k' ? 1 : suffix == 'm' ? 2 : suffix == 'g' ? 3 : suffix == 't' ? 4 : 0;
            if (power > 0) {
                position++;
                value *= Math.pow(1000, power);
            }
        }
        return value;
    }
}
