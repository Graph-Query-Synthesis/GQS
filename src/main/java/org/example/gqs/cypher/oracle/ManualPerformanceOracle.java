package org.example.gqs.cypher.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletionException;

public class ManualPerformanceOracle <G extends CypherGlobalState<?,S>, S extends CypherSchema<G,?>> implements TestOracle {

    private final G globalState;
    private IQueryGenerator<S, G> queryGenerator;

    public static final long BRANCH_PAIR_SIZE = 65536;
    public static final  long BRANCH_SIZE = 1000000;

    public static final long PORT = 9009;
    public static final byte CLEAR = 1, PRINT_MEM = 2;

    private OutputStream outputStream;


    public ManualPerformanceOracle(G globalState, IQueryGenerator<S,G> generator) {
        this.globalState = globalState;
        this.queryGenerator = generator;
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = queryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);
        System.out.println(sb);
        long resultLength = 0;

        byte[] branchCoverage = new byte[(int) BRANCH_SIZE];
        byte[] branchPairCoverage = new byte[(int) BRANCH_PAIR_SIZE];

        try {
            List<Long> results = globalState.executeStatementAndGetTime(new CypherQueryAdapter(sb.toString()));
            long time1 = Math.max(results.get(0), 1L);
            long time2 = Math.max(results.get(1), 1L);
            double retime = time1 * 1.0 / time2;
            System.out.println("：" + retime);
        } catch (CompletionException e) {
            System.out.println("CypherGremlin！");
            System.out.println(e.getMessage());
        }
        boolean isBugDetected = false;

        queryGenerator.addNewRecord(sequence, isBugDetected, resultLength, branchCoverage, branchPairCoverage);
    }
}
