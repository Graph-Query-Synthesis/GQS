package org.example.gqs.memGraph.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.memGraph.MemGraphGlobalState;
import org.example.gqs.memGraph.MemGraphSchema;

import java.util.ArrayList;
import java.util.List;

public class MemGraphSchemaGenerator extends CypherSchemaGenerator<MemGraphSchema, MemGraphGlobalState> {


    public MemGraphSchemaGenerator(MemGraphGlobalState globalState){
        super(globalState);
    }

    @Override
    public MemGraphSchema generateSchemaObject(MemGraphGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {
        return new MemGraphSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }

}
