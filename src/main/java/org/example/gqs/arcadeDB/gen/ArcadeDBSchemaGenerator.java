package org.example.gqs.arcadeDB.gen;

import org.example.gqs.cypher.gen.CypherSchemaGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.arcadeDB.ArcadeDBGlobalState;
import org.example.gqs.arcadeDB.ArcadeDBSchema;

import java.util.ArrayList;
import java.util.List;

public class ArcadeDBSchemaGenerator extends CypherSchemaGenerator<ArcadeDBSchema, ArcadeDBGlobalState> {


    public ArcadeDBSchemaGenerator(ArcadeDBGlobalState globalState){
        super(globalState);
    }

    @Override
    public ArcadeDBSchema generateSchemaObject(ArcadeDBGlobalState globalState, List<CypherSchema.CypherLabelInfo> labels, List<CypherSchema.CypherRelationTypeInfo> relationTypes, List<CypherSchema.CypherPatternInfo> patternInfos) {

        return new ArcadeDBSchema(new ArrayList<>(), labels, relationTypes, patternInfos);
    }

}
