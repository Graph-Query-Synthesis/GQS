package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.IReturn;

public interface IReturnAnalyzer extends IReturn, IClauseAnalyzer {
    @Override
    IReturn getSource();
}
