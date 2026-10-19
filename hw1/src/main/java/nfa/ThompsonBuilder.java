package nfa;

import tokens.TokenType;

public class ThompsonBuilder {

    public static NFA fromChar(char c) {
        NFAState s = new NFAState(), e = new NFAState();
        s.addTransition(c, e);
        return new NFA(s, e);
    }

    public static NFA fromRange(char from, char to) {
        NFAState s = new NFAState(), e = new NFAState();
        for (char c = from; c <= to; c++) s.addTransition(c, e);
        return new NFA(s, e);
    }

    public static NFA fromString(String str) {
        NFA cur = fromChar(str.charAt(0));
        for (int i = 1; i < str.length(); i++) {
            cur = concat(cur, fromChar(str.charAt(i)));
        }
        return cur;
    }

    public static NFA notChar(char exclude) {
        NFAState s = new NFAState(), e = new NFAState();
        for (char c = 0; c < 128; c++) {
            if (c != exclude) s.addTransition(c, e);
        }
        return new NFA(s, e);
    }


    public static NFA concat(NFA a, NFA b) {
        a.end.addEpsilon(b.start);
        return new NFA(a.start, b.end);
    }

    public static NFA union(NFA... nfas) {
        NFAState s = new NFAState(), e = new NFAState();
        for (NFA nfa : nfas) {
            s.addEpsilon(nfa.start);
            nfa.end.addEpsilon(e);
        }
        return new NFA(s, e);
    }

    public static NFA star(NFA a) {
        NFAState s = new NFAState(), e = new NFAState();
        s.addEpsilon(a.start);
        s.addEpsilon(e);
        a.end.addEpsilon(a.start);
        a.end.addEpsilon(e);
        return new NFA(s, e);
    }

    public static NFA plus(NFA a) {
        NFAState s = new NFAState(), e = new NFAState();
        s.addEpsilon(a.start);
        a.end.addEpsilon(a.start);
        a.end.addEpsilon(e);
        return new NFA(s, e);
    }



    public static NFA bind(NFA nfa, TokenType type) {
        nfa.end.accept = type;
        return nfa;
    }


    public static NFA buildFunnyLexerNFA() {
        NFAState rootStart = new NFAState();
        java.util.List<NFA> tokens = new java.util.ArrayList<>();

        tokens.add(bind(fromString("function"),  TokenType.KW_FUNCTION));
        tokens.add(bind(fromString("requires"),  TokenType.KW_REQUIRES));
        tokens.add(bind(fromString("returns"),   TokenType.KW_RETURNS));
        tokens.add(bind(fromString("ensures"),   TokenType.KW_ENSURES));
        tokens.add(bind(fromString("uses"),      TokenType.KW_USES));
        tokens.add(bind(fromString("while"),     TokenType.KW_WHILE));
        tokens.add(bind(fromString("if"),        TokenType.KW_IF));
        tokens.add(bind(fromString("else"),      TokenType.KW_ELSE));
        tokens.add(bind(fromString("invariant"), TokenType.KW_INVARIANT));
        tokens.add(bind(fromString("assert"),    TokenType.KW_ASSERT));
        tokens.add(bind(fromString("assume"),    TokenType.KW_ASSUME));
        tokens.add(bind(fromString("int"),       TokenType.KW_INT));
        tokens.add(bind(fromString("length"),    TokenType.KW_LENGTH));
        tokens.add(bind(fromString("or"),        TokenType.KW_OR));
        tokens.add(bind(fromString("and"),       TokenType.KW_AND));
        tokens.add(bind(fromString("not"),       TokenType.KW_NOT));
        tokens.add(bind(fromString("true"),      TokenType.KW_TRUE));
        tokens.add(bind(fromString("false"),     TokenType.KW_FALSE));
        tokens.add(bind(fromString("forall"),    TokenType.KW_FORALL));
        tokens.add(bind(fromString("exists"),    TokenType.KW_EXISTS));

        tokens.add(bind(fromString("=="), TokenType.EQ_EQ));
        tokens.add(bind(fromString("!="), TokenType.NOT_EQ));
        tokens.add(bind(fromString("<="), TokenType.LESS_EQ));
        tokens.add(bind(fromString(">="), TokenType.GREATER_EQ));
        tokens.add(bind(fromString("->"), TokenType.ARROW));

        tokens.add(bind(fromString("="), TokenType.ASSIGN));
        tokens.add(bind(fromString("<"), TokenType.LESS));
        tokens.add(bind(fromString(">"), TokenType.GREATER));
        tokens.add(bind(fromString("+"), TokenType.PLUS));
        tokens.add(bind(fromString("-"), TokenType.MINUS));
        tokens.add(bind(fromString("*"), TokenType.STAR));
        tokens.add(bind(fromString("/"), TokenType.SLASH));

        tokens.add(bind(fromString("("), TokenType.LPAREN));
        tokens.add(bind(fromString(")"), TokenType.RPAREN));
        tokens.add(bind(fromString("["), TokenType.LBRACK));
        tokens.add(bind(fromString("]"), TokenType.RBRACK));
        tokens.add(bind(fromString("{"), TokenType.LBRACE));
        tokens.add(bind(fromString("}"), TokenType.RBRACE));
        tokens.add(bind(fromString(","), TokenType.COMMA));
        tokens.add(bind(fromString(";"), TokenType.SEMICOLON));
        tokens.add(bind(fromString(":"), TokenType.COLON));

        NFA comment = concat(fromString("//"), star(notChar('\n')));
        tokens.add(bind(comment, TokenType.COMMENT));

        NFA ws = plus(union(
                fromChar(' '),
                fromChar('\t'),
                fromChar('\r'),
                fromChar('\n')
        ));
        tokens.add(bind(ws, TokenType.WS));

        NFA alpha = union(fromRange('a', 'z'), fromRange('A', 'Z'), fromChar('_'));
        NFA alnum = union(fromRange('a', 'z'), fromRange('A', 'Z'),
                fromRange('0', '9'), fromChar('_'));
        tokens.add(bind(concat(alpha, star(alnum)), TokenType.IDENT));

        NFA nonzero = concat(fromRange('1', '9'), star(fromRange('0', '9')));
        tokens.add(bind(union(fromChar('0'), nonzero), TokenType.INT));

        for (NFA t : tokens) {
            rootStart.addEpsilon(t.start);
        }

        return new NFA(rootStart, new NFAState());
    }
}
