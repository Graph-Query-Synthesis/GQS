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

public class StringCatExpression extends CypherExpression{
    private IExpression left, right;
    public String result;

    public IExpression getLeft() {
        return left;
    }

    public void setLeft(IExpression left) {
        this.left = left;
    }

    public IExpression getRight() {
        return right;
    }

    public void setRight(IExpression right) {
        this.right = right;
    }
    public String getValueWithNoBase(IExpression e)
    {
        if(e instanceof ConstExpression)
        {
            return ((ConstExpression) e).getValue().toString();
        }
        else if(e instanceof StringCatExpression)
        {
            return ((StringCatExpression) e).getResult();
        }
        else if(e instanceof GetPropertyExpression)
        {
            return ((GetPropertyExpression) e).getValue().toString();
        }
        else
        {
            try{
                throw new Exception("Unknown Expression Type in String concat "+ left.toString()+right.toString());
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        throw new RuntimeException("Stringcat getValuewithNoBase error");
    }
    public String getResult()
    {
        if(result != null)
            return result;
        else
        {
            return result = getValueWithNoBase(left)+getValueWithNoBase(right);
        }
    }

    public StringCatExpression(IExpression left, IExpression right){
        this.left = left;
        this.right = right;
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(CypherType.STRING);
    }

    @Override
    public IExpression getCopy() {
        return new StringCatExpression(left, right);
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("(");
        left.toTextRepresentation(sb);
        sb.append("+");
        right.toTextRepresentation(sb);
        sb.append(")");
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof StringCatExpression)){
            return false;
        }
        return left.equals(((StringCatExpression) o).left) && right.equals(((StringCatExpression) o).right);
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        if(originalExpression == left){
            this.left = newExpression;
            newExpression.setParentExpression(this);
            return;
        }
        if(originalExpression == right){
            this.right = newExpression;
            newExpression.setParentExpression(this);
            return;
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        String leftVal = (String) left.getValue(varToProperties);
        String rightVal = (String) right.getValue(varToProperties);
        return leftVal + rightVal;
    }

    @Override
    public Object getValue() {
        String leftVal = (String) ((CypherExpression)left).getValue();
        String rightVal = (String) ((CypherExpression)right).getValue();
        return leftVal + rightVal;
    }

    @Override
    public Set<IIdentifier> reliedContent() {
        Set<IIdentifier> result = ((CypherExpression)left).reliedContent();
        result.addAll(((CypherExpression)right).reliedContent());
        return result;
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        CypherExpression leftExpression = (CypherExpression)left;
        CypherExpression rightExpression = (CypherExpression)right;
        Set<IIdentifier> leftReliedContent = leftExpression.reliedContent();
        Set<IIdentifier> rightReliedContent = rightExpression.reliedContent();
        boolean remove = false;
        for(IIdentifier identifier : toRemove){
            if(leftReliedContent.contains(identifier)){
                remove = true;
                break;
            }
            if(rightReliedContent.contains(identifier)){
                remove = true;
                break;
            }
        }
        if(remove)
        {
            this.left = new ConstExpression(getValueWithNoBase(left));
            this.right = new ConstExpression(getValueWithNoBase(right));
        }
    }
}
