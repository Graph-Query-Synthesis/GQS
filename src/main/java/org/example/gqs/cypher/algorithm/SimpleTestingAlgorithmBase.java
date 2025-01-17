package org.example.gqs.cypher.algorithm;

import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.OracleFactory;
import org.example.gqs.StateToReproduce;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.oracle.DifferentialNonEmptyBranchOracle;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.exceptions.DatabaseCrashException;
import org.example.gqs.exceptions.MustRestartDatabaseException;

public abstract class SimpleTestingAlgorithmBase <S extends CypherSchema<G,?>, G extends CypherGlobalState<O, S>,
        O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends CypherConnection> extends CypherTestingAlgorithm<S,G,O,C> {


    public SimpleTestingAlgorithmBase(CypherProviderAdapter<G, S, O> provider) {
        super(provider);
    }

    public abstract IQueryGenerator<S,G> createQueryGenerator();

    @Override
    public void generateAndTestDatabase(G globalState) throws Exception {
        try {
            generateDatabase(globalState);
            globalState.getManager().incrementCreateDatabase();

            TestOracle oracle = new DifferentialNonEmptyBranchOracle<G, S>(globalState, createQueryGenerator());
            for (int i = 0; i < globalState.getOptions().getNrQueries(); i++) {
                try (StateToReproduce.OracleRunReproductionState localState = globalState.getState().createLocalState()) {
                    assert localState != null;
                    try {
                        oracle.check();
                        globalState.getManager().incrementSelectQueryCount();
                    } catch (MustRestartDatabaseException e) {
                        throw e;
                    } catch (DatabaseCrashException e) {
                        if (e.getCause() instanceof MustRestartDatabaseException) {
                            throw new MustRestartDatabaseException(e);
                        }
                        e.printStackTrace();
                        globalState.getLogger().logException(e, globalState.getState());
                    } catch (Exception e) {
                        e.printStackTrace();
                        globalState.getLogger().logException(e, globalState.getState());
                    }
                    assert localState != null;
                    localState.executedWithoutError();
                }
            }
            throw new RuntimeException("total number reached");
        } finally {
            globalState.getConnection().close();
        }
    }

    public abstract void generateDatabase(G globalState) throws Exception;
}
