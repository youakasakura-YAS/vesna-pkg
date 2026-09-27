package @@PKG@@;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser/serializer (no external deps).
 * Supports object, array, string, number, boolean, null.
 */
public final class VesnaJson {

    private VesnaJson() {}

    // ---------- parse ----------

    public static Object parse(String text) {
        Parser p = new Parser(text);
        Object v = p.parseValue();
        p.skipWs();
        if (!p.atEnd()) throw new IllegalArgumentException("trailing JSON content at " + p.pos);
        return v;
    }

    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) throw new IllegalArgumentException("expected JSON object");
        return cast(v);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> cast(Object v) {
        return (Map<String, Object>) v;
    }

    private static final class Parser {
        final String s;
        int pos = 0;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        void skipWs() {
            while (!atEnd()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        char peek() { return s.charAt(pos); }

        Object parseValue() {
            skipWs();
            if (atEnd()) throw new IllegalArgumentException("unexpected end of JSON");
            char c = peek();
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default: return parseNumber();
            }
        }

        Map<String, Object> parseObject() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            pos++; // {
            skipWs();
            if (!atEnd() && peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                if (atEnd() || peek() != ':') throw new IllegalArgumentException("missing ':' at " + pos);
                pos++;
                Object val = parseValue();
                m.put(key, val);
                skipWs();
                if (atEnd()) throw new IllegalArgumentException("unterminated object");
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; return m; }
                throw new IllegalArgumentException("unexpected '" + c + "' at " + pos);
            }
        }

        List<Object> parseArray() {
            List<Object> l = new ArrayList<Object>();
            pos++; // [
            skipWs();
            if (!atEnd() && peek() == ']') { pos++; return l; }
            while (true) {
                l.add(parseValue());
                skipWs();
                if (atEnd()) throw new IllegalArgumentException("unterminated array");
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; return l; }
                throw new IllegalArgumentException("unexpected '" + c + "' at " + pos);
            }
        }

        String parseString() {
            if (atEnd() || peek() != '"') throw new IllegalArgumentException("expected string at " + pos);
            pos++;
            StringBuilder sb = new StringBuilder();
            while (!atEnd()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (atEnd()) throw new IllegalArgumentException("unterminated escape");
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > s.length()) throw new IllegalArgumentException("bad \\u escape");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: throw new IllegalArgumentException("bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("unterminated string");
        }

        Number parseNumber() {
            int start = pos;
            while (!atEnd()) {
                char c = s.charAt(pos);
                if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' ||
                        (c >= '0' && c <= '9')) pos++;
                else break;
            }
            String t = s.substring(start, pos);
            if (t.isEmpty()) throw new IllegalArgumentException("invalid token at " + start);
            if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0) {
                return Double.parseDouble(t);
            }
            return Long.parseLong(t);
        }

        void expect(String lit) {
            if (pos + lit.length() > s.length() || !s.startsWith(lit, pos)) {
                throw new IllegalArgumentException("expected " + lit + " at " + pos);
            }
            pos += lit.length();
        }
    }

    // ---------- serialize ----------

    public static String encode(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return encodeString((String) v);
        if (v instanceof Boolean) return ((Boolean) v).booleanValue() ? "true" : "false";
        if (v instanceof Number) return v.toString();
        if (v instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(encodeString(String.valueOf(e.getKey()))).append(':').append(encode(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (v instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : (List<?>) v) {
                if (!first) sb.append(',');
                first = false;
                sb.append(encode(o));
            }
            return sb.append(']').toString();
        }
        if (v instanceof Character) return encodeString(String.valueOf(v));
        return encodeString(String.valueOf(v));
    }

    public static String encodeString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    // helpers
    public static String str(Object v, String dflt) {
        return v == null ? dflt : String.valueOf(v);
    }
}
