package org.example.gqs.tinkerGraph;

import org.apache.tinkerpop.gremlin.driver.Result;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.opencypher.gremlin.translation.TranslationFacade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TinkerConnection extends CypherConnection {


    private Cluster cluster;
    private Client client;

    public TinkerConnection(Cluster cluster){
        this.cluster = cluster;
        this.client = cluster.connect();
    }

    public TinkerConnection(){
    }


    @Override
    public String getDatabaseVersion() throws Exception {
        return "ThinkerGraph";
    }

    @Override
    public void close() throws Exception {
        client.close();
        cluster.close();
    }

    @Override
    public void executeStatement(String arg) throws Exception {
        String cypher = arg;
        TranslationFacade cfog = new TranslationFacade();
        String gremlin = cfog.toGremlinGroovy(cypher);
        System.out.println(gremlin);
        Object b = this.client.submit(gremlin).all().get();
    }

    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception {
        String cypher = arg;
        String gremlin = (new TranslationFacade()).toGremlinGroovy(cypher);
        System.out.println(gremlin);
        List<Result> b = this.client.submit(gremlin).all().get();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Result r : b) {
            Map<String, Object> result = (Map<String, Object>) r.getObject();
            results.add(result);
        }
        return Arrays.asList(new GQSResultSet(results));
    }
}
