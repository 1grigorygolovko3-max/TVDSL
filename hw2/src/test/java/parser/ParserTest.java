package parser;

import dfa.DFAState;
import dfa.HopcroftMinimizer;
import dfa.SubsetConstruction;
import lexer.Lexer;
import nfa.ThompsonBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {
    private static DFAState start;

    @BeforeAll
    static void buildLexer() {
        var raw = new ArrayList<DFAState>();
        DFAState rawStart = SubsetConstruction.convert(ThompsonBuilder.buildFunnyLexerNFA(), raw);
        var min = new ArrayList<DFAState>();
        start = HopcroftMinimizer.minimize(raw, rawStart, min);
    }

    @Test
    void minimalFunctionMatchesSnapshot() {
        Parser.Result result = parse("""
                inc(a:int) returns r:int {
                  r = a + 1;
                }
                """);
        assertTrue(result.ok(), result.errors().toString());
        assertEquals("""
                Module
                  Function inc
                    params
                      Param a : int
                    returns
                      Param r : int
                    body
                      Block
                        Assign r
                          Binary +
                            Var a
                            Int 1
                """, result.module().pretty());
    }

    @Test
    void precedenceIsLeftAndUnaryBindsTighter() {
        Parser.Result result = parse("""
                calc() returns r:int {
                  r = a + b * c - -d;
                }
                """);
        assertTrue(result.ok(), result.errors().toString());
        assertTrue(result.module().pretty().contains("""
        Assign r
          Binary -
            Binary +
              Var a
              Binary *
                Var b
                Var c
            Unary -
              Var d
"""));
    }

    @Test
    void contractsControlFlowAndArraySugar() {
        Parser.Result result = parse("""
                fill(a:int[], i:int, v:int)
                requires i >= 0 and i < length(a)
                returns r:int[]
                ensures r == a
                uses tmp
                {
                  if (v > 0) {
                    a[i] = v;
                  } else {
                    a[i] = 0;
                  }
                  while (i != 0)
                  invariant i >= 0
                  {
                    i = i - 1;
                  }
                  assert v >= 0;
                  assume i >= 0;
                  r = a[i];
                }
                """);
        assertTrue(result.ok(), result.errors().toString());
        String tree = result.module().pretty();
        assertTrue(tree.contains("Param a : int[]"));
        assertTrue(tree.contains("requires"));
        assertTrue(tree.contains("Binary and"));
        assertTrue(tree.contains("Call length"));
        assertTrue(tree.contains("ensures"));
        assertTrue(tree.contains("Local tmp"));
        assertTrue(tree.contains("If"));
        assertTrue(tree.contains("else"));
        assertTrue(tree.contains("Call set"));
        assertTrue(tree.contains("While"));
        assertTrue(tree.contains("invariant"));
        assertTrue(tree.contains("Assert"));
        assertTrue(tree.contains("Assume"));
        assertTrue(tree.contains("Index"));
    }

    @Test
    void tupleAssignmentKeepsTheCall() {
        Parser.Result result = parse("""
                swap(a:int, b:int) returns x:int, y:int {
                  x, y = id(b, a);
                }
                """);
        assertTrue(result.ok(), result.errors().toString());
        assertTrue(result.module().pretty().contains("""
        TupleAssign x, y
          Call id
            Var b
            Var a
"""));
    }

    @Test
    void implicationIsRightAssociative() {
        Parser.Result result = parse("""
                check(a:int, b:int, c:int)
                requires a > 0 -> b > 0 -> c > 0
                returns r:int
                {
                  r = 1;
                }
                """);
        assertTrue(result.ok(), result.errors().toString());
        assertTrue(result.module().pretty().contains("""
    requires
      Binary ->
        Binary >
          Var a
          Int 0
        Binary ->
          Binary >
            Var b
            Int 0
          Binary >
            Var c
            Int 0
"""));
    }

    @Test
    void emptyInputIsAnErrorAtFirstPosition() {
        Parser.Result result = parse("");
        assertFalse(result.ok());
        assertTrue(result.errors().get(0).startsWith("1:1:"));
        assertTrue(result.errors().get(0).contains("пустой ввод"));
    }

    @Test
    void negativeProgramsReportPositionAndTerminate() {
        assertErrorContains("inc(a:int returns r:int { r = 1; }", "1:", "')'");
        assertErrorContains("inc() returns r:int { r = ; }", "ожидалось выражение");
        assertErrorContains("inc() returns r:int { r = 1; } )", "ожидалась функция");
        assertErrorContains("inc() returns r:int { r = 1; ", "'}'");

        assertErrorContains("inc() returns r:int { r = a + + 1; }", "ожидалось выражение");
        assertErrorContains("inc() returns r:int { if (a <> 0) { r = 1; } }", "ожидалось выражение");
        assertErrorContains("inc() returns r:int { r = a + % 1; }", "лексическая ошибка");

        Parser.Result recovered = parse("""
                inc( returns r:int { r = 1; }
                ok() returns r:int { r = 2; }
                """);
        assertFalse(recovered.ok());
        assertTrue(recovered.module().pretty().contains("Function ok"));
        assertTrue(recovered.errors().size() < 40);

        Parser.Result garbage = parse("π\nok() returns r:int { r = 2; }\n");
        assertFalse(garbage.ok());
        assertTrue(garbage.errors().get(0).startsWith("1:1:"));
        assertTrue(garbage.errors().get(0).contains("лексическая ошибка"));
        assertFalse(String.join("\n", garbage.errors()).contains("пустой ввод"));
        assertTrue(garbage.module().pretty().contains("Function ok"));
    }

    @Test
    void tokenPositionsSurviveNewlines() {
        var tokens = Lexer.scan("a\nb", start);
        assertEquals("a", tokens.get(0).lexeme);
        assertEquals(1, tokens.get(0).line);
        assertEquals(1, tokens.get(0).column);
        assertEquals("b", tokens.get(1).lexeme);
        assertEquals(2, tokens.get(1).line);
        assertEquals(1, tokens.get(1).column);
    }

    private static void assertErrorContains(String source, String... parts) {
        Parser.Result result = parse(source);
        assertFalse(result.ok(), source);
        String joined = String.join("\n", result.errors());
        for (String part : parts) {
            assertTrue(joined.contains(part), joined);
        }
    }

    private static Parser.Result parse(String source) {
        return new Parser(Lexer.scan(source, start)).parse();
    }
}
