package tokens;

public final class Token {
    public final TokenType type;
    public final String lexeme;
    public final int offset;
    public final int endOffset;
    public final int line;
    public final int column;

    public Token(TokenType type, String lexeme, int offset, int endOffset, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.offset = offset;
        this.endOffset = endOffset;
        this.line = line;
        this.column = column;
    }

    public String position() {
        return line + ":" + column;
    }
}
