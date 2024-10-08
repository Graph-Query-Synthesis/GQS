package org.example.gqs.janusGraph;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.translation.TranslationFacade;

import java.util.Arrays;
import java.util.List;

public class JanusConnection extends CypherConnection {


    private Cluster cluster;

    public JanusConnection(Cluster cluster){
        this.cluster = cluster;
    }

    public JanusConnection(){
    }


    @Override
    public String getDatabaseVersion() throws Exception {
        return "";
    }

    @Override
    public void close() throws Exception {
        Client gremlinClient = cluster.connect();
        gremlinClient.submit("MATCH (n) DETACH DELETE n");
        gremlinClient.close();
        cluster.close();
    }

    @Override
    public void executeStatement(String arg) throws Exception{
        String cypher = arg;
        Client gremlinClient = cluster.connect();
        CypherGremlinClient translatingGremlinClient = CypherGremlinClient.translating(gremlinClient);
        String gremlin = (new TranslationFacade()).toGremlinGroovy(cypher);
        System.out.println(gremlin);
        translatingGremlinClient.submit(cypher).all();
    }

    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        String cypher = arg;
        Client gremlinClient = cluster.connect();
        CypherGremlinClient translatingGremlinClient = CypherGremlinClient.translating(gremlinClient);
        String gremlin = (new TranslationFacade()).toGremlinGroovy(cypher);
        System.out.println(gremlin);
        return Arrays.asList(new GQSResultSet(translatingGremlinClient.submit(cypher).all()));
    }
}
