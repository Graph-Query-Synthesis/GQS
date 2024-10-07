package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.ICreate;

public interface ICreateAnalyzer extends ICreate, IClauseAnalyzer {
    @Override
    ICreate getSource();
}
