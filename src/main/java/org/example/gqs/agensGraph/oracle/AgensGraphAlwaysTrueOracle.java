package org.example.gqs.agensGraph.oracle;

import org.example.gqs.agensGraph.AgensGraphSchema;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.agensGraph.AgensGraphGlobalState;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;

public class AgensGraphAlwaysTrueOracle implements TestOracle {

    private final AgensGraphGlobalState globalState;
    private RandomQueryGenerator<AgensGraphSchema, AgensGraphGlobalState> randomQueryGenerator;

    public AgensGraphAlwaysTrueOracle(AgensGraphGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<AgensGraphSchema, AgensGraphGlobalState>();
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = randomQueryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);

        System.out.println(sb);
        GQSResultSet r = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString())).get(0);
        System.out.println(r.getResult());
    }
}
