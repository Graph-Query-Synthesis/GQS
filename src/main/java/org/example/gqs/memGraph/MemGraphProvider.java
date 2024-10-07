package org.example.gqs.memGraph;

import com.google.gson.JsonObject;
import org.example.gqs.common.log.LoggableFactory;

import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherLoggableFactory;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.memGraph.gen.MemGraphGraphGenerator;
import org.example.gqs.MainOptions;
import org.neo4j.driver.Driver;

import java.util.List;

public class MemGraphProvider extends CypherProviderAdapter<MemGraphGlobalState, MemGraphSchema, MemGraphOptions> {
    public MemGraphProvider() {
        super(MemGraphGlobalState.class, MemGraphOptions.class);
    }

    @Override
    public CypherConnection createDatabase(MemGraphGlobalState globalState) throws Exception {
        String databaseName = globalState.getDatabaseName();
        long port = 0;
        if(databaseName.contains("-"))
        {
            String[] split = databaseName.split("-");
            Integer number = Integer.parseInt(split[2]);
            port = 20000 + number;
            globalState.dbmsSpecificOptions.port = port;
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions(), port);
        }
        else
        {
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
        }

    }

    @Override
    public String getDBMSName() {
        return "memgraph";
    }

    @Override
    public LoggableFactory getLoggableFactory() {
        return new CypherLoggableFactory();
    }

    @Override
    protected void checkViewsAreValid(MemGraphGlobalState globalState) {

    }

    @Override
    public void generateDatabase(MemGraphGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = MemGraphGraphGenerator.createGraph(globalState);
        for(CypherQueryAdapter query : queries){
            globalState.executeStatement(query);
        }
    }

    @Override
    public MemGraphOptions generateOptionsFromConfig(JsonObject config) {
        return MemGraphOptions.parseOptionFromFile(config);
    }

    @Override
    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, MemGraphOptions specificOptions) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = specificOptions.getPort();
        if (host == null) {
            host = MemGraphOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = MemGraphOptions.DEFAULT_PORT;
        }

        String url = String.format("bolt://%s:%d", host, port);


        Driver driver = MemGraphDriverManager.getDriver(url, username, password);
        MemGraphConnection con = new MemGraphConnection(driver, specificOptions);
        con.executeStatement("MATCH (n) DETACH DELETE n");
        return con;
    }

    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, MemGraphOptions specificOptions, long databaseNo) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = databaseNo;
        if (host == null) {
            host = MemGraphOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = MemGraphOptions.DEFAULT_PORT;
        }

        String url = String.format("bolt://%s:%d", host, port);


        Driver driver = MemGraphDriverManager.getDriver(url, username, password);
        MemGraphConnection con = new MemGraphConnection(driver, specificOptions);
        con.executeStatement("MATCH (n) DETACH DELETE n");
        return con;
    }
}
