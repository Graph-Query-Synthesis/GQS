package org.example.gqs.neo4j;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonObject;
import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.OracleFactory;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.neo4j.Neo4jOptions.Neo4jOracleFactory;
import org.example.gqs.neo4j.oracle.Neo4jNoRecOracle;
import org.example.gqs.neo4j.oracle.Neo4jSmithCrashOracle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class Neo4jOptions implements DBMSSpecificOptions<Neo4jOracleFactory> {

    public static final String DEFAULT_HOST = "localhost";
    public static final long DEFAULT_PORT = 7687;

    public static Neo4jOptions parseOptionFromFile(JsonObject jsonObject) {
        Neo4jOptions options = new Neo4jOptions();
        if (jsonObject.has("host")) {
            options.host = jsonObject.get("host").getAsString();
        }
        if (jsonObject.has("port")) {
            options.port = jsonObject.get("port").getAsInt();
        }
        if (jsonObject.has("username")) {
            options.username = jsonObject.get("username").getAsString();
        }
        if (jsonObject.has("password")) {
            options.password = jsonObject.get("password").getAsString();
        }
        if (jsonObject.has("use_jdbc")) {
            options.useJDBC = jsonObject.get("use_jdbc").getAsBoolean();
        }
        if (jsonObject.has("proxy_port")) {
            options.proxyPort = jsonObject.get("proxy_port").getAsInt();
        }
        return options;
    }

    @Parameter(names = "--oracle")
    public List<Neo4jOracleFactory> oracles = Arrays.asList(Neo4jOracleFactory.RANDOM_CRASH, Neo4jOracleFactory.NO_REC);

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

    @Parameter(names = "--host")
    public String host = DEFAULT_HOST;

    @Parameter(names = "--port")
    public long port = DEFAULT_PORT;

    @Parameter(names = "--username")
    public String username = "neo4j";

    @Parameter(names = "--password")
    public String password = "sqlancer";

    @Parameter(names = "--use_jdbc")
    public boolean useJDBC = false;

    @Parameter(names = "--proxy_port")
    public long proxyPort = 0;


    @Override
    public List<Neo4jOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    @Override
    public IQueryGenerator getQueryGenerator() {
        return null;
    }

    public enum Neo4jOracleFactory implements OracleFactory<Neo4jGlobalState> {

        RANDOM_CRASH {
            @Override
            public TestOracle create(Neo4jGlobalState globalState) throws SQLException {
                return new Neo4jSmithCrashOracle(globalState);
            }
        },
        NO_REC {
            @Override
            public TestOracle create(Neo4jGlobalState globalState) throws SQLException {
                return new Neo4jNoRecOracle(globalState);
            }
        }
    }
}
