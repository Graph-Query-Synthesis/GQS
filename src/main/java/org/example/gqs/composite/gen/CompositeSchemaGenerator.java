package org.example.gqs.composite.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.composite.CompositeGlobalState;
import org.example.gqs.composite.CompositeSchema;

import java.util.ArrayList;
import java.util.List;

public class CompositeSchemaGenerator extends CypherSchemaGenerator<CompositeSchema, CompositeGlobalState> {


    public CompositeSchemaGenerator(CompositeGlobalState globalState){
        super(globalState);
    }

    @Override
    public CompositeSchema generateSchemaObject(CompositeGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {

        return new CompositeSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }

}
