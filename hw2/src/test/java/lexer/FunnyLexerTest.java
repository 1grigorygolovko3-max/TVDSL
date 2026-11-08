package lexer;

import dfa.DFAState;
import dfa.HopcroftMinimizer;
import dfa.SubsetConstruction;
import json.DfaJsonExporter;
import nfa.NFA;
import nfa.ThompsonBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tokens.TokenType;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class FunnyLexerTest {

    private static final Pattern TOKEN_LINE = Pattern.compile("\\[([A-Z_]+)\\s*]\\s*(.*)");
    private static final Pattern QUOTED = Pattern.compile("'([^']*)'");

    private static DFAState start;
    private static List<DFAState> minStates;
    private static int dfaBefore;

    @BeforeAll
    static void buildAutomaton() {
        NFA nfa = ThompsonBuilder.buildFunnyLexerNFA();
        List<DFAState> raw = new ArrayList<>();
        DFAState rawStart = SubsetConstruction.convert(nfa, raw);
        dfaBefore = raw.size();
        minStates = new ArrayList<>();
        start = HopcroftMinimizer.minimize(raw, rawStart, minStates);
    }

    @Test
    void structure() {
        assertTrue(dfaBefore > 0, "");
        assertTrue(minStates.size() > 0, "minDFA has to contain at least one state");
        assertTrue(minStates.size() <= dfaBefore, "minDFA cannot be greater than dfaBefore");

        DFAState trap = trap();
        assertEquals(TokenType.ERROR, trap.accept);
        for (char c = 0; c < 128; c++) {
            assertEquals(trap, trap.trans[c], "trap has to loop on '" + c + "'");
        }
        assertEquals(trap, start.trans['@'], "unknown symbol has to be in trap");

        String json = DfaJsonExporter.export(minStates, start);
        assertNotNull(json);
        assertTrue(json.contains("\"startState\""));
        assertTrue(json.contains("\"states\""));
    }

    @Test
    void emptyAndWhitespace() {
        assertTokens("");
        assertTokens(" \t\r\n");
        assertTokens("   ");
    }

    @Test
    void integers() {
        assertTokens("0", "INT:0");
        assertEquals(TokenType.INT, acceptWhole("0"));

        assertTokens("00", "INT:0", "INT:0");
        assertEquals(TokenType.ERROR, acceptWhole("00"));

        assertTokens("01", "INT:0", "INT:1");
        assertEquals(TokenType.ERROR, acceptWhole("01"));

        assertTokens("42", "INT:42");
        assertTokens("123045", "INT:123045");
        assertTokens("007", "INT:0", "INT:0", "INT:7");
    }

    @Test
    void identifiersWithUnderscore() {
        assertTokens("foo_bar", "IDENT:foo_bar");
        assertTokens("_a", "IDENT:_a");
        assertTokens("a1_b2_c3", "IDENT:a1_b2_c3");
        assertTokens("ifx", "IDENT:ifx");
        assertTokens("integer", "IDENT:integer");
        assertTokens("123functionality", "INT:123", "IDENT:functionality");
    }

    @Test
    void keywords() {
        assertTokens("function",  "KW_FUNCTION:function");
        assertTokens("requires",  "KW_REQUIRES:requires");
        assertTokens("returns",   "KW_RETURNS:returns");
        assertTokens("ensures",   "KW_ENSURES:ensures");
        assertTokens("uses",      "KW_USES:uses");
        assertTokens("while",     "KW_WHILE:while");
        assertTokens("if",        "KW_IF:if");
        assertTokens("else",      "KW_ELSE:else");
        assertTokens("invariant", "KW_INVARIANT:invariant");
        assertTokens("assert",    "KW_ASSERT:assert");
        assertTokens("assume",    "KW_ASSUME:assume");
        assertTokens("int",       "KW_INT:int");
        assertTokens("length",    "KW_LENGTH:length");
        assertTokens("or",        "KW_OR:or");
        assertTokens("and",       "KW_AND:and");
        assertTokens("not",       "KW_NOT:not");
        assertTokens("true",      "KW_TRUE:true");
        assertTokens("false",     "KW_FALSE:false");
        assertTokens("forall",    "KW_FORALL:forall");
        assertTokens("exists",    "KW_EXISTS:exists");
    }

    @Test
    void delimitersAndOperators() {
        assertTokens("()[]{},;",
                "LPAREN:(", "RPAREN:)", "LBRACK:[", "RBRACK:]",
                "LBRACE:{", "RBRACE:}", "COMMA:,", "SEMICOLON:;");
        assertTokens(":", "COLON::");

        assertTokens("+-*/", "PLUS:+", "MINUS:-", "STAR:*", "SLASH:/");
        assertTokens("==", "EQ_EQ:==");
        assertTokens("!=", "NOT_EQ:!=");
        assertTokens("<=", "LESS_EQ:<=");
        assertTokens(">=", "GREATER_EQ:>=");
        assertTokens("->", "ARROW:->");
        assertTokens("<",  "LESS:<");
        assertTokens(">",  "GREATER:>");
        assertTokens("=",  "ASSIGN:=");
    }

    @Test
    void mixedExpressions() {
        assertTokens("x = 42", "IDENT:x", "ASSIGN:=", "INT:42");
        assertTokens("x -> y", "IDENT:x", "ARROW:->", "IDENT:y");
        assertTokens("a:int", "IDENT:a", "COLON::", "KW_INT:int");
        assertTokens("if (x >= 10)",
                "KW_IF:if", "LPAREN:(", "IDENT:x",
                "GREATER_EQ:>=", "INT:10", "RPAREN:)");
        assertTokens("a = 1; b = 2;",
                "IDENT:a", "ASSIGN:=", "INT:1", "SEMICOLON:;",
                "IDENT:b", "ASSIGN:=", "INT:2", "SEMICOLON:;");
    }

    @Test
    void commentsAndOutsideAlphabet() {
        assertTokens("// c\r\nx", "IDENT:x");
        assertTokens("// comment only");
        assertTokens("x = 1; // comment", "IDENT:x", "ASSIGN:=", "INT:1", "SEMICOLON:;");
        assertTokens("@", "ERROR:@");
        assertTokens("я", "ERROR:я");
        assertTokens("x @ y", "IDENT:x", "ERROR:@", "IDENT:y");
    }

    private static DFAState trap() {
        DFAState trap = null;
        for (DFAState state : minStates) {
            if (state.accept == TokenType.ERROR) {
                assertNull(trap, "ONLY ONE trap");
                trap = state;
            }
        }
        assertNotNull(trap, "trap NOT FOUND");
        return trap;
    }

    private static TokenType acceptWhole(String text) {
        DFAState current = start;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 127) {
                return TokenType.ERROR;
            }
            current = current.trans[c];
        }
        return current.accept;
    }

    private static void assertTokens(String input, String... expected) {
        assertEquals(List.of(expected), visible(input));
    }

    private static List<String> visible(String input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            Lexer.tokenize(input, start);
        } finally {
            System.setOut(previous);
        }
        String dumped = buffer.toString(StandardCharsets.UTF_8);
        List<String> tokens = new ArrayList<>();
        for (String line : dumped.split("\n")) {
            Matcher matcher = TOKEN_LINE.matcher(line.trim());
            if (!matcher.matches()) continue;
            String type = matcher.group(1);
            if (type.equals("EOF")) continue;
            Matcher quoted = QUOTED.matcher(matcher.group(2));
            String lexeme = quoted.find() ? quoted.group(1) : "";
            tokens.add(type + ":" + lexeme);
        }
        return tokens;
    }
}