package org.example.gqs.tinkerGraph.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.tinkerGraph.TinkerGlobalState;
import org.example.gqs.tinkerGraph.schema.TinkerSchema;

import java.util.ArrayList;
import java.util.List;

public class TinkerSchemaGenerator extends CypherSchemaGenerator<TinkerSchema, TinkerGlobalState> {


    public TinkerSchemaGenerator(TinkerGlobalState globalState){
        super(globalState);
    }

    @Override
    public TinkerSchema generateSchemaObject(TinkerGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {
        return new TinkerSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }
}
