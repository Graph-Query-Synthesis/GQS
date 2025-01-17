package org.example.gqs.agensGraph;

import com.google.gson.JsonObject;
import org.example.gqs.MainOptions;
import org.example.gqs.common.log.LoggableFactory;

import org.example.gqs.agensGraph.gen.AgensGraphGraphGenerator;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherLoggableFactory;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class AgensGraphProvider extends CypherProviderAdapter<AgensGraphGlobalState, AgensGraphSchema, AgensGraphOptions> {
    public AgensGraphProvider() {
        super(AgensGraphGlobalState.class, AgensGraphOptions.class);
    }

    @Override
    public CypherConnection createDatabase(AgensGraphGlobalState globalState) throws Exception {
        return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
    }

    @Override
    public String getDBMSName() {
        return "agensgraph";
    }

    @Override
    public LoggableFactory getLoggableFactory() {
        return new CypherLoggableFactory();
    }

    @Override
    protected void checkViewsAreValid(AgensGraphGlobalState globalState) {

    }

    @Override
    public void generateDatabase(AgensGraphGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = AgensGraphGraphGenerator.createGraph(globalState);
        for (CypherQueryAdapter query : queries) {
            globalState.executeStatement(query);
        }
    }

    @Override
    public AgensGraphOptions generateOptionsFromConfig(JsonObject config) {
        return AgensGraphOptions.parseOptionFromFile(config);
    }

    @Override
    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, AgensGraphOptions specificOptions) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = specificOptions.getPort();
        if (host == null) {
            host = AgensGraphOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = AgensGraphOptions.DEFAULT_PORT;
        }
        AgensGraphConnection con = null;
        try{
            Class.forName("net.bitnine.agensgraph.Driver");
            String connectionString = "jdbc:agensgraph://"+host+":"+port+"/postgres";
            Connection connection = DriverManager.getConnection(connectionString, username, password);
            con = new AgensGraphConnection(connection);
            con.executeStatement("DROP GRAPH IF EXISTS sqlancer CASCADE");
            con.executeStatement("CREATE GRAPH sqlancer");
            con.executeStatement("SET graph_path = sqlancer");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }
}
