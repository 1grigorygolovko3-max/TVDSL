package dfa;

import tokens.TokenType;

import java.util.*;

public class HopcroftMinimizer {

    public static DFAState minimize(List<DFAState> dfaStates,
                                    DFAState startState,
                                    List<DFAState> minDfaStatesOut) {
        DFAState trap = new DFAState();
        trap.accept = TokenType.ERROR;
        for (char c = 0; c < 128; c++) trap.trans[c] = trap;
        dfaStates.add(trap);

        for (DFAState d : dfaStates) {
            for (char c = 0; c < 128; c++) {
                if (d.trans[c] == null) d.trans[c] = trap;
            }
        }

        Map<TokenType, Set<DFAState>> partitionsMap = new HashMap<>();
        Set<DFAState> nonAccepting = new HashSet<>();

        for (DFAState d : dfaStates) {
            if (d.accept == null) {
                nonAccepting.add(d);
            } else {
                partitionsMap.computeIfAbsent(d.accept, k -> new HashSet<>()).add(d);
            }
        }

        List<Set<DFAState>> partitions = new ArrayList<>(partitionsMap.values());
        if (!nonAccepting.isEmpty()) partitions.add(nonAccepting);

        Deque<Set<DFAState>> worklist = new ArrayDeque<>(partitions);
        while (!worklist.isEmpty()) {
            Set<DFAState> A = worklist.poll();

            for (char c = 0; c < 128; c++) {
                Set<DFAState> X = new HashSet<>();
                for (DFAState d : dfaStates) {
                    if (A.contains(d.trans[c])) X.add(d);
                }
                if (X.isEmpty()) continue;

                List<Set<DFAState>> nextP = new ArrayList<>();
                for (Set<DFAState> Y : partitions) {
                    Set<DFAState> intersect = new HashSet<>(Y);
                    intersect.retainAll(X);
                    Set<DFAState> diff = new HashSet<>(Y);
                    diff.removeAll(X);

                    if (!intersect.isEmpty() && !diff.isEmpty()) {
                        nextP.add(intersect);
                        nextP.add(diff);
                        if (worklist.contains(Y)) {
                            worklist.remove(Y);
                            worklist.add(intersect);
                            worklist.add(diff);
                        } else {
                            worklist.add(intersect.size() <= diff.size() ? intersect : diff);
                        }
                    } else {
                        nextP.add(Y);
                    }
                }
                partitions = nextP;
            }
        }

        DFAState.resetCounter();
        Map<Set<DFAState>, DFAState> pToMin = new HashMap<>();
        DFAState minStart = null;

        for (Set<DFAState> partition : partitions) {
            DFAState m = new DFAState();
            m.accept = partition.iterator().next().accept;
            pToMin.put(partition, m);
            minDfaStatesOut.add(m);
            if (partition.contains(startState)) minStart = m;
        }

        for (Set<DFAState> partition : partitions) {
            DFAState m = pToMin.get(partition);
            DFAState oldRep = partition.iterator().next();
            for (char c = 0; c < 128; c++) {
                DFAState target = oldRep.trans[c];
                for (Set<DFAState> pTarget : partitions) {
                    if (pTarget.contains(target)) {
                        m.trans[c] = pToMin.get(pTarget);
                        break;
                    }
                }
            }
        }

        return minStart;
    }
}