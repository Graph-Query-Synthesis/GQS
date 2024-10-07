package org.example.gqs.agensGraph;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public class AgensGraphConnection extends CypherConnection {

    private Connection connection;

    public AgensGraphConnection(Connection connection){
        this.connection = connection;
    }

    @Override
    public String getDatabaseVersion() throws Exception {
        return "agensgraph";
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Override
    public void executeStatement(String arg) throws Exception{
        Statement stmt = connection.createStatement();
        stmt.execute(arg);
    }

    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        Statement stmt = connection.createStatement();
        return Arrays.asList(new GQSResultSet(stmt.executeQuery(arg)));
    }
}
