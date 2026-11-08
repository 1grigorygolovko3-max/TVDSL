package lexer;

import dfa.DFAState;
import tokens.Token;
import tokens.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    public static List<Token> scan(String input, DFAState start) {
        List<Token> out = new ArrayList<>();
        int pos = 0;
        int line = 1;
        int column = 1;

        while (pos < input.length()) {
            int startPos = pos;
            int startLine = line;
            int startColumn = column;
            DFAState curr = start;
            int lastAcceptPos = -1;
            TokenType lastAcceptToken = null;

            int i = pos;
            while (i < input.length()) {
                char c = input.charAt(i);
                if (c > 127) {
                    break;
                }
                curr = curr.trans[c];
                if (curr.accept == TokenType.ERROR) {
                    break;
                }
                if (curr.accept != null) {
                    lastAcceptPos = i;
                    lastAcceptToken = curr.accept;
                }
                i++;
            }

            int end;
            TokenType type;
            if (lastAcceptToken != null) {
                end = lastAcceptPos + 1;
                type = lastAcceptToken;
            } else {
                end = Math.min(pos + 1, input.length());
                type = TokenType.ERROR;
            }

            String lexeme = input.substring(pos, end);
            int[] cursor = move(input, pos, end, line, column);
            line = cursor[0];
            column = cursor[1];
            pos = end;

            if (type != TokenType.WS && type != TokenType.COMMENT) {
                out.add(new Token(type, lexeme, startPos, end, startLine, startColumn));
            }
        }

        out.add(new Token(TokenType.EOF, "", pos, pos, line, column));
        return out;
    }

    public static void tokenize(String input, DFAState start) {
        System.out.println("Text: " + input.replace("\n", "\\n"));
        for (Token token : scan(input, start)) {
            if (token.type == TokenType.EOF) {
                System.out.printf("  [EOF] %d:%d%n", token.line, token.column);
            } else if (token.type == TokenType.ERROR) {
                char shown = token.lexeme.isEmpty() ? '?' : token.lexeme.charAt(0);
                System.out.printf("  [ERROR       ] Unknown '%c' (code %d) %d:%d%n",
                        shown, (int) shown, token.line, token.column);
            } else {
                System.out.printf("  [%-12s] '%s' %d:%d%n",
                        token.type, token.lexeme, token.line, token.column);
            }
        }
        System.out.println("-".repeat(40));
    }

    private static int[] move(String input, int from, int to, int line, int column) {
        for (int k = from; k < to; k++) {
            char ch = input.charAt(k);
            if (ch == '\n') {
                line++;
                column = 1;
            } else if (ch == '\r') {
                if (k + 1 >= to || input.charAt(k + 1) != '\n') {
                    line++;
                    column = 1;
                }
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }
}
