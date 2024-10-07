package org.example.gqs.redisGraph.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.redisGraph.RedisGraphGlobalState;
import org.example.gqs.redisGraph.RedisGraphSchema;

import java.util.ArrayList;
import java.util.List;

public class RedisGraphSchemaGenerator extends CypherSchemaGenerator<RedisGraphSchema, RedisGraphGlobalState> {


    public RedisGraphSchemaGenerator(RedisGraphGlobalState globalState){
        super(globalState);
    }

    @Override
    public RedisGraphSchema generateSchemaObject(RedisGraphGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {

        return new RedisGraphSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }

}
