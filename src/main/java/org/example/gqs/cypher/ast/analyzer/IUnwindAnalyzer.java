package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.IUnwind;

public interface IUnwindAnalyzer extends IUnwind, IClauseAnalyzer {
    @Override
    IUnwind getSource();
}
