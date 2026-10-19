package json;

import dfa.DFAState;
import tokens.TokenType;

import java.util.List;

public class DfaJsonExporter {

    public static String export(List<DFAState> dfa, DFAState start) {
        Integer trapId = null;
        for (DFAState st : dfa) {
            if (st.accept == TokenType.ERROR) {
                trapId = st.id;
            }
        }
        if (trapId == null) {
            throw new IllegalStateException("trap state not found");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"startState\": ").append(start.id).append(",\n");
        sb.append("  \"trap\": ").append(trapId).append(",\n");
        sb.append("  \"unlistedGoesToTrap\": true,\n");
        sb.append("  \"skip\": [\"WS\", \"COMMENT\"],\n");
        sb.append("  \"states\": [\n");

        for (int i = 0; i < dfa.size(); i++) {
            DFAState st = dfa.get(i);
            sb.append("    { \"id\": ").append(st.id)
                    .append(", \"accept\": ")
                    .append(st.accept == null ? "null" : "\"" + st.accept + "\"")
                    .append(", \"trans\": {");

            boolean first = true;
            for (char c = 0; c < 128; c++) {
                if (st.trans[c] != null && st.trans[c].accept != TokenType.ERROR) {
                    if (!first) sb.append(", ");
                    String key = (c == '"' || c == '\\')
                            ? "\\" + c
                            : (c < 32 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
                    sb.append("\"").append(key).append("\": ").append(st.trans[c].id);
                    first = false;
                }
            }
            sb.append("} }").append(i == dfa.size() - 1 ? "" : ",").append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}
