package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.ast.analyzer.IMergeAnalyzer;

public interface IMerge extends ICypherClause{
    IPattern getPattern();
    void setPattern(IPattern pattern);

    @Override
    IMergeAnalyzer toAnalyzer();
}
