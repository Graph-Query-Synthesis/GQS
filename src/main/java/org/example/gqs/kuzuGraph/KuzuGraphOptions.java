package org.example.gqs.kuzuGraph;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.gson.JsonObject;
import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.OracleFactory;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.kuzuGraph.oracle.KuzuGraphAlwaysTrueOracle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Parameters(separators = "=", commandDescription = "MemGraph (default port: " + KuzuGraphOptions.DEFAULT_PORT
        + ", default host: " + KuzuGraphOptions.DEFAULT_HOST)
public class KuzuGraphOptions implements DBMSSpecificOptions<KuzuGraphOptions.MemGraphOracleFactory> {

    public static final String DEFAULT_HOST = "localhost";
    public static final long DEFAULT_PORT = 10015;

    @Parameter(names = "--oracle")
    public List<MemGraphOracleFactory> oracles = Arrays.asList(MemGraphOracleFactory.ALWAYS_TRUE);

    @Parameter(names = "--host")
    public String host = DEFAULT_HOST;

    @Parameter(names = "--port")
    public long port = DEFAULT_PORT;

    @Parameter(names = "--username")
    public String username = "";

    @Parameter(names = "--password")
    public String password = "";

    @Parameter(names = "--restart-command")
    public String restartCommand = "";

    public static KuzuGraphOptions parseOptionFromFile(JsonObject jsonObject){
        KuzuGraphOptions options = new KuzuGraphOptions();
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
    public List<MemGraphOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    @Override
    public IQueryGenerator getQueryGenerator() {
        return null;
    }

    public enum MemGraphOracleFactory implements OracleFactory<KuzuGraphGlobalState> {

        ALWAYS_TRUE {

            @Override
            public TestOracle create(KuzuGraphGlobalState globalState) throws SQLException {
                return new KuzuGraphAlwaysTrueOracle(globalState);
            }
        }
    }
}
