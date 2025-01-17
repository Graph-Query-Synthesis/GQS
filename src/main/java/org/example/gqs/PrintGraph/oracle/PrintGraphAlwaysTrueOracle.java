package org.example.gqs.PrintGraph.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;
import org.example.gqs.PrintGraph.PrintGraphGlobalState;
import org.example.gqs.PrintGraph.PrintGraphSchema;

public class PrintGraphAlwaysTrueOracle implements TestOracle {

    private final PrintGraphGlobalState globalState;
    private RandomQueryGenerator<PrintGraphSchema, PrintGraphGlobalState> randomQueryGenerator;

    public PrintGraphAlwaysTrueOracle(PrintGraphGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<PrintGraphSchema, PrintGraphGlobalState>();
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = randomQueryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);

        System.out.println(sb);
    }
}
