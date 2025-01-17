package org.example.gqs.cypher;

import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.ExecutionTimer;
import org.example.gqs.GlobalState;
import org.example.gqs.common.query.Query;
import org.example.gqs.common.schema.AbstractSchema;

public abstract class CypherGlobalState <O extends DBMSSpecificOptions<?>, S extends AbstractSchema<?, ?>>
        extends GlobalState<O, S, CypherConnection> {
    @Override
    protected void executeEpilogue(Query<?> q, boolean success, ExecutionTimer timer) throws Exception {
        boolean logExecutionTime = getOptions().logExecutionTime();
        if (success && getOptions().printSucceedingStatements()) {
            System.out.println(q.getQueryString());
        }
        if (logExecutionTime) {
            getLogger().writeCurrent(" -- " + timer.end().asString());
        }
        if (q.couldAffectSchema()) {
            updateSchema();
        }
    }
}
