package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.IMatch;

public interface IMatchAnalyzer extends IMatch, IClauseAnalyzer {
    @Override
    IMatch getSource();
}
