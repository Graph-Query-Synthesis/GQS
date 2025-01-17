package org.example.gqs.arcadeDB.oracle;

import org.example.gqs.arcadeDB.ArcadeDBSchema;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.arcadeDB.ArcadeDBGlobalState;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;

public class ArcadeDBAlwaysTrueOracle implements TestOracle {

    private final ArcadeDBGlobalState globalState;
    private RandomQueryGenerator<ArcadeDBSchema, ArcadeDBGlobalState> randomQueryGenerator;

    public ArcadeDBAlwaysTrueOracle(ArcadeDBGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<ArcadeDBSchema, ArcadeDBGlobalState>();
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
