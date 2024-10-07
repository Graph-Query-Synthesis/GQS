package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;

public interface IPatternGenerator {
    void fillMatchPattern(IMatchAnalyzer match);
}
