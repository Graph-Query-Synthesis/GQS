package org.example.gqs.neo4j;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Arrays;
import java.util.List;

public class Neo4jConnection extends CypherConnection {


    private Driver driver;
    private Neo4jOptions options;
    public long port;

    public DatabaseManagementService managementService = null;
    public GraphDatabaseService databaseService = null;

    public Neo4jConnection(Driver driver, Neo4jOptions options) {
        this.driver = driver;
        this.options = options;
    }

    public Neo4jConnection(DatabaseManagementService management, GraphDatabaseService service, Long port) {
        this.managementService = management;
        this.databaseService = service;
        this.port = port;
    }


    @Override
    public String getDatabaseVersion() throws Exception {
        return "neo4j";
    }


    @Override
    public void close() throws Exception {
        Neo4jDriverManager.closeDriver(driver);
    }


    @Override
    public void executeStatement(String arg) throws Exception {
        try (Session session = driver.session()) {
            String greeting = session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {
                    tx.run(arg);
                    return "";
                }
            });
        }
    }


    @Override

    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception {
        try (Session session = driver.session()) {
            GQSResultSet resultSet = new GQSResultSet(session.run(arg));
            if (resultSet.getResult() != null)
                resultSet.resolveFloat();
            return Arrays.asList(resultSet);
        }
    }

    public void reproduce(List<String> queries) {
    }
}
