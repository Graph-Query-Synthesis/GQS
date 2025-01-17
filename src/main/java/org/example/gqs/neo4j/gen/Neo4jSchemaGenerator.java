package org.example.gqs.neo4j.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.neo4j.Neo4jGlobalState;
import org.example.gqs.neo4j.schema.Neo4jSchema;

import java.util.ArrayList;
import java.util.List;

public class Neo4jSchemaGenerator extends CypherSchemaGenerator<Neo4jSchema, Neo4jGlobalState> {


    public Neo4jSchemaGenerator(Neo4jGlobalState globalState){
        super(globalState);
    }

    @Override
    public Neo4jSchema generateSchemaObject(Neo4jGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {
        return new Neo4jSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }
}
