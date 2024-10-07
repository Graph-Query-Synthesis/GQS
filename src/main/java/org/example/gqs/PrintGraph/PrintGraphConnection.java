package org.example.gqs.PrintGraph;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;

import java.util.Arrays;
import java.util.List;

public class PrintGraphConnection extends CypherConnection {
    private String graphName;

    private PrintGraphOptions options;

    public PrintGraphConnection(PrintGraphOptions options){
         this.options = options;
    }


    @Override
    public String getDatabaseVersion() {
        return "Printgraph";
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void executeStatement(String arg) throws Exception{
    }

    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        return Arrays.asList(new GQSResultSet());
    }
}
