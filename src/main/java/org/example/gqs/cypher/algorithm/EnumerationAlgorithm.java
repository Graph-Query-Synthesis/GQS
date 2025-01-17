package org.example.gqs.cypher.algorithm;

import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.IgnoreMeException;
import org.example.gqs.OracleFactory;
import org.example.gqs.StateToReproduce;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.gen.*;
import org.example.gqs.cypher.gen.graph.EnumerationGraphGenerator;
import org.example.gqs.cypher.gen.query.EnumerationQueryGenerator;
import org.example.gqs.cypher.oracle.DifferentialNonEmptyBranchOracle;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.exceptions.DatabaseCrashException;
import org.example.gqs.exceptions.MustRestartDatabaseException;

import java.util.List;

public class EnumerationAlgorithm<S extends CypherSchema<G,?>, G extends CypherGlobalState<O, S>,
        O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends CypherConnection> extends CypherTestingAlgorithm<S,G,O,C>{
    private static EnumerationSeq graphSeq = new EnumerationSeq();


    public EnumerationAlgorithm(CypherProviderAdapter<G, S, O> provider) {
        super(provider);
    }

    @Override
    public void generateAndTestDatabase(G globalState) throws Exception {

        try {
            generateDatabase(globalState);
            globalState.getManager().incrementCreateDatabase();

            EnumerationSeq querySeq = new EnumerationSeq();
            TestOracle oracle = new DifferentialNonEmptyBranchOracle<G, S>(globalState, new EnumerationQueryGenerator<>(querySeq));

            for (int i = 0; i < globalState.getOptions().getNrQueries(); i++) {
                try (StateToReproduce.OracleRunReproductionState localState = globalState.getState().createLocalState()) {
                    assert localState != null;
                    try {
                        oracle.check();
                        globalState.getManager().incrementSelectQueryCount();
                    } catch (IgnoreMeException e) {
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

                    querySeq.finish();
                    querySeq.printPresent();
                    querySeq.randomize();

                    assert localState != null;
                    localState.executedWithoutError();

                }
            }
            throw new RuntimeException("total number reached");
        } finally {
            globalState.getConnection().close();
        }
    }

    public void generateDatabase(G globalState) throws Exception{
        EnumerationGraphGenerator<G,S> generator = new EnumerationGraphGenerator<>(globalState, graphSeq);
        List<CypherQueryAdapter> queries = generator.createGraph(globalState);

        for(CypherQueryAdapter query : queries){
            globalState.executeStatement(query);
            globalState.getState().logCreateStatement(query);
        }

        graphSeq.finish();
        graphSeq.randomize();
    }
}
