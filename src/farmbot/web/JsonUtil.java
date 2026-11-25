package farmbot.web;

import java.util.List;
import java.util.Map;

/**
 * A tiny hand-written JSON helper - no framework, as the spec requires.
 *
 * {@link #write} serialises the plain Java types the handlers build (Map, List,
 * String, Number, Boolean, null) into a JSON string, escaping strings correctly.
 * {@link #parseObject} does the reverse for the two POST bodies the API receives
 * (login and register), which are small flat objects of string values.
 *
 * It is deliberately minimal: it covers exactly what this project sends and
 * receives, and nothing more.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    // ---- writing ----------------------------------------------------------

    /** Serialises a value tree (Map / List / String / Number / Boolean / null) to JSON. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value); // valid JSON literal as-is
        } else if (value instanceof Map<?, ?> map) {
            writeObject(sb, map);
        } else if (value instanceof Iterable<?> list) {
            writeArray(sb, list);
        } else {
            writeString(sb, value.toString()); // fallback: treat as a string
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Iterable<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
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

    // ---- reading ----------------------------------------------------------

    /**
     * Parses a flat JSON object of string values, e.g. {@code {"username":"x","password":"y"}},
     * into a Map. Enough for the login and register request bodies. Returns an empty map for
     * blank input. Throws IllegalArgumentException on malformed JSON.
     */
    public static Map<String, String> parseObject(String json) {
        return new MiniParser(json).parseObject();
    }

    /** A small recursive-descent parser for flat {string: string} objects. */
    private static final class MiniParser {
        private final String src;
        private int pos;

        MiniParser(String src) {
            this.src = src == null ? "" : src;
        }

        Map<String, String> parseObject() {
            Map<String, String> result = new java.util.LinkedHashMap<>();
            skipWhitespace();
            if (pos >= src.length()) {
                return result; // empty body
            }
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parseString();
                result.put(key, value);
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    break;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + (pos - 1));
                }
            }
            return result;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= src.length()) {
                    throw new IllegalArgumentException("Unterminated string");
                }
                char c = src.charAt(pos++);
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = src.substring(pos, Math.min(pos + 4, src.length()));
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new IllegalArgumentException("Bad escape \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            if (pos >= src.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return src.charAt(pos);
        }

        private char next() {
            if (pos >= src.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return src.charAt(pos++);
        }

        private void expect(char expected) {
            char c = next();
            if (c != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' but found '" + c + "'");
            }
        }
    }
}
