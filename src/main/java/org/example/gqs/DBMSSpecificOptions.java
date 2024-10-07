package org.example.gqs;

import org.example.gqs.cypher.dsl.IQueryGenerator;

import java.util.List;

public interface DBMSSpecificOptions<F extends OracleFactory<? extends GlobalState<?, ?, ?>>> {

    List<F> getTestOracleFactory();

    IQueryGenerator getQueryGenerator();

}
