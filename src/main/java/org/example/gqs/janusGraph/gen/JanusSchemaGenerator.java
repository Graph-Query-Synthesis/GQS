package org.example.gqs.janusGraph.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.janusGraph.JanusGlobalState;
import org.example.gqs.janusGraph.schema.JanusSchema;

import java.util.ArrayList;
import java.util.List;

public class JanusSchemaGenerator extends CypherSchemaGenerator<JanusSchema, JanusGlobalState> {


    public JanusSchemaGenerator(JanusGlobalState globalState){
        super(globalState);
    }

    @Override
    public JanusSchema generateSchemaObject(JanusGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {
        return new JanusSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }
}
