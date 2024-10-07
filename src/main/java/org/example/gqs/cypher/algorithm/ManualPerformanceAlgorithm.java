package org.example.gqs.cypher.algorithm;

import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.IgnoreMeException;
import org.example.gqs.OracleFactory;
import org.example.gqs.StateToReproduce;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.gen.graph.ManualGraphGenerator;
import org.example.gqs.cypher.gen.query.ManualQueryGenerator;
import org.example.gqs.cypher.oracle.ManualPerformanceOracle;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.exceptions.DatabaseCrashException;
import org.example.gqs.exceptions.MustRestartDatabaseException;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ManualPerformanceAlgorithm <S extends CypherSchema<G,?>, G extends CypherGlobalState<O, S>,
        O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends CypherConnection> extends CypherTestingAlgorithm<S,G,O,C>{

    public ManualPerformanceAlgorithm(CypherProviderAdapter<G, S, O> provider) {
        super(provider);
    }

    public static long presentNum = 0;
    public static boolean changed = false;

    @Override
    public void generateAndTestDatabase(G globalState) throws Exception {
        if(!changed){
            presentNum = globalState.getOptions().getManualStarting();
            changed = true;
        }
        try {
            File dir = new File("databases");
            File[] files = dir.listFiles();
            ManualGraphGenerator<G> generator = new ManualGraphGenerator<>();
            ManualQueryGenerator<S, G> queryGenerator = new ManualQueryGenerator<>();

            if (Arrays.stream(files).anyMatch(f -> f.getName().equals("" + presentNum))) {
                generator.loadFile("databases/" + presentNum + "/graph.txt");
                queryGenerator.loadFile("databases/" + presentNum + "/query.txt");
            } else {
                System.exit(0);
            }


            List<CypherQueryAdapter> queries = generator.createGraph(globalState);
            for (CypherQueryAdapter query : queries) {
                globalState.executeStatement(query);
                globalState.getState().logCreateStatement(query);
            }


            globalState.getManager().incrementCreateDatabase();

            ManualPerformanceOracle<G, S> oracle = new ManualPerformanceOracle<G, S>(globalState, queryGenerator);
            for (int i = 0; i < queryGenerator.queries.size(); i++) {
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
                    assert localState != null;
                    localState.executedWithoutError();
                }
            }
            presentNum++;
        } finally {
            globalState.getConnection().close();
        }
    }
}
