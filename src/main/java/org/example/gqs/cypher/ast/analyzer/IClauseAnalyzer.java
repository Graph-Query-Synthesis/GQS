package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.ICypherClause;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.IPattern;

import java.util.List;

public interface IClauseAnalyzer extends ICypherClause {

    List<IAliasAnalyzer> getLocalAliases();

    List<INodeAnalyzer> getLocalNodeIdentifiers();

    List<IRelationAnalyzer> getLocalRelationIdentifiers();

    List<IIdentifierAnalyzer> getLocalIdentifiers();

    List<IAliasAnalyzer> getAvailableAliases();

    List<INodeAnalyzer> getAvailableNodeIdentifiers();

    List<IRelationAnalyzer> getAvailableRelationIdentifiers();

    List<IIdentifierAnalyzer> getAvailableIdentifiers();

    List<IAliasAnalyzer> getExtendableAliases();

    List<INodeAnalyzer> getExtendableNodeIdentifiers();

    List<IRelationAnalyzer> getExtendableRelationIdentifiers();

    List<IIdentifierAnalyzer> getExtendableIdentifiers();

    IIdentifierAnalyzer getIdentifierAnalyzer(String name);

    IIdentifierAnalyzer getIdentifierAnalyzer(IIdentifier identifier);

    List<IPattern> getLocalPatternContainsIdentifier(IIdentifier identifier);


    ICypherClause getSource();
}
