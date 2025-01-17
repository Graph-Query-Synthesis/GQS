package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.analyzer.IReturnAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;

import java.util.List;
import java.util.Map;

public interface IAliasGenerator {
    void fillReturnAlias(IReturnAnalyzer returnClause);
    void fillReturnAlias(IReturnAnalyzer returnClause, List<Map<String, Object>> namespace);
    void fillWithAlias(IWithAnalyzer withClause);
}
