package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.IWith;

public interface IWithAnalyzer extends IWith, IClauseAnalyzer {
    @Override
    IWith getSource();
}
