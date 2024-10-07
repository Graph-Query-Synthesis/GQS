package org.example.gqs.arcadeDB;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.arcadeDB.gen.ArcadeDBSchemaGenerator;

public class ArcadeDBGlobalState extends CypherGlobalState<ArcadeDBOptions, ArcadeDBSchema> {

    private ArcadeDBSchema arcadeDBSchema = null;

    public ArcadeDBGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected ArcadeDBSchema readSchema()  {
        if(arcadeDBSchema == null){
            arcadeDBSchema = new ArcadeDBSchemaGenerator(this).generateSchema();
        }
        return arcadeDBSchema;
    }
}
