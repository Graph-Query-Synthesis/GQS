package org.example.gqs;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.example.gqs.common.query.Query;

public class StateToReproduce {

    public final List<Query<?>> statements = new ArrayList<>();

    public final List<Query<?>> createStatements = new ArrayList<>();

    private final String databaseName;

    private final DatabaseProvider<?, ?, ?> databaseProvider;

    public String databaseVersion;

    protected long seedValue;

    String exception;

    public OracleRunReproductionState localState;

    public StateToReproduce(String databaseName, DatabaseProvider<?, ?, ?> databaseProvider) {
        this.databaseName = databaseName;
        this.databaseProvider = databaseProvider;
    }

    public String getException() {
        return exception;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseVersion() {
        return databaseVersion;
    }

    public void logStatement(String queryString) {
        if (queryString == null) {
            throw new IllegalArgumentException();
        }
        System.out.println(queryString);
        logStatement(databaseProvider.getLoggableFactory().getQueryForStateToReproduce(queryString));
    }

    public void logStatement(Query<?> query) {
        if (query == null) {
            throw new IllegalArgumentException();
        }
        statements.add(query);
    }

    public void logCreateStatement(Query<?> query){
        if (query == null) {
            throw new IllegalArgumentException();
        }
        createStatements.add(query);
    }

    public List<Query<?>> getStatements() {
        return Collections.unmodifiableList(statements);
    }

    public List<Query<?>> getCreateStatements() {
        return Collections.unmodifiableList(createStatements);
    }

    @Deprecated
    public void commentStatements() {
        for (int i = 0; i < statements.size(); i++) {
            Query<?> statement = statements.get(i);
            Query<?> newQuery = databaseProvider.getLoggableFactory().commentOutQuery(statement);
            statements.set(i, newQuery);
        }
    }

    public long getSeedValue() {
        return seedValue;
    }

    public OracleRunReproductionState getLocalState() {
        return localState;
    }

    public class OracleRunReproductionState implements Closeable {

        private final List<Query<?>> statements = new ArrayList<>();

        public boolean success;

        public OracleRunReproductionState() {
            StateToReproduce.this.localState = this;
        }

        public void executedWithoutError() {
            this.success = true;
        }

        public void log(String s) {
            statements.add(databaseProvider.getLoggableFactory().getQueryForStateToReproduce(s));
        }

        @Override
        public void close() {
            if (!success) {
                StateToReproduce.this.statements.addAll(statements);
            }

        }

    }

    public OracleRunReproductionState createLocalState() {
        return new OracleRunReproductionState();
    }

}
