package org.example.gqs.PrintGraph;

import org.example.gqs.cypher.CypherGlobalState;

public class PrintGraphGlobalState extends CypherGlobalState<PrintGraphOptions, PrintGraphSchema> {

    private PrintGraphSchema PrintGraphSchema = null;

    public PrintGraphGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected PrintGraphSchema readSchema()  {
        return PrintGraphSchema;
    }
}
