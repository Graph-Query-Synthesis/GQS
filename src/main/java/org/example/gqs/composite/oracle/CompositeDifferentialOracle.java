package org.example.gqs.composite.oracle;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.composite.CompositeSchema;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.composite.CompositeGlobalState;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;

import java.util.List;
import java.util.concurrent.CompletionException;

public class CompositeDifferentialOracle implements TestOracle {

    private final CompositeGlobalState globalState;
    private IQueryGenerator<CompositeSchema, CompositeGlobalState> queryGenerator;

    public CompositeDifferentialOracle(CompositeGlobalState globalState) {
        this.globalState = globalState;
        this.queryGenerator = globalState.getDbmsSpecificOptions().getQueryGenerator();
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = queryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);
        System.out.println(sb);
        List<GQSResultSet> results;
        long resultLength = 0;
        try {
            results = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString()));
            for (int i = 1; i < results.size(); i++) {
                if (!results.get(i).compareWithOutOrder(results.get(i - 1))) {
                    String msg = "The contents of the result sets mismatch!\n";
                    msg = msg + "First: " + results.get(i - 1).getRowNum() + " --- " + results.get(i - 1).resultToStringList() + "\n";
                    msg = msg + "Second: " + results.get(i).getRowNum() + " --- " + results.get(i).resultToStringList() + "\n";
                    throw new AssertionError(msg);
                }
            }
            resultLength = results.get(0).getRowNum();
        } catch (CompletionException e) {
            System.out.println("CypherGremlin！");
            System.out.println(e.getMessage());
        }
        boolean isBugDetected = false;

        List<CypherSchema.CypherLabelInfo> labels = globalState.getSchema().getLabels();
        List<CypherSchema.CypherRelationTypeInfo> relations = globalState.getSchema().getRelationTypes();
        if (resultLength > 0) {
            queryGenerator.addExecutionRecord(sequence, isBugDetected, resultLength);
        }
    }
}
