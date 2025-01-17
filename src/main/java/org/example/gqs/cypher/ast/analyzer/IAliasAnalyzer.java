package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IAlias;
import org.example.gqs.cypher.ast.IExpression;


public interface IAliasAnalyzer extends IAlias, IIdentifierAnalyzer {
    @Override
    IAliasAnalyzer getFormerDef();
    void setFormerDef(IAliasAnalyzer formerDef);
    IExpression getAliasDefExpression();

    @Override
    IAlias getSource();

    ICypherTypeDescriptor analyzeType(ICypherSchema cypherSchema);
}
