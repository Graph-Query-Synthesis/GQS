package org.example.gqs;

import org.example.gqs.common.log.LoggableFactory;

public interface DatabaseProvider<G extends GlobalState<O, ?, C>, O extends DBMSSpecificOptions<?>, C extends GDSmithDBConnection> {

    Class<G> getGlobalStateClass();

    Class<O> getOptionClass();

    void generateAndTestDatabase(G globalState) throws Exception;

    C createDatabase(G globalState) throws Exception;

    String getDBMSName();

    LoggableFactory getLoggableFactory();

    StateToReproduce getStateToReproduce(String databaseName);

}
