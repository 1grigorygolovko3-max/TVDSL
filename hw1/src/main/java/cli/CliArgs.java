package cli;

public class CliArgs {
    public boolean nfa = false;
    public boolean dfa = false;
    public boolean min = false;
    public boolean test = false;
    public String jsonPath = "lexer_dfa.json";
    public String testFile = null;
    public boolean help = false;

    public static CliArgs parse(String[] args) {
        CliArgs a = new CliArgs();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--nfa"     -> a.nfa = true;
                case "--dfa"     -> a.dfa = true;
                case "--min"     -> a.min = true;
                case "--test"    -> a.test = true;
                case "--help"    -> a.help = true;
                case "--json"    -> {
                    if (i + 1 < args.length) a.jsonPath = args[++i];
                }
                case "--test-file" -> {
                    if (i + 1 < args.length) a.testFile = args[++i];
                }
                default -> System.err.println("Invalid arg: " + args[i]);
            }
        }

        if (!a.nfa && !a.dfa && !a.min && !a.test && !a.help) {
            a.nfa = a.dfa = a.min = a.test = true;
        }
        return a;
    }
}