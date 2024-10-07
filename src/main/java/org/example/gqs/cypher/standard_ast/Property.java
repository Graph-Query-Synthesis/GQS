package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IProperty;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;

public class Property implements IProperty {
    private String key;
    private IExpression value;
    private CypherType type;

    public Property(String key, CypherType type, IExpression value){
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public Property(String key, IExpression value){
        this.key = key;
        this.value = value;
        if(value instanceof ConstExpression)
        {
            this.type = (CypherType) decideType(((ConstExpression) value).getValue());
        }
        else
        {
            this.type = CypherType.UNKNOWN;
        }
    }

    public static ICypherType decideType (Object input)
    {
        if(input instanceof String){
            return CypherType.STRING;
        }
        if(input instanceof Integer) {
            throw new RuntimeException("using integer instead of long!");
            
        }
        if(input instanceof Long) {
            return CypherType.NUMBER;
        }
        if(input instanceof Boolean) {
            return CypherType.BOOLEAN;
        }
        return CypherType.UNKNOWN;
    }

    @Override
    public ICypherType getType() {
        return type;
    }

    @Override
    public IExpression getVal() {
        return value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public IProperty getCopy() {
        if(value != null){
            return new Property(key, type, value.getCopy());
        }
        return new Property(key, type, null);

    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append(key).append(":");
        value.toTextRepresentation(sb);
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Property)){
            return false;
        }
        return ((Property) o).key.equals(key);
    }

    @Override
    public int hashCode(){
        return key.hashCode();
    }

}
