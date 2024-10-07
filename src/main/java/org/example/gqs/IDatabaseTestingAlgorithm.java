package org.example.gqs;

import org.example.gqs.common.schema.AbstractSchema;

public interface IDatabaseTestingAlgorithm  <G extends GlobalState<O, ? extends AbstractSchema<G, ?>, C>, O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends GDSmithDBConnection>{
    void generateAndTestDatabase(G globalState) throws Exception;
}
