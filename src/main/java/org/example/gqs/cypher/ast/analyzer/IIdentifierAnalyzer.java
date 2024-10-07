package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;

public interface IIdentifierAnalyzer extends IIdentifier {
    IIdentifierAnalyzer getFormerDef();

    IIdentifier getSource();

    IExpression getSourceRefExpression();

    IContextInfo getContextInfo();
}
