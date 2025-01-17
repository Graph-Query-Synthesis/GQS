package org.example.gqs.kuzuGraph.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.kuzuGraph.KuzuGraphGlobalState;
import org.example.gqs.kuzuGraph.KuzuGraphSchema;

import java.util.ArrayList;
import java.util.List;

public class KuzuGraphSchemaGenerator extends CypherSchemaGenerator<KuzuGraphSchema, KuzuGraphGlobalState> {


    public KuzuGraphSchemaGenerator(KuzuGraphGlobalState globalState){
        super(globalState);
    }

    @Override
    public KuzuGraphSchema generateSchemaObject(KuzuGraphGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {
        return new KuzuGraphSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }

}
