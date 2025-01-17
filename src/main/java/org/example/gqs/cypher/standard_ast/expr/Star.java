package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.CypherTypeDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Star extends CypherExpression{
    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(CypherType.UNKNOWN);
    }

    @Override
    public IExpression getCopy() {
        return new Star();
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        return null;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("*");
    }

    @Override
    public Object getValue() {
        throw new RuntimeException("getValue not implemented in Star.java");
    }

    @Override
    public Set<IIdentifier> reliedContent() {
        throw new RuntimeException("not implemented in Star.java");
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        throw new RuntimeException("not implemented in Star.java");
    }
}
