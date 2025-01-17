package org.example.gqs.redisGraph;

import com.falkordb.FalkorDB;
import com.falkordb.ResultSet;
import org.example.gqs.Main;
import org.example.gqs.MainOptions;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.exceptions.MustRestartDatabaseException;
import com.falkordb.Graph;


import java.util.Arrays;
import java.util.List;

public class RedisGraphConnection extends CypherConnection {

    private Graph graph;
    private String graphName;

    private RedisGraphOptions options;

    public RedisGraphConnection(RedisGraphOptions options, Graph graph, String graphName){
        this.graph = graph;
        this.graphName = graphName;
        this.options = options;
    }


    @Override
    public String getDatabaseVersion() {
        return "falkordb";
    }

    @Override
    public void close() throws Exception {
        graph.close();
    }

    @Override
    public void executeStatement(String arg) throws Exception{
        try {
            graph.query(arg);
        } catch (redis.clients.jedis.exceptions.JedisConnectionException e) {
            System.out.println("got jedis crashed");
            e.printStackTrace();
            throw new MustRestartDatabaseException(e);
        }

        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        try{
            ResultSet resultSet = graph.query(arg, 60*1000);
            return Arrays.asList(new GQSResultSet(resultSet));
        } catch (redis.clients.jedis.exceptions.JedisConnectionException e){
            try{
                graph.query("MATCH (n) RETURN n LIMIT 1");
            }
            catch(Exception f) {
                System.out.println("got jedis crashed");
                f.printStackTrace();
                String r = "[{\"Crash\":\"CrashQuery\"}]";
                return Arrays.asList(new GQSResultSet(r));
            }
        }
        String r = "[{\"a1\":\"ProblematicQuery\"}]";
        return Arrays.asList(new GQSResultSet(r));
    }

    public void reproduce(List<String> queries) {
        try {
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long port = options.getPort();
        Main.rebootDatabase(port - 20000, MainOptions.stopCommandFalkorDB, MainOptions.deleteCommandFalkorDB, MainOptions.deleteFileFalkorDB, MainOptions.startCommandFalkorDB, MainOptions.mode + "database");
        try {

            graph = FalkorDB.driver("127.0.0.1", (int) port).graph("social");
            executeStatement("MATCH (n) DETACH DELETE n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String query : queries) {
            try {
                executeStatement(query);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}