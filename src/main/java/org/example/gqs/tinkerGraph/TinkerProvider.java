package org.example.gqs.tinkerGraph;

import com.google.gson.JsonObject;
import org.example.gqs.AbstractAction;
import org.example.gqs.MainOptions;
import org.example.gqs.common.log.LoggableFactory;
import org.example.gqs.cypher.*;
import org.example.gqs.tinkerGraph.gen.TinkerGraphGenerator;
import org.example.gqs.tinkerGraph.gen.TinkerNodeGenerator;
import org.example.gqs.tinkerGraph.schema.TinkerSchema;
import org.apache.tinkerpop.gremlin.driver.Cluster;

import java.util.*;

public class TinkerProvider extends CypherProviderAdapter<TinkerGlobalState, TinkerSchema, TinkerOptions> {
    public TinkerProvider() {
        super(TinkerGlobalState.class, TinkerOptions.class);
    }

    @Override
    public TinkerOptions generateOptionsFromConfig(JsonObject config) {
        return TinkerOptions.parseOptionFromFile(config);
    }

    @Override
    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, TinkerOptions specificOptions) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = specificOptions.getPort();
        if (host == null) {
            host = TinkerOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = TinkerOptions.DEFAULT_PORT;
        }

        String url = String.format("bolt://%s:%d", host, port);
        Cluster cluster;
        if(specificOptions.configFile != null){
            cluster = Cluster.open(specificOptions.configFile);
        }
        else{
            cluster = Cluster.open();
        }
        TinkerConnection con = new TinkerConnection(cluster);
        con.executeStatement("MATCH (n) DETACH DELETE n");
        return con;
    }

    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, TinkerOptions specificOptions, long databaseNo) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = databaseNo;
        if (host == null) {
            host = TinkerOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = TinkerOptions.DEFAULT_PORT;
        }

        String url = String.format("bolt://%s:%d", host, port);
        Cluster cluster;
        cluster = Cluster.build().port((int) port).create();
        TinkerConnection con = new TinkerConnection(cluster);
        con.executeStatement("MATCH (n) DETACH DELETE n");
        return con;
    }

    enum Action implements AbstractAction<TinkerGlobalState> {
        CREATE(TinkerNodeGenerator::createNode);

        private final CypherQueryProvider<TinkerGlobalState> cypherQueryProvider;

        Action(CypherQueryProvider<TinkerGlobalState> cypherQueryProvider) {
            this.cypherQueryProvider = cypherQueryProvider;
        }

        @Override
        public CypherQueryAdapter getQuery(TinkerGlobalState globalState) throws Exception {
            return cypherQueryProvider.getQuery(globalState);
        }
    }

    @Override
    public CypherConnection createDatabase(TinkerGlobalState globalState) throws Exception {
        String databaseName = globalState.getDatabaseName();
        long port = 0;
        if(databaseName.contains("-")) {
            String[] split = databaseName.split("-");
            Integer number = Integer.parseInt(split[2]);
            port = 20000 + number;
            globalState.dbmsSpecificOptions.port = port;
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions(), port);
        }
        else {
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
        }
    }

    @Override
    public String getDBMSName() {
        return "thinker";
    }

    @Override
    public LoggableFactory getLoggableFactory() {
        return new CypherLoggableFactory();
    }

    @Override
    protected void checkViewsAreValid(TinkerGlobalState globalState) {

    }

    @Override
    public void generateDatabase(TinkerGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = TinkerGraphGenerator.createGraph(globalState);
        for(CypherQueryAdapter query : queries){
            globalState.executeStatement(query);
        }
    }
}
