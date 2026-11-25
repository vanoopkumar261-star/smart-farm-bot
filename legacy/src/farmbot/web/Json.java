package farmbot.web;

import java.util.List;
import java.util.Map;

/**
 * Hand-rolled JSON helper - no external library, matching this project's
 * "plain javac/java, no dependencies" rule. toJson() serializes anything
 * this app's responses need (String/Number/Boolean/Map/List/null).
 * parseFlat() deliberately only handles flat {"key":"value"} objects,
 * since every POST body this app sends (login/register/strategy) is flat -
 * this is not a general JSON parser.
 */
public final class Json {
    private Json() {
    }

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), sb);
                sb.append(':');
                write(entry.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(item, sb);
            }
            sb.append(']');
        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    public static Map<String, String> parseFlat(String body) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        if (body == null) {
            return result;
        }
        String trimmed = body.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '{') {
            return result;
        }
        trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        if (trimmed.isEmpty()) {
            return result;
        }
        int i = 0;
        int n = trimmed.length();
        while (i < n) {
            i = skipWhitespace(trimmed, i);
            String key = parseString(trimmed, i);
            i += stringLength(trimmed, i);
            i = skipWhitespace(trimmed, i);
            i++; // consume ':'
            i = skipWhitespace(trimmed, i);
            String value;
            if (i < n && trimmed.charAt(i) == '"') {
                value = parseString(trimmed, i);
                i += stringLength(trimmed, i);
            } else {
                int start = i;
                while (i < n && trimmed.charAt(i) != ',') {
                    i++;
                }
                value = trimmed.substring(start, i).trim();
            }
            result.put(key, value);
            i = skipWhitespace(trimmed, i);
            if (i < n && trimmed.charAt(i) == ',') {
                i++;
            }
        }
        return result;
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static String parseString(String s, int start) {
        int i = start + 1;
        StringBuilder sb = new StringBuilder();
        while (i < s.length() && s.charAt(i) != '"') {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(next);
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int stringLength(String s, int start) {
        int i = start + 1;
        while (i < s.length() && s.charAt(i) != '"') {
            if (s.charAt(i) == '\\') {
                i++;
            }
            i++;
        }
        return (i - start) + 1;
    }
}
