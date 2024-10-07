package org.example.gqs.janusGraph;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.janusGraph.gen.JanusSchemaGenerator;
import org.example.gqs.janusGraph.schema.JanusSchema;

public class JanusGlobalState extends CypherGlobalState<JanusOptions, org.example.gqs.janusGraph.schema.JanusSchema> {

    private JanusSchema JanusSchema = null;

    public JanusGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected JanusSchema readSchema()  {
        if(JanusSchema == null){
            JanusSchema = new JanusSchemaGenerator(this).generateSchema();
        }
        return JanusSchema;
    }
}
