package org.example.gqs.composite.oracle;

import org.example.gqs.composite.CompositeSchema;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.composite.CompositeGlobalState;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;

public class CompositeAlwaysTrueOracle implements TestOracle {

    private final CompositeGlobalState globalState;
    private RandomQueryGenerator<CompositeSchema, CompositeGlobalState> randomQueryGenerator;

    public CompositeAlwaysTrueOracle(CompositeGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<CompositeSchema, CompositeGlobalState>();
    }

    @Override
    public void check() throws Exception {
    }
}
