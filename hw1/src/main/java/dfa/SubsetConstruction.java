package dfa;

import nfa.NFA;
import nfa.NFAState;

import java.util.*;

public class SubsetConstruction {

    public static Set<NFAState> epsilonClosure(Set<NFAState> states) {
        Set<NFAState> closure = new HashSet<>(states);
        Deque<NFAState> stack = new ArrayDeque<>(states);
        while (!stack.isEmpty()) {
            NFAState s = stack.pop();
            for (NFAState eps : s.epsilons) {
                if (closure.add(eps)) stack.push(eps);
            }
        }
        return closure;
    }

    public static DFAState convert(NFA nfa, List<DFAState> dfaStatesOut) {
        DFAState.resetCounter();
        Map<Set<NFAState>, DFAState> map = new HashMap<>();

        Set<NFAState> startSet = epsilonClosure(Set.of(nfa.start));
        DFAState startDFA = new DFAState();
        startDFA.nfaStates = startSet;
        resolveAccept(startDFA);

        map.put(startSet, startDFA);
        dfaStatesOut.add(startDFA);

        Deque<DFAState> queue = new ArrayDeque<>();
        queue.add(startDFA);

        while (!queue.isEmpty()) {
            DFAState current = queue.poll();

            for (char c = 0; c < 128; c++) {
                Set<NFAState> moveSet = new HashSet<>();
                for (NFAState ns : current.nfaStates) {
                    if (ns.trans[c] != null) moveSet.addAll(ns.trans[c]);
                }
                if (moveSet.isEmpty()) continue;

                Set<NFAState> closure = epsilonClosure(moveSet);

                DFAState nextDFA = map.get(closure);
                if (nextDFA == null) {
                    nextDFA = new DFAState();
                    nextDFA.nfaStates = closure;
                    resolveAccept(nextDFA);
                    map.put(closure, nextDFA);
                    dfaStatesOut.add(nextDFA);
                    queue.add(nextDFA);
                }
                current.trans[c] = nextDFA;
            }
        }
        return startDFA;
    }

    private static void resolveAccept(DFAState dfa) {
        for (NFAState ns : dfa.nfaStates) {
            if (ns.accept != null) {
                if (dfa.accept == null || ns.accept.ordinal() < dfa.accept.ordinal()) {
                    dfa.accept = ns.accept;
                }
            }
        }
    }
}