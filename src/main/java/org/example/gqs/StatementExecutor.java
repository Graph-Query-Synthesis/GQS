package org.example.gqs;

import java.util.ArrayList;
import java.util.List;

import org.example.gqs.common.query.Query;

public class StatementExecutor<G extends GlobalState<?, ?, ?>, A extends AbstractAction<G>> {

    private final G globalState;
    private final A[] actions;
    private final ActionMapper<G, A> mapping;
    private final AfterQueryAction queryConsumer;

    @FunctionalInterface
    public interface AfterQueryAction {
        void notify(Query<?> q) throws Exception;
    }

    @FunctionalInterface
    public interface ActionMapper<T, A> {
        long map(T globalState, A action);
    }

    public StatementExecutor(G globalState, A[] actions, ActionMapper<G, A> mapping, AfterQueryAction queryConsumer) {
        this.globalState = globalState;
        this.actions = actions.clone();
        this.mapping = mapping;
        this.queryConsumer = queryConsumer;
    }

    @SuppressWarnings("unchecked")
    public void executeStatements() throws Exception {
        Randomly r = globalState.getRandomly();
        int[] nrRemaining = new int[actions.length];
        List<A> availableActions = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < actions.length; i++) {
            A action = actions[i];
            long nrPerformed = mapping.map(globalState, action);
            if (nrPerformed != 0) {
                availableActions.add(action);
            }
            nrRemaining[i] = (int) nrPerformed;
            total += nrPerformed;
        }
        while (total != 0) {
            A nextAction = null;
            long selection = r.getInteger(0, total);
            long previousRange = 0;
            long i;
            for (i = 0; i < nrRemaining.length; i++) {
                if (previousRange <= selection && selection < previousRange + nrRemaining[(int) i]) {
                    nextAction = actions[(int) i];
                    break;
                } else {
                    previousRange += nrRemaining[(int) i];
                }
            }
            assert nextAction != null;
            assert nrRemaining[(int) i] > 0;
            nrRemaining[(int) i]--;
            @SuppressWarnings("rawtypes")
            Query query = null;
            try {
                boolean success;
                long nrTries = 0;
                do {
                    query = nextAction.getQuery(globalState);
                    success = globalState.executeStatement(query);
                } while (nextAction.canBeRetried() && !success
                        && nrTries++ < globalState.getOptions().getNrStatementRetryCount());
            } catch (IgnoreMeException e) {

            }
            if (query != null && query.couldAffectSchema()) {
                globalState.updateSchema();
                queryConsumer.notify(query);
            }
            total--;
        }
    }
}
