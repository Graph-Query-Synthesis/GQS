package org.example.gqs.neo4j;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Neo4jProxyConnection extends CypherConnection {


    private Neo4jOptions options;

    public Neo4jProxyConnection(Neo4jOptions options){
        this.options = options;
    }


    @Override
    public String getDatabaseVersion() throws Exception {
        return "neo4j";
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void executeStatement(String arg) throws Exception{
        executeStatementAndGet(arg);
    }

    private static class Request{
        String username;
        String password;
        String host;
        long port;
        String query;
    }

    private static class Response{
        public GQSResultSet GQSResultSet;
        public String exceptionMsg;
    }


    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        try (Socket socket = new Socket("localhost", (int) options.proxyPort)) {
            OutputStream outputStream = socket.getOutputStream();
            Request request = new Request();
            request.username = options.getUsername();
            request.password = options.getPassword();
            request.port = options.getPort();
            request.host = options.getHost();
            request.query = arg;
            Gson gson = new GsonBuilder().serializeNulls().create();
            String requestString = gson.toJson(request);
            outputStream.write(requestString.getBytes());
            outputStream.flush();
            socket.shutdownOutput();

            InputStream inputStream = socket.getInputStream();
            byte[] responseBytes = inputStream.readAllBytes();
            Response response = gson.fromJson(new String(responseBytes), Response.class);
            if (!response.exceptionMsg.equals("")) {
                throw new RuntimeException(response.exceptionMsg);
            }
            response.GQSResultSet.resolveFloat();
            return new ArrayList<>(Arrays.asList(response.GQSResultSet));
        }
    }
}
