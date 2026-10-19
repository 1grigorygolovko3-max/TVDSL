package nfa;

public class NFA {
    public final NFAState start;
    public final NFAState end;

    public NFA(NFAState start, NFAState end) {
        this.start = start;
        this.end = end;
    }
}