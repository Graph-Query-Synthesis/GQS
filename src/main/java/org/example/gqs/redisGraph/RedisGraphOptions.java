package org.example.gqs.redisGraph;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.gson.JsonObject;
import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.OracleFactory;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.redisGraph.oracle.RedisGraphAlwaysTrueOracle;
import org.example.gqs.common.oracle.TestOracle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Parameters(separators = "=", commandDescription = "RedisGraph (default port: " + RedisGraphOptions.DEFAULT_PORT
        + ", default host: " + RedisGraphOptions.DEFAULT_HOST)
public class RedisGraphOptions implements DBMSSpecificOptions<RedisGraphOptions.RedisGraphOracleFactory> {

    public static final String DEFAULT_HOST = "localhost";
    public static final long DEFAULT_PORT = 6379;

    public static RedisGraphOptions parseOptionFromFile(JsonObject jsonObject){
        RedisGraphOptions options = new RedisGraphOptions();
        if(jsonObject.has("host")){
            options.host = jsonObject.get("host").getAsString();
        }
        if(jsonObject.has("port")){
            options.port = jsonObject.get("port").getAsInt();
        }
        if(jsonObject.has("username")){
            options.username = jsonObject.get("username").getAsString();
        }
        if(jsonObject.has("password")){
            options.password = jsonObject.get("password").getAsString();
        }
        if(jsonObject.has("restart-command")){
            options.restartCommand = jsonObject.get("restart-command").getAsString();
        }
        return options;
    }

    @Parameter(names = "--restart-command")
    public String restartCommand = "";

    @Parameter(names = "--oracle")
    public List<RedisGraphOracleFactory> oracles = Arrays.asList(RedisGraphOracleFactory.ALWAYS_TRUE);

    @Parameter(names = "--host")
    public String host = DEFAULT_HOST;

    @Parameter(names = "--port")
    public long port = DEFAULT_PORT;

    @Parameter(names = "--username")
    public String username = "neo4j";

    @Parameter(names = "--password")
    public String password = "sqlancer";

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public long getPort() {
        return port;
    }

    @Override
    public List<RedisGraphOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    @Override
    public IQueryGenerator getQueryGenerator() {
        return null;
    }

    public enum RedisGraphOracleFactory implements OracleFactory<RedisGraphGlobalState> {

        ALWAYS_TRUE {

            @Override
            public TestOracle create(RedisGraphGlobalState globalState) throws SQLException {
                return new RedisGraphAlwaysTrueOracle(globalState);
            }
        }
    }
}
