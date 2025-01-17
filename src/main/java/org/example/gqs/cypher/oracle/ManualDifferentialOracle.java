package org.example.gqs.cypher.oracle;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.exceptions.ResultMismatchException;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class ManualDifferentialOracle <G extends CypherGlobalState<?,S>, S extends CypherSchema<G,?>> implements TestOracle {

    private final G globalState;
    private IQueryGenerator<S, G> queryGenerator;

    public static final long BRANCH_PAIR_SIZE = 65536;
    public static final  long BRANCH_SIZE = 1000000;

    public static final long PORT = 9009;
    public static final byte CLEAR = 1, PRINT_MEM = 2;

    private OutputStream outputStream;


    public ManualDifferentialOracle(G globalState, IQueryGenerator<S,G> generator, OutputStream outputStream) {
        this.globalState = globalState;
        this.queryGenerator = generator;
        this.outputStream = outputStream;
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = queryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);
        System.out.println(sb);
        List<GQSResultSet> results;
        long resultLength = 0;

        byte[] branchCoverage = new byte[(int) BRANCH_SIZE];
        byte[] branchPairCoverage = new byte[(int) BRANCH_PAIR_SIZE];

        try {


            results = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString()));

            boolean found = false;
            StringBuilder msgSb = new StringBuilder();
            List<Integer> differenceVector = new ArrayList<>();
            for (int i = 1; i < results.size(); i++) {
                if (results.get(i) == null) {
                    differenceVector.add(3);
                    continue;
                }
                if (!results.get(i).compareWithOutOrder(results.get(0))) {
                    if (!found) {
                        msgSb.append("The contents of the result sets mismatch!\n");
                        found = true;
                    }
                    String msg = "";
                    msg = msg + "Difference between " + (0) + " and " + i;
                    msg = msg + "First: " + results.get(0).getRowNum() + " --- " + results.get(0).resultToStringList() + "\n";
                    msg = msg + "Second: " + results.get(i).getRowNum() + " --- " + results.get(i).resultToStringList() + "\n";
                    msgSb.append(msg);
                    differenceVector.add(1);
                } else {
                    differenceVector.add(0);
                }
            }
            outputStream.write(differenceVector.toString().getBytes());
            outputStream.write("\n".getBytes());
            if (found) {
                throw new ResultMismatchException(msgSb.toString());
            }
            resultLength = results.get(0).getRowNum();
        } catch (CompletionException e) {
            System.out.println("CypherGremlin！");
            System.out.println(e.getMessage());
        }
        boolean isBugDetected = false;

        List<CypherSchema.CypherLabelInfo> labels = globalState.getSchema().getLabels();
        List<CypherSchema.CypherRelationTypeInfo> relations = globalState.getSchema().getRelationTypes();

        queryGenerator.addNewRecord(sequence, isBugDetected, resultLength, branchCoverage, branchPairCoverage);
    }
}
