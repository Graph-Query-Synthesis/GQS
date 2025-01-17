package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.analyzer.IUnwindAnalyzer;

public interface IListGenerator {
    void fillUnwindList(IUnwindAnalyzer unwindAnalyzer);
}
