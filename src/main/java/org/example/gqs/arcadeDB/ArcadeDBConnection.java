package org.example.gqs.arcadeDB;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public class ArcadeDBConnection extends CypherConnection {

    public Connection connection;
    public ArcadeDBConnection(Connection connection){
        this.connection = connection;
    }

    @Override
    public String getDatabaseVersion() throws Exception {
        return "arcadedb";
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Override
    public void executeStatement(String arg) throws Exception{
        Statement stmt = connection.createStatement();
        stmt.execute("{cypher}" + arg);
    }

    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        Statement stmt = connection.createStatement();
        return Arrays.asList(new GQSResultSet(stmt.executeQuery("{cypher}" + arg)));
    }
}
