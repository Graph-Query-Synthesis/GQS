package org.example.gqs.composite;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.DatabaseProvider;
import org.example.gqs.Main;
import org.example.gqs.MainOptions;
import org.example.gqs.common.log.LoggableFactory;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherLoggableFactory;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CompositePerformanceProvider extends CypherProviderAdapter<CompositeGlobalState, CompositeSchema, CompositeOptions> {
    public CompositePerformanceProvider() {
        super(CompositeGlobalState.class, CompositeOptions.class);
    }

    @Override
    public CypherConnection createDatabase(CompositeGlobalState globalState) throws Exception {
        return createDatabaseWithOptions(globalState.getOptions(), globalState.getDbmsSpecificOptions());
    }

    @Override
    public void generateAndTestDatabase(CompositeGlobalState globalState) throws Exception {
        try {
            generateDatabase(globalState);
            TestOracle oracle = getTestOracle(globalState);
            oracle.check();
        } finally {
            globalState.getConnection().close();
        }
    }

    @Override
    public String getDBMSName() {
        return "org/example/gqs/performance";
    }

    @Override
    public LoggableFactory getLoggableFactory() {
        return new CypherLoggableFactory();
    }

    @Override
    protected void checkViewsAreValid(CompositeGlobalState globalState) {

    }

    @Override
    public void generateDatabase(CompositeGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = globalState.getDbmsSpecificOptions().graphGenerator.create(globalState)
                .createGraph(globalState);
        for(CypherQueryAdapter query : queries){
            globalState.executeStatement(query);
        }
    }

    @Override
    public CompositeOptions generateOptionsFromConfig(JsonObject config) {
        return null;
    }

    @Override
    public CypherConnection createDatabaseWithOptions(MainOptions mainOptions, CompositeOptions specificOptions) throws Exception {
        List<CypherConnection> connections = new ArrayList<>();
        Gson gson = new Gson();
        try {
            FileReader fileReader = new FileReader(specificOptions.getConfigPath());
            JsonObject jsonObject = gson.fromJson(fileReader, JsonObject.class);
            Set<String> databaseNamesWithVersion = jsonObject.keySet();
            for(DatabaseProvider provider: Main.getDBMSProviders()){
                String databaseName = provider.getDBMSName().toLowerCase();
                MainOptions options = mainOptions;
                for(String nameWithVersion : databaseNamesWithVersion){
                    if(nameWithVersion.contains(provider.getDBMSName().toLowerCase())){
                        DBMSSpecificOptions command = ((CypherProviderAdapter)provider)
                                .generateOptionsFromConfig(jsonObject.getAsJsonObject(nameWithVersion));
                        connections.add(((CypherProviderAdapter)provider).createDatabaseWithOptions(options, command));
                    }
                }

            }
            System.out.println("success");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CompositeConnection compositeConnection = new CompositeConnection(connections, mainOptions);
        return compositeConnection;
    }
}
