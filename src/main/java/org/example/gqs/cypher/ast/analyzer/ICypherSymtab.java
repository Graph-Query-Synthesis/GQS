package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.IPattern;
import org.example.gqs.cypher.ast.IRet;

import java.util.List;

public interface ICypherSymtab {
    List<IPattern> getPatterns();
    void setPatterns(List<IPattern> patterns);
    List<IRet> getAliasDefinitions();
    void setAliasDefinition(List<IRet> aliasDefinitions);

    List<IAliasAnalyzer> getLocalAliasDefs();
    List<IAliasAnalyzer> getAvailableAliasDefs();
    List<INodeAnalyzer> getLocalNodePatterns();
    List<IRelationAnalyzer> getLocalRelationPatterns();
    List<INodeAnalyzer> getAvailableNodePatterns();
    List<IRelationAnalyzer> getAvailableRelationPatterns();
    List<IIdentifierAnalyzer> getLocalIdentifiers();
    List<IIdentifierAnalyzer> getAvailableIdentifiers();

}
