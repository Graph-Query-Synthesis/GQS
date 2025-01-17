package org.example.gqs;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.dsl.IGraphGenerator;

public interface IGraphGeneratorFactory <G extends CypherGlobalState<?,?>, GG extends IGraphGenerator<G>>{
    GG create(G globalState);
}
