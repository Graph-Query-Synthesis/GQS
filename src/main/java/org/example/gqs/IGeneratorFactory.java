package org.example.gqs;

import org.example.gqs.cypher.dsl.IQueryGenerator;

public interface IGeneratorFactory <G extends IQueryGenerator>{
    G create();
}
