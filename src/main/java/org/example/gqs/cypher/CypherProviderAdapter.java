package org.example.gqs.cypher;

import org.example.gqs.cypher.algorithm.*;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.MainOptions;
import org.example.gqs.OracleFactory;
import org.example.gqs.ProviderAdapter;

public abstract  class CypherProviderAdapter <G extends CypherGlobalState<O, S>, S extends CypherSchema<G,?>, O extends DBMSSpecificOptions<? extends OracleFactory<G>>> extends ProviderAdapter<G, O, CypherConnection> {

    public CypherProviderAdapter(Class<G> globalClass, Class<O> optionClass) {
        super(globalClass, optionClass);
    }

    @Override
    public void generateAndTestDatabase(G globalState) throws Exception {
        CypherTestingAlgorithm<S, G, O, CypherConnection> algorithm;
        switch (globalState.getOptions().getAlgorithm()) {
            case SIMPLE:
                algorithm = new SimpleTestingAlgorithm<S, G, O, CypherConnection>(this);
                break;
            case PATTERN_GUIDED:
                algorithm = new NonEmptyAlgorithm<>(this);
                break;
            case MANUAL:
                algorithm = new ManualDifferentialAlgorithm<>(this);
                break;
            case NON_EMPTY:
                algorithm = new CoverageGuidedAlgorithm<>(this);
                break;
            case COMPARED1:
                algorithm = new Compared1AlgorithmNew<>(this);
                break;
            case COMPARED2:
                algorithm = new Compared2AlgorithmNew<>(this);
                break;
            case COMPARED3:
                algorithm = new Compared3AlgorithmNew<>(this);
                break;
            case COMPARED4:
                algorithm = new Compared4Algorithm<>(this);
                break;
            case COMPARED5:
                algorithm = new Compared5Algorithm<>(this);
                break;
            case MANUAL_PERF:
                algorithm = new ManualPerformanceAlgorithm<>(this);
                break;
            case ENUM:
                algorithm = new EnumerationAlgorithm<>(this);
                break;
            default:
                throw new RuntimeException();
        }
        algorithm.generateAndTestDatabase(globalState);
        System.gc();
    }

    @Override
    protected void checkViewsAreValid(G globalState){

    }

    public abstract CypherConnection createDatabaseWithOptions(MainOptions mainOptions, O specificOptions) throws Exception;

}
