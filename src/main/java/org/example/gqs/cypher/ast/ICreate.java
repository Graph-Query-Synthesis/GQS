package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.ast.analyzer.ICreateAnalyzer;

public interface ICreate extends ICypherClause{
    IPattern getPattern();
    void setPattern(IPattern pattern);

    @Override
    ICreateAnalyzer toAnalyzer();
}
