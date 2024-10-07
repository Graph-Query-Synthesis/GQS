package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.standard_ast.Alias;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.CypherTypeDescriptor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BinaryLogicalExpression extends CypherExpression {

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(CypherType.BOOLEAN);
    }

    @Override
    public IExpression getCopy() {
        IExpression left = null, right = null;
        if(this.left != null){
            left = this.left.getCopy();
        }
        if(this.right != null){
            right = this.right.getCopy();
        }
        return new BinaryLogicalExpression(left, right, this.op);
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
        switch (op){
            case AND:
                return (boolean) left.getValue(varToProperties) && (boolean) right.getValue(varToProperties);
            case OR:
                return (boolean) left.getValue(varToProperties) || (boolean) right.getValue(varToProperties);
            case XOR:
                return (boolean) left.getValue(varToProperties) ^ (boolean) right.getValue(varToProperties);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public Object getValue() {
        switch (op){
            case AND:
                return (boolean) ((CypherExpression)left).getValue() && (boolean) ((CypherExpression)right).getValue();
            case OR:
                return (boolean) ((CypherExpression)left).getValue() || (boolean) ((CypherExpression)right).getValue();
            case XOR:
                return (boolean) ((CypherExpression)left).getValue() ^ (boolean) ((CypherExpression)right).getValue();
            default:
                throw new RuntimeException();
        }
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

        Set<String> leftString = new HashSet<>();
        for(IIdentifier identifier : leftReliedContent){
            leftString.add(identifier.getName());
        }
        Set<String> rightString = new HashSet<>();
        for(IIdentifier identifier : rightReliedContent){
            rightString.add(identifier.getName());
        }

        boolean removeLeft = false;
        boolean removeRight = false;
        for(IIdentifier identifier : toRemove){
            if(removeLeft && removeRight)
                break;
            if(!removeLeft && leftString.contains(identifier.getName())) {
                removeLeft = true;
            }
            if(!removeRight && rightString.contains(identifier.getName())) {
                removeRight = true;
            }

        }
        if(removeLeft)
        {
            if(left instanceof IdentifierExpression) {
                if (((IdentifierExpression) left).getIdentifier() instanceof Alias) {
                    left = new ConstExpression(((IdentifierExpression) left).getValue());
                } else {
                    throw new RuntimeException("In BinaryLogicalExpression removeElement: left is IdentifierExpression but not Alias");
                }
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
            if(right instanceof IdentifierExpression && toRemove.contains(((IdentifierExpression) right).getIdentifier())) {
                if (((IdentifierExpression) right).getIdentifier() instanceof Alias) {
                    right = new ConstExpression(((IdentifierExpression) right).getValue());
                } else {
                    throw new RuntimeException("In BinaryLogicalExpression removeElement: right is IdentifierExpression but not Alias");
                }
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

    public enum BinaryLogicalOperation{
        OR("OR"),
        AND("AND"),
        XOR("XOR");

        BinaryLogicalOperation(String textRepresentation){
            this.TextRepresentation = textRepresentation;
        }

        private final String TextRepresentation;

        public String getTextRepresentation(){
            return this.TextRepresentation;
        }
    }

    public static BinaryLogicalOperation randomOp() {
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 60);
        if (operationNum < 30) {
            return BinaryLogicalOperation.AND;
        }
        if (operationNum < 60) {
            return BinaryLogicalOperation.OR;
        }
        return BinaryLogicalOperation.XOR;
    }

    public static BinaryLogicalExpression randomLogical(IExpression left, IExpression right) {
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 60);
        if (operationNum < 30) {
            return new BinaryLogicalExpression(left, right, BinaryLogicalOperation.AND);
        }
        if (operationNum < 60) {
            return new BinaryLogicalExpression(left, right, BinaryLogicalOperation.OR);
        }
        return new BinaryLogicalExpression(left, right, BinaryLogicalOperation.XOR);
    }

    private IExpression left;
    private IExpression right;
    private BinaryLogicalOperation op;

    public BinaryLogicalExpression(IExpression left, IExpression right, BinaryLogicalOperation op){
        left.setParentExpression(this);
        right.setParentExpression(this);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public IExpression getLeftExpression(){
        return left;
    }
    public IExpression setLeftExpression(IExpression left)
    {
        this.left = left;
        return this;
    }

    public IExpression getRightExpression(){
        return right;
    }
    public IExpression setRightExpression(IExpression right)
    {
        this.right = right;
        return this;
    }

    public BinaryLogicalOperation getOperation(){
        return op;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("(");
        left.toTextRepresentation(sb);
        sb.append(" ").append(op.getTextRepresentation()).append(" ");
        right.toTextRepresentation(sb);
        sb.append(")");
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof BinaryLogicalExpression)){
            return false;
        }
        return left.equals(((BinaryLogicalExpression)o).left) && right.equals(((BinaryLogicalExpression)o).right)
                && op == ((BinaryLogicalExpression)o).op;
    }
}
