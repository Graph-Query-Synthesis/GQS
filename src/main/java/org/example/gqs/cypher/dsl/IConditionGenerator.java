package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;

import java.util.List;
import java.util.Map;

public interface IConditionGenerator {
    void fillMatchCondtion(IMatchAnalyzer matchClause);
    void fillMatchCondtion(IMatchAnalyzer matchClause, List<Map<String, Object>> namespace);
    void fillWithCondition(IWithAnalyzer withClause);
}
