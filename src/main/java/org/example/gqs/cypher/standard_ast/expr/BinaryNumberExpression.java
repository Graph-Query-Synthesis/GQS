package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
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

public class BinaryNumberExpression extends CypherExpression{
    private IExpression left, right;
    private BinaryNumberOperation op;
    
    public BinaryNumberExpression(IExpression left, IExpression right, BinaryNumberOperation op){
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public static IExpression randomBinaryNumber(IExpression left, IExpression right){
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 100);
        if(operationNum < 25) {
        }
        if(operationNum < 50){
            return new BinaryNumberExpression(left, right, BinaryNumberOperation.MULTIPLY);
        }
        if(operationNum < 75){
            return new BinaryNumberExpression(left, right, BinaryNumberOperation.MINUS);
        }
        return new BinaryNumberExpression(left, right, BinaryNumberOperation.ADD);
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(CypherType.NUMBER);
    }

    @Override
    public IExpression getCopy() {
        return new BinaryNumberExpression(left.getCopy(), right.getCopy(), op);
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("(");
        if(op == BinaryNumberOperation.DIVISION){
            if(MainOptions.mode == "kuzu")
            {
                sb.append("CAST(");
            }
            else
                sb.append("toInteger(");
        }
        left.toTextRepresentation(sb);
        sb.append(op.getTextRepresentation());
        right.toTextRepresentation(sb);
        if(op == BinaryNumberOperation.DIVISION){
            if(MainOptions.mode == "kuzu")
                sb.append(", \"INT64\")");
            else
                sb.append(")");
        }
        sb.append(")");
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
        boolean removeLeft = false;
        boolean removeRight = false;
        for(IIdentifier identifier : toRemove){
            if(leftReliedContent.contains(identifier)){
                removeLeft = true;
                break;
            }
            if(rightReliedContent.contains(identifier)){
                removeRight = true;
                break;
            }
        }
        if(removeLeft)
        {
            if(left instanceof IdentifierExpression)
            {
                left = new ConstExpression(((IdentifierExpression) left).getValue());
            }
            else if (left instanceof GetPropertyExpression)
            {
                left = new ConstExpression(((GetPropertyExpression) left).getValue());
            }
            else{
                ((CypherExpression)left).removeElement(toRemove);
            }
        }
        if(removeRight)
        {
            if(right instanceof IdentifierExpression && toRemove.contains(((IdentifierExpression) right).getIdentifier()))
            {
                right = new ConstExpression(((IdentifierExpression) right).getValue());
            }
            else if (right instanceof GetPropertyExpression)
            {
                right = new ConstExpression(((GetPropertyExpression) right).getValue());
            }
            else{
                ((CypherExpression)right).removeElement(toRemove);
            }
        }
    }

    public enum BinaryNumberOperation{
        ADD("+"), MINUS("-"), MULTIPLY("*"), DIVISION("/");
        
        BinaryNumberOperation(String textRepresentation){
            this.TextRepresentation = textRepresentation;
        }
        
        private final String TextRepresentation;

        public String getTextRepresentation(){
            return this.TextRepresentation;
        }
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof BinaryNumberExpression)){
            return false;
        }
        return left.equals(((BinaryNumberExpression)o).left) && right.equals(((BinaryNumberExpression)o).right)
                && op == ((BinaryNumberExpression)o).op;
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
        if(left.getValue(varToProperties) == ExprVal.UNKNOWN || right.getValue(varToProperties) == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }
        long leftVal = (long) left.getValue(varToProperties);
        long rightVal = (long) right.getValue(varToProperties);
        switch (op){
            case ADD:
                return leftVal + rightVal;
            case MINUS:
                return leftVal - rightVal;
            case DIVISION:
                return (long) (leftVal / rightVal);
            case MULTIPLY:
                return leftVal * rightVal;
            default:
                throw new RuntimeException();
        }
    }

    public Long getValue()
    {
        long leftVal = -1;
        if(left instanceof IdentifierExpression)
            leftVal = (long) ((IdentifierExpression)left).getValue();
        else if(left instanceof ConstExpression)
            leftVal = (long) ((ConstExpression)left).getValue();
        else if(left instanceof GetPropertyExpression)
            leftVal = (long) ((GetPropertyExpression)left).getValue();
        else if(left instanceof BinaryNumberExpression)
            leftVal = (long) ((BinaryNumberExpression)left).getValue();
        else
            throw new RuntimeException("Unknown type of left expression: " + left.getClass().toString());
        long rightVal = -1;
        if(right instanceof IdentifierExpression)
            rightVal = (long) ((IdentifierExpression)right).getValue();
        else if(right instanceof ConstExpression)
            rightVal = (long) ((ConstExpression)right).getValue();
        else if(right instanceof GetPropertyExpression)
            rightVal = (long) ((GetPropertyExpression)right).getValue();
        else if(right instanceof BinaryNumberExpression)
            rightVal = (long) ((BinaryNumberExpression)right).getValue();
        else
            throw new RuntimeException("Unknown type of right expression: " + right.getClass().toString());

        switch (op){
            case ADD:
                return leftVal + rightVal;
            case MINUS:
                return leftVal - rightVal;
            case DIVISION:
                return leftVal / rightVal;
            case MULTIPLY:
                return leftVal * rightVal;
            default:
                throw new RuntimeException();
        }
    }

    public IExpression getLeft(){
        return left;
    }

    public IExpression getRight(){
        return right;
    }
}
