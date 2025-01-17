package org.example.gqs.common.query;

import org.example.gqs.GlobalState;
import org.example.gqs.GDSmithDBConnection;
import org.example.gqs.common.log.Loggable;

import java.util.List;

public abstract class Query<C extends GDSmithDBConnection> implements Loggable {

    public abstract String getQueryString();

    public abstract String getUnterminatedQueryString();

    public abstract boolean couldAffectSchema();

    public abstract <G extends GlobalState<?, ?, C>> boolean execute(G globalState, String... fills) throws Exception;

    public abstract ExpectedErrors getExpectedErrors();

    @Override
    public String toString() {
        return getQueryString();
    }

    public <G extends GlobalState<?, ?, C>> List<GQSResultSet> executeAndGet(G globalState, String... fills)
            throws Exception {
        throw new AssertionError();
    }

    public <G extends GlobalState<?, ?, C>> List<Long> executeAndGetTime(G globalState, String... fills)
            throws Exception {
        throw new AssertionError();
    }

    public <G extends GlobalState<?, ?, C>> boolean executeLogged(G globalState) throws Exception {
        logQueryString(globalState);
        return execute(globalState);
    }

    public <G extends GlobalState<?, ?, C>> List<GQSResultSet> executeAndGetLogged(G globalState) throws Exception {
        logQueryString(globalState);
        return executeAndGet(globalState);
    }

    private <G extends GlobalState<?, ?, C>> void logQueryString(G globalState) {
        if (globalState.getOptions().logEachSelect()) {
            globalState.getLogger().writeCurrent(getQueryString());
        }
    }

}
