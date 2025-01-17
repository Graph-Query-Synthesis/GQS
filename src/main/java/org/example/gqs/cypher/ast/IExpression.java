package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;

import java.util.List;
import java.util.Map;

public interface IExpression extends ITextRepresentation, ICopyable{
    IExpression getParentExpression();
    void setParentExpression(IExpression parentExpression);
    ICypherClause getExpressionRootClause();
    void setParentClause(ICypherClause parentClause);

    ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers);

    @Override
    IExpression getCopy();

    void replaceChild(IExpression originalExpression, IExpression newExpression);

    Object getValue(Map<String, Object> varToProperties);
    boolean equals(Object obj);

}
