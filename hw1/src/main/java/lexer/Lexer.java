package lexer;

import dfa.DFAState;
import tokens.TokenType;

public class Lexer {

    public static void tokenize(String input, DFAState start) {
        System.out.println("Text: " + input.replace("\n", "\\n"));
        int pos = 0;

        while (pos < input.length()) {
            DFAState curr = start;
            int lastAcceptPos = -1;
            TokenType lastAcceptToken = null;

            int i = pos;
            while (i < input.length()) {
                char c = input.charAt(i);
                if (c > 127) { curr = null; break; }

                curr = curr.trans[c];

                if (curr.accept == TokenType.ERROR) break;
                if (curr.accept != null) {
                    lastAcceptPos = i;
                    lastAcceptToken = curr.accept;
                }
                i++;
            }

            if (lastAcceptToken != null) {
                String lexeme = input.substring(pos, lastAcceptPos + 1);
                if (lastAcceptToken != TokenType.WS && lastAcceptToken != TokenType.COMMENT) {
                    System.out.printf("  [%-12s] '%s'%n", lastAcceptToken, lexeme);
                }
                pos = lastAcceptPos + 1;
            } else {
                System.out.printf("  [ERROR       ] Unknown '%c' (code %d)%n",
                        input.charAt(pos), (int) input.charAt(pos));
                pos++;
            }
        }

        System.out.println("  [EOF]");
        System.out.println("-".repeat(40));
    }
}