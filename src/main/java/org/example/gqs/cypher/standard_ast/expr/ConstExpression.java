package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.ICopyable;
import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.CypherTypeDescriptor;

import java.util.*;

public class ConstExpression extends CypherExpression {
    Object value;
    ICypherType type;

    public ConstExpression(Object value){
        this.value = value;
        if(value instanceof Integer || value instanceof Float || value instanceof Long || value instanceof Double){
            type = CypherType.NUMBER;
            if(value instanceof Integer) {
                this.value = Long.parseLong(this.value.toString());
            }
            if(value instanceof Double)
            {
                this.value = Long.parseLong(this.value.toString());
            }

        }
        else if(value instanceof String){
            type = CypherType.STRING;
        }
        else if(value instanceof Boolean){
            type = CypherType.BOOLEAN;
        }
        else if (value instanceof List)
        {
            type = CypherType.LIST;
        }
        else {
            type = CypherType.UNKNOWN;
        }
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(type);
    }

    public ICypherType getType(){
        return type;
    }

    public Object getValue(){
        return value;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        switch ((CypherType)type){
            case NUMBER: sb.append("" + value); break;
            case STRING: sb.append("\'" + value + "\'"); break;
            case BOOLEAN: sb.append("" + value); break;
            case LIST: sb.append("[" + value + "]"); break;
            case UNKNOWN: sb.append("null");
                break;
        }
    }

    @Override
    public IExpression getCopy() {
        if (value instanceof ICopyable) {
            return new ConstExpression(((ICopyable) value).getCopy());
        }
        return new ConstExpression(value);
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        return value;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof ConstExpression)){
            return false;
        }
        if(type != ((ConstExpression) o).type){
            return false;
        }
        if(type == CypherType.UNKNOWN){
            return false;
        }
        return value.equals(((ConstExpression) o).value);
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public Set<IIdentifier> reliedContent() {
        return new HashSet<>();
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
    }
}
