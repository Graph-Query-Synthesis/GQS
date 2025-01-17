package org.example.gqs.redisGraph.oracle;

import org.example.gqs.redisGraph.RedisGraphSchema;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.redisGraph.RedisGraphGlobalState;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;

public class RedisGraphAlwaysTrueOracle implements TestOracle {

    private final RedisGraphGlobalState globalState;
    private RandomQueryGenerator<RedisGraphSchema, RedisGraphGlobalState> randomQueryGenerator;

    public RedisGraphAlwaysTrueOracle(RedisGraphGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<RedisGraphSchema, RedisGraphGlobalState>();
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = randomQueryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);

        System.out.println(sb);
    }
}
