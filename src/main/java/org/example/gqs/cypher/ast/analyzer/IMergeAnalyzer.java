package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.IMerge;

public interface IMergeAnalyzer extends IMerge, IClauseAnalyzer {
    @Override
    IMerge getSource();
}
