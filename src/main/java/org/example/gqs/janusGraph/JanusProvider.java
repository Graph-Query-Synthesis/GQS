package org.example.gqs.janusGraph;

import com.google.gson.JsonObject;
import org.example.gqs.AbstractAction;
import org.example.gqs.MainOptions;
import org.example.gqs.common.log.LoggableFactory;
import org.example.gqs.cypher.*;
import org.example.gqs.janusGraph.gen.JanusNodeGenerator;
import org.example.gqs.janusGraph.schema.JanusSchema;
import org.example.gqs.janusGraph.gen.JanusGraphGenerator;
import org.apache.tinkerpop.gremlin.driver.Cluster;

import java.util.List;

public class JanusProvider extends CypherProviderAdapter<JanusGlobalState, JanusSchema, JanusOptions> {
    public JanusProvider() {
        super(JanusGlobalState.class, JanusOptions.class);
    }

    @Override
    public JanusOptions generateOptionsFromConfig(JsonObject config) {
        return JanusOptions.parseOptionFromFile(config);
    }

    @Override
    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, JanusOptions specificOptions) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = specificOptions.getPort();
        if (host == null) {
            host = JanusOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = JanusOptions.DEFAULT_PORT;
        }

        String url = String.format("bolt://%s:%d", host, port);
        Cluster cluster;
        if(specificOptions.configFile != null){
            cluster = Cluster.open(specificOptions.configFile);
        }
        else{
            cluster = Cluster.open();
        }
        JanusConnection con = new JanusConnection(cluster);
        con.executeStatement("MATCH (n) DETACH DELETE n");
        return con;
    }

    enum Action implements AbstractAction<JanusGlobalState> {
        CREATE(JanusNodeGenerator::createNode);

        private final CypherQueryProvider<JanusGlobalState> cypherQueryProvider;

        Action(CypherQueryProvider<JanusGlobalState> cypherQueryProvider) {
            this.cypherQueryProvider = cypherQueryProvider;
        }

        @Override
        public CypherQueryAdapter getQuery(JanusGlobalState globalState) throws Exception {
            return cypherQueryProvider.getQuery(globalState);
        }
    }

    @Override
    public CypherConnection createDatabase(JanusGlobalState globalState) throws Exception {
       return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
    }

    @Override
    public String getDBMSName() {
        return "janusgraph";
    }

    @Override
    public LoggableFactory getLoggableFactory() {
        return new CypherLoggableFactory();
    }

    @Override
    protected void checkViewsAreValid(JanusGlobalState globalState) {

    }

    @Override
    public void generateDatabase(JanusGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = JanusGraphGenerator.createGraph(globalState);
        for(CypherQueryAdapter query : queries){
            globalState.executeStatement(query);
        }
    }
}
