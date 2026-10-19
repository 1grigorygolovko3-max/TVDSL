package dfa;

import nfa.NFAState;
import tokens.TokenType;

import java.util.HashSet;
import java.util.Set;

public class DFAState {
    private static int globalId = 0;

    public final int id;
    public final DFAState[] trans;
    public TokenType accept = null;
    public Set<NFAState> nfaStates = new HashSet<>();

    public DFAState() {
        this.id = globalId++;
        this.trans = new DFAState[128];
    }

    public static void resetCounter() {
        globalId = 0;
    }
}