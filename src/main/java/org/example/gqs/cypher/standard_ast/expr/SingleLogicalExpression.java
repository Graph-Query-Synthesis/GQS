package org.example.gqs.cypher.standard_ast.expr;

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

public class SingleLogicalExpression extends CypherExpression {

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(CypherType.BOOLEAN);
    }

    public static SingleLogicalOperation randomOp(){
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 90);
        if(operationNum < 30){
            return SingleLogicalOperation.NOT;
        }
        if(operationNum < 60){
            return SingleLogicalOperation.IS_NULL;
        }
        return SingleLogicalOperation.IS_NOT_NULL;
    }

    public static SingleLogicalExpression randomLogical(IExpression expr) {
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 90);
        if (operationNum < 30) {
            return new SingleLogicalExpression(expr, SingleLogicalOperation.NOT);
        }
        if (operationNum < 60) {
            return new SingleLogicalExpression(expr, SingleLogicalOperation.IS_NULL);
        }
        return new SingleLogicalExpression(expr, SingleLogicalOperation.IS_NOT_NULL);
    }

    @Override
    public IExpression getCopy() {
        IExpression child = null;
        if(this.child != null){
            child = this.child.getCopy();
        }
        return new SingleLogicalExpression(child, this.op);
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        if(originalExpression == child){
            this.child = newExpression;
            newExpression.setParentExpression(this);
            return;
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        Object childObject= child.getValue(varToProperties);
        if(childObject == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }

        switch (op){
            case NOT:
                return !(boolean) childObject;
            case IS_NULL:
                return childObject == null;
            case IS_NOT_NULL:
                return childObject != null;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public Object getValue() {
        switch (op){
            case NOT:
                return !(boolean) ((CypherExpression)child).getValue();
            case IS_NULL:
                return ((CypherExpression)child).getValue() == null;
            case IS_NOT_NULL:
                return ((CypherExpression)child).getValue() != null;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public Set<IIdentifier> reliedContent() {
        return ((CypherExpression)child).reliedContent();
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        if(child instanceof IdentifierExpression || child instanceof GetPropertyExpression)
        {
            Set<IIdentifier> reliedContent = ((CypherExpression) child).reliedContent();
            for(IIdentifier identifier: toRemove){
                if(reliedContent.contains(identifier)){
                    child = new ConstExpression(((CypherExpression) child).getValue());
                    return;
                }
            }
        }
        else{
            Set<IIdentifier> reliedContent = ((CypherExpression) child).reliedContent();
            for(IIdentifier identifier: toRemove){
                if(reliedContent.contains(identifier)){
                    ((CypherExpression)child).removeElement(toRemove);
                    return;
                }
            }
        }

    }

    public enum SingleLogicalOperation{
        IS_NULL("IS NULL"),
        IS_NOT_NULL("IS NOT NULL"),
        NOT("NOT");

        SingleLogicalOperation(String textRepresentation){
            this.TextRepresentation = textRepresentation;
        }

        private final String TextRepresentation;

        public String getTextRepresentation(){
            return this.TextRepresentation;
        }
    }

    private IExpression child;
    private final SingleLogicalOperation op;

    public SingleLogicalExpression(IExpression child, SingleLogicalOperation op){
        this.child = child;
        this.op = op;
        child.setParentExpression(this);
    }

    public IExpression getChildExpression(){
        return child;
    }

    public SingleLogicalOperation getOperation(){
        return op;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("(");
        if(op == SingleLogicalOperation.NOT){
            sb.append(op.getTextRepresentation()).append(" ");
        }
        child.toTextRepresentation(sb);
        if(op != SingleLogicalOperation.NOT){
            sb.append(" ").append(op.getTextRepresentation());
        }
        sb.append(")");
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof SingleLogicalExpression)){
            return false;
        }
        if(child.equals(((SingleLogicalExpression) o).child)){
            return op == ((SingleLogicalExpression) o).op;
        }
        return false;
    }

}
