package org.example.gqs.redisGraph;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.redisGraph.gen.RedisGraphSchemaGenerator;

public class RedisGraphGlobalState extends CypherGlobalState<RedisGraphOptions, RedisGraphSchema> {

    private RedisGraphSchema redisGraphSchema = null;

    public RedisGraphGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected RedisGraphSchema readSchema() {
        if(redisGraphSchema == null){
            redisGraphSchema = new RedisGraphSchemaGenerator(this).generateSchema();
        }
        return redisGraphSchema;
    }
}
