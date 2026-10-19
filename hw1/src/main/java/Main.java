import dfa.DFAState;
import dfa.HopcroftMinimizer;
import dfa.SubsetConstruction;
import json.DfaJsonExporter;
import lexer.Lexer;
import nfa.NFA;
import nfa.ThompsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import cli.CliArgs;

public class Main {
    public static void main(String[] args) throws IOException {
        CliArgs cli = CliArgs.parse(args);

        if (cli.help) {
            printHelp();
            return;
        }


        NFA nfa = ThompsonBuilder.buildFunnyLexerNFA();
        if (cli.nfa) printNfa(nfa);


        List<DFAState> dfaStates = new ArrayList<>();
        DFAState dfaStart = SubsetConstruction.convert(nfa, dfaStates);
        if (cli.dfa) printDfa(dfaStates, dfaStart);


        int dfaBefore = dfaStates.size();
        List<DFAState> minStates = new ArrayList<>();
        DFAState minStart = HopcroftMinimizer.minimize(dfaStates, dfaStart, minStates);
        if (cli.min) {
            System.out.println("DFA before:    " + dfaBefore);
            System.out.println("DFA after: " + minStates.size());
        }

        if (cli.test) {
            runTests(minStart, cli.testFile);
        }

        if (cli.jsonPath != null) {
            String json = DfaJsonExporter.export(minStates, minStart);
            Files.writeString(Paths.get(cli.jsonPath), json);
            System.out.println("JSON: " + cli.jsonPath);
        }


    }


    private static void printNfa(NFA nfa) {
        System.out.println("=== NFA ===");
        System.out.println("Start: " + nfa.start.id);
        System.out.println("End:   " + nfa.end.id);
    }

    private static void printDfa(List<DFAState> states, DFAState start) {
        System.out.println("=== DFA ===");
        System.out.println("Start:  " + start.id);
        System.out.println("States: " + states.size());
        long accepting = states.stream().filter(s -> s.accept != null).count();
        System.out.println("Accepting: " + accepting);
    }

    private static void runTests(DFAState start, String testFile) throws IOException {
        List<String> tests = new ArrayList<>();
        if (testFile == null) {
            tests = List.of(
                    "",

                    " ",
                    "   ",
                    "\t",
                    "\t\t",
                    "\r\n",
                    "  \t\r\n  ",
                    "\r\n\t ",


                    "0",
                    "42",
                    "123045",
                    "00",
                    "01",
                    "007",
                    "123functionality",


                    "x",
                    "foo",
                    "a1_b2_c3",
                    "_hidden",
                    "my_var_1",
                    "abc123",
                    "if_var",
                    "integer",
                    "or",
                    "order",
                    "not",
                    "nothing",

                    "function",
                    "returns",
                    "while",
                    "if",
                    "else",
                    "assert",
                    "assume",
                    "invariant",
                    "length",

                    "(",
                    ")",
                    "[",
                    "]",
                    "{",
                    "}",
                    ",",
                    ";",
                    "+",
                    "-",
                    "*",
                    "/",
                    "==",
                    "!=",
                    "<=",
                    ">=",
                    "<",
                    ">",
                    "=",
                    "->",

                    "x = 42",
                    "if (x >= 10)",
                    "a + b",
                    "x == y",
                    "x != y",
                    "x <= y",
                    "x >= y",
                    "x < y",
                    "x > y",
                    "a = b;",
                    "(a + b) * c",
                    "- x + y",
                    "a = 1; b = 2;",

                    "//",
                    "// comment",
                    "//comment\nx = 1",
                    "x = 1; // comment",
                    "// comment\n b = 6;",

                    "function main() { returns 0; }",
                    "function main(a:int) returns r:int { r = a; }",
                    "if (x == 0) { y = 1; } else { y = 2; }",
                    "while (x != y) { x = x - 1; }",
                    "assert x > 0",
                    "assume x >= 0",
                    "invariant x > 0",

                    "int a = 5; // comment\n b = 6;",
                    "int a = 5;\nint b = 6;",
                    "x = 42; // the answer",
                    "my_var_1 == _hidden",
                    "if (x >= 10) // test",
                    "πabc",
                    "abcπ"
            );
        } else {
            String content = Files.readString(Paths.get(testFile));
            String[] blocks = content.split("(?m)^\\s*---\\s*$");
            for (String block : blocks) {
                String test = block.strip();
                if (!test.isEmpty()) {
                    tests.add(test);
                }
            }
        }
        if (tests.isEmpty())
            throw new IOException("Invalid test file");
        else
            for (String test : tests) Lexer.tokenize(test, start);
    }



    private static void printHelp() {
        System.out.println("""
        Usage:
          --nfa            print NFA
          --dfa            print DFA
          --min            print DFA size before and after minimization
          --test           run test
          --test-file FILE 
          --json PATH      DFA in JSON (default: lexer_dfa.json)
          --help
        """);
    }
}
