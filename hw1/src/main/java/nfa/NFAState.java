package nfa;

import tokens.TokenType;
import java.util.ArrayList;
import java.util.List;

public class NFAState {
    private static int globalId = 0;

    public final int id;
    public final List<NFAState>[] trans;
    public final List<NFAState> epsilons = new ArrayList<>();
    public TokenType accept = null;

    public NFAState() {
        this.id = globalId++;
        this.trans = new List[128];
    }

    public void addTransition(char c, NFAState next) {
        if (trans[c] == null) trans[c] = new ArrayList<>();
        trans[c].add(next);
    }

    public void addEpsilon(NFAState next) {
        epsilons.add(next);
    }

}