package tokens;

public enum TokenType {
    KW_FUNCTION,
    KW_REQUIRES,
    KW_RETURNS,
    KW_ENSURES,
    KW_USES,
    KW_WHILE,
    KW_IF,
    KW_ELSE,
    KW_INVARIANT,
    KW_ASSERT,
    KW_ASSUME,
    KW_INT,
    KW_LENGTH,
    KW_OR,
    KW_AND,
    KW_NOT,
    KW_TRUE,
    KW_FALSE,
    KW_FORALL,
    KW_EXISTS,

    EQ_EQ,        // ==
    NOT_EQ,       // !=
    LESS_EQ,      // <=
    GREATER_EQ,   // >=
    ARROW,        // ->

    ASSIGN,       // =
    LESS,         // <
    GREATER,      // >
    PLUS,         // +
    MINUS,        // -
    STAR,         // *
    SLASH,        // /

    LPAREN,       // (
    RPAREN,       // )
    LBRACK,       // [
    RBRACK,       // ]
    LBRACE,       // {
    RBRACE,       // }
    COMMA,        // ,
    SEMICOLON,    // ;
    COLON,        // :

    IDENT,
    INT,

    WS,
    COMMENT,

    ERROR
}