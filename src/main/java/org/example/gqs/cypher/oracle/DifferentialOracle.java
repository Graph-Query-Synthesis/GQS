package org.example.gqs.cypher.oracle;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.exceptions.ResultMismatchException;

import java.util.List;
import java.util.concurrent.CompletionException;

public class DifferentialOracle <G extends CypherGlobalState<?,S>, S extends CypherSchema<G,?>> implements TestOracle {

    private final G globalState;
    private IQueryGenerator<S, G> queryGenerator;

    public DifferentialOracle(G globalState, IQueryGenerator<S, G> queryGenerator) {
        this.globalState = globalState;
        this.queryGenerator = queryGenerator;
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
                    msg = msg + "Difference between " + (i - 1) + " and " + i;
                    msg = msg + "First: " + results.get(i - 1).getRowNum() + " --- " + results.get(i - 1).resultToStringList() + "\n";
                    msg = msg + "Second: " + results.get(i).getRowNum() + " --- " + results.get(i).resultToStringList() + "\n";
                    throw new ResultMismatchException(msg);
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
    }
}
