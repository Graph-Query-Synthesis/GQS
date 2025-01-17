package org.example.gqs;

import org.example.gqs.common.oracle.TestOracle;

public interface OracleFactory<G extends GlobalState<?, ?, ?>> {

    TestOracle create(G globalState) throws Exception;

    default boolean requiresAllTablesToContainRows() {
        return false;
    }

}
