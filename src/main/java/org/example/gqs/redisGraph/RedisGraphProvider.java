package org.example.gqs.redisGraph;

import com.falkordb.FalkorDB;
import com.google.gson.JsonObject;
import org.example.gqs.common.log.LoggableFactory;

import org.example.gqs.cypher.CypherLoggableFactory;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.redisGraph.gen.RedisGraphGraphGenerator;
import org.example.gqs.MainOptions;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherProviderAdapter;
import java.util.List;

public class RedisGraphProvider extends CypherProviderAdapter<RedisGraphGlobalState, RedisGraphSchema, RedisGraphOptions> {
    public RedisGraphProvider() {
        super(RedisGraphGlobalState.class, RedisGraphOptions.class);
    }

    @Override
    public CypherConnection createDatabase(RedisGraphGlobalState globalState) throws Exception {
        String databaseName = globalState.getDatabaseName();
        long port = 0;
        if(databaseName.contains("-"))
        {
            String[] split = databaseName.split("-");
            Integer number = Integer.parseInt(split[2]);
            port = 20000 + number;
            globalState.dbmsSpecificOptions.port = port;
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
        }
        else
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
    }

    @Override
    public String getDBMSName() {
        return "falkordb";
    }

    @Override
    public LoggableFactory getLoggableFactory() {
        return new CypherLoggableFactory();
    }

    @Override
    protected void checkViewsAreValid(RedisGraphGlobalState globalState) {

    }

    @Override
    public void generateDatabase(RedisGraphGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = RedisGraphGraphGenerator.createGraph(globalState);
        for (CypherQueryAdapter query : queries) {
            globalState.executeStatement(query);
        }
    }

    @Override
    public RedisGraphOptions generateOptionsFromConfig(JsonObject config) {
        return RedisGraphOptions.parseOptionFromFile(config);
    }

    @Override
    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, RedisGraphOptions specificOptions) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = specificOptions.getPort();
        if (host == null) {
            host = RedisGraphOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = RedisGraphOptions.DEFAULT_PORT;
        }
        RedisGraphConnection con = null;
        try{

            con = new RedisGraphConnection(specificOptions, FalkorDB.driver("127.0.0.1", (int) port).graph("social"), "social");
            con.executeStatement("MATCH (n) DETACH DELETE n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }
}
