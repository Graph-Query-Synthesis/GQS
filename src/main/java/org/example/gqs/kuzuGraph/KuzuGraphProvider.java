package org.example.gqs.kuzuGraph;

import com.google.gson.JsonObject;
import com.kuzudb.KuzuConnection;
import com.kuzudb.KuzuDatabase;
import org.apache.commons.io.FileUtils;
import org.example.gqs.MainOptions;
import org.example.gqs.common.log.LoggableFactory;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherLoggableFactory;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.kuzuGraph.gen.KuzuGraphGraphGenerator;

import java.io.File;
import java.util.List;

public class KuzuGraphProvider extends CypherProviderAdapter<KuzuGraphGlobalState, KuzuGraphSchema, KuzuGraphOptions> {
    public KuzuGraphProvider() {
        super(KuzuGraphGlobalState.class, KuzuGraphOptions.class);
    }
    @Override
    public CypherConnection createDatabase(KuzuGraphGlobalState globalState) throws Exception {
        String databaseName = globalState.getDatabaseName();
        long port = 0;
        if(databaseName.contains("-"))
        {
            String[] split = databaseName.split("-");
            Integer number = Integer.parseInt(split[2]);
            port = 20000 + number;
            globalState.dbmsSpecificOptions.port = port;
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions(), port);
        }
        else
        {
            return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
        }

    }

    @Override
    public String getDBMSName() {
        return "kuzu";
    }

    @Override
    public LoggableFactory getLoggableFactory() {
        return new CypherLoggableFactory();
    }

    @Override
    protected void checkViewsAreValid(KuzuGraphGlobalState globalState) {

    }

    @Override
    public void generateDatabase(KuzuGraphGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = KuzuGraphGraphGenerator.createGraph(globalState);
        for(CypherQueryAdapter query : queries){
            globalState.executeStatement(query);
        }
    }

    @Override
    public KuzuGraphOptions generateOptionsFromConfig(JsonObject config) {
        return KuzuGraphOptions.parseOptionFromFile(config);
    }

    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, KuzuGraphOptions specificOptions, long databaseNo) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = databaseNo;
        if (host == null) {
            host = KuzuGraphOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = KuzuGraphOptions.DEFAULT_PORT;
        }

        String databasePath = "/home/auroraeth/kuzugraph/"+Long.toString(port);
        CypherConnection con;
        FileUtils.deleteDirectory(new File(databasePath));
        KuzuDatabase db = new KuzuDatabase(databasePath);
        con = new KuzuGraphConnection(db, new KuzuConnection(db), specificOptions);
        con.executeStatement("MATCH (n) RETURN n");
        return con;
    }

    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, KuzuGraphOptions specificOptions) throws Exception {
        String username = specificOptions.getUsername();
        String password = specificOptions.getPassword();
        String host = specificOptions.getHost();
        long port = specificOptions.getPort();
        if (host == null) {
            host = KuzuGraphOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = KuzuGraphOptions.DEFAULT_PORT;
        }

        String databasePath = "/home/auroraeth/kuzugraph/"+Long.toString(port);
        CypherConnection con;
        FileUtils.deleteDirectory(new File(databasePath));
        KuzuDatabase db  = new KuzuDatabase(databasePath);
        con = new KuzuGraphConnection(db, new KuzuConnection(db), specificOptions);
        con.executeStatement("MATCH (n) RETURN n");
        return con;
    }

}
