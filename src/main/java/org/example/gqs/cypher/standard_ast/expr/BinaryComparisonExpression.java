package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.standard_ast.Alias;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.CypherTypeDescriptor;

import java.util.*;

public class BinaryComparisonExpression extends CypherExpression {

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(CypherType.BOOLEAN);
    }


    @Override
    public Object getValue() {
        Object leftObject = ((CypherExpression)left).getValue();
        Object rightObject = ((CypherExpression)right).getValue();
        if(leftObject == ExprVal.UNKNOWN || rightObject == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }
        if(leftObject instanceof String){
            switch (op){
                case SMALLER:
                    return ((String) leftObject).compareTo((String) rightObject) < 0;
                case SMALLER_OR_EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) <= 0;
                case HIGHER:
                    return ((String) leftObject).compareTo((String) rightObject) > 0;
                case NOT_EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) != 0;
                case EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) == 0;
                case HIGHER_OR_EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) >= 0;
                default:
                    throw new RuntimeException();
            }
        }
        if(leftObject instanceof Number){
            return switch (op) {
                case SMALLER -> ((long) leftObject) < ((long) rightObject);
                case SMALLER_OR_EQUAL -> ((long) leftObject) <= ((long) rightObject);
                case HIGHER -> ((long) leftObject) > ((long) rightObject);
                case NOT_EQUAL -> ((long) leftObject) != ((long) rightObject);
                case EQUAL -> ((long) leftObject) == ((long) rightObject);
                case HIGHER_OR_EQUAL -> ((long) leftObject) >= ((long) rightObject);
                default -> throw new RuntimeException();
            };
        }
        if(leftObject instanceof Boolean){
            return switch (op) {
                case EQUAL -> leftObject == rightObject;
                case NOT_EQUAL -> leftObject != rightObject;
                default -> throw new RuntimeException();
            };
        }
        throw new RuntimeException();
    }

    public Set<IIdentifier> reliedContent()
    {
        Set<IIdentifier> result = new HashSet<>();
        if(left instanceof CypherExpression)
        {
            result.addAll(((CypherExpression)left).reliedContent());
        }
        else
        {
            throw new RuntimeException("In BinaryComparisonExpression, left is not a CypherExpression, but "+left.getClass().getName());
        }
        if(right instanceof CypherExpression)
        {
            result.addAll(((CypherExpression)right).reliedContent());
        }
        else
        {
            throw new RuntimeException("In BinaryComparisonExpression, right is not a CypherExpression, but "+right.getClass().getName());
        }
        return result;
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        CypherExpression leftExpression = (CypherExpression) left;
        CypherExpression rightExpression = (CypherExpression) right;
        Set<IIdentifier> leftReliedContent = leftExpression.reliedContent();
        Set<IIdentifier> rightReliedContent = rightExpression.reliedContent();
        if (leftExpression instanceof GetPropertyExpression && ((GetPropertyExpression) leftExpression).getFromExpression() instanceof IdentifierExpression && ((IdentifierExpression) ((GetPropertyExpression) leftExpression).getFromExpression()).getIdentifier().getName().equals("r5")) {
            if (rightExpression instanceof GetPropertyExpression && ((GetPropertyExpression) rightExpression).getFromExpression() instanceof IdentifierExpression && ((IdentifierExpression) ((GetPropertyExpression) rightExpression).getFromExpression()).getIdentifier().getName().equals("n0"))
                System.out.println("dd");
        }

        Set<String> leftString = new HashSet<>();
        for (IIdentifier identifier : leftReliedContent) {
            leftString.add(identifier.getName());
        }
        Set<String> rightString = new HashSet<>();
        for (IIdentifier identifier : rightReliedContent) {
            rightString.add(identifier.getName());
        }

        String toRuleOut = "";
        boolean removeLeft = false;
        boolean removeRight = false;
        for (IIdentifier identifier : toRemove) {
            if (removeLeft && removeRight)
                break;
            if (!removeLeft && leftString.contains(identifier.getName())) {
                removeLeft = true;
                if (!leftReliedContent.contains(identifier))
                    toRuleOut = "left";
            }
            if (!removeRight && rightString.contains(identifier.getName())) {
                removeRight = true;
                if (!rightReliedContent.contains(identifier))
                    toRuleOut = "right";
            }
        }

        if (removeLeft) {
            if (left instanceof IdentifierExpression) {
                left = new ConstExpression(((IdentifierExpression) left).getValue());
            } else if (left instanceof GetPropertyExpression) {
                if (toRuleOut.equals("left")) {
                    IdentifierExpression original = null;
                    for (IIdentifier identifier : toRemove) {
                        if (leftString.contains(identifier.getName()) && !leftReliedContent.contains(identifier)) {
                            original = new IdentifierExpression(identifier);
                        }
                    }
                    if (original == null) {
                        throw new RuntimeException("In BinaryComparisonExpression removeElement: left is GetPropertyExpression, trying to rule out an element, but no original identifier found");
                    }
                    left = new GetPropertyExpression(original, ((GetPropertyExpression) left).getPropertyName());
                }
                left = new ConstExpression(((GetPropertyExpression) left).getValue());
            } else {
                ((CypherExpression) left).removeElement(toRemove);
            }
        }
        if (removeRight) {
            if (right instanceof IdentifierExpression && toRemove.contains(((IdentifierExpression) right).getIdentifier())) {
                right = new ConstExpression(((IdentifierExpression) right).getValue());
            } else if (right instanceof GetPropertyExpression) {
                if (toRuleOut.equals("right")) {
                    IdentifierExpression original = null;
                    for (IIdentifier identifier : toRemove) {
                        if (rightString.contains(identifier.getName()) && !rightReliedContent.contains(identifier)) {
                            original = new IdentifierExpression(identifier);
                        }
                    }
                    if (original == null) {
                        throw new RuntimeException("In BinaryComparisonExpression removeElement: right is GetPropertyExpression, trying to rule out an element, but no original identifier found");
                    }
                    right = new GetPropertyExpression(original, ((GetPropertyExpression) right).getPropertyName());
                }
                right = new ConstExpression(((GetPropertyExpression) right).getValue());
            } else {
                ((CypherExpression) right).removeElement(toRemove);
            }
        }
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
        return new BinaryComparisonExpression(left, right, this.op);
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
        Object leftObject = left.getValue(varToProperties);
        Object rightObject = right.getValue(varToProperties);
        if(leftObject == ExprVal.UNKNOWN || rightObject == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }
        if(leftObject instanceof String){
            switch (op){
                case SMALLER:
                    return ((String) leftObject).compareTo((String) rightObject) < 0;
                case SMALLER_OR_EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) <= 0;
                case HIGHER:
                    return ((String) leftObject).compareTo((String) rightObject) > 0;
                case NOT_EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) != 0;
                case EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) == 0;
                case HIGHER_OR_EQUAL:
                    return ((String) leftObject).compareTo((String) rightObject) >= 0;
                default:
                    throw new RuntimeException();
            }
        }
        if(leftObject instanceof Number){
            switch (op){
                case SMALLER:
                    return ((long)leftObject) < ((long) rightObject);
                case SMALLER_OR_EQUAL:
                    return ((long)leftObject) <= ((long) rightObject);
                case HIGHER:
                    return ((long)leftObject) > ((long) rightObject);
                case NOT_EQUAL:
                    return ((long)leftObject) != ((long) rightObject);
                case EQUAL:
                    return ((long)leftObject) == ((long) rightObject);
                case HIGHER_OR_EQUAL:
                    return ((long)leftObject) >= ((long) rightObject);
                default:
                    throw new RuntimeException();
            }
        }
        throw new RuntimeException();
    }

    public static BinaryComparisonOperation randomOperation(){
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 100);
        if(operationNum < 5){
            return BinaryComparisonOperation.NOT_EQUAL;
        }
        if(operationNum < 50){
            return BinaryComparisonOperation.EQUAL;
        }
        if(operationNum < 60){
            return BinaryComparisonOperation.HIGHER;
        }
        if(operationNum < 70){
            return BinaryComparisonOperation.HIGHER_OR_EQUAL;
        }
        if(operationNum < 80){
            return BinaryComparisonOperation.SMALLER;
        }
        return BinaryComparisonOperation.SMALLER_OR_EQUAL;
    }

    public static BinaryComparisonExpression randomComparison(IExpression left, IExpression right){
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 100);
        if(operationNum < 5){
            return new BinaryComparisonExpression(left, right, BinaryComparisonOperation.EQUAL);
        }
        if(operationNum < 20){
            return new BinaryComparisonExpression(left, right, BinaryComparisonOperation.NOT_EQUAL);
        }
        if(operationNum < 40){
            return new BinaryComparisonExpression(left, right, BinaryComparisonOperation.HIGHER);
        }
        if(operationNum < 60){
            return new BinaryComparisonExpression(left, right, BinaryComparisonOperation.HIGHER_OR_EQUAL);
        }
        if(operationNum < 80){
            return new BinaryComparisonExpression(left, right, BinaryComparisonOperation.SMALLER);
        }
        return new BinaryComparisonExpression(left, right, BinaryComparisonOperation.SMALLER_OR_EQUAL);
    }

    public enum BinaryComparisonOperation{
        SMALLER("<"),
        EQUAL("="),
        SMALLER_OR_EQUAL("<="),
        HIGHER(">"),
        HIGHER_OR_EQUAL(">="),
        NOT_EQUAL("<>");

        BinaryComparisonOperation(String textRepresentation){
            this.TextRepresentation = textRepresentation;
        }

        private final String TextRepresentation;

        public String getTextRepresentation(){
            return this.TextRepresentation;
        }

        public BinaryComparisonOperation reverse(){
            switch (this){
                case EQUAL:
                    return NOT_EQUAL;
                case NOT_EQUAL:
                    return EQUAL;
                case HIGHER:
                    return SMALLER_OR_EQUAL;
                case HIGHER_OR_EQUAL:
                    return SMALLER;
                case SMALLER:
                    return HIGHER_OR_EQUAL;
                case SMALLER_OR_EQUAL:
                    return HIGHER;
                default:
                    throw new RuntimeException();
            }
        }
    }

    private IExpression left, right;
    private BinaryComparisonOperation op;

    public BinaryComparisonExpression(IExpression left, IExpression right, BinaryComparisonOperation op) {
        left.setParentExpression(this);
        right.setParentExpression(this);
        this.left = left;
        this.right = right;
        this.op = op;
        if (MainOptions.mode == "thinker") {
            if (right instanceof IdentifierExpression && ((IdentifierExpression) right).getIdentifier() instanceof Alias) {
                Object value = ((IdentifierExpression) right).getValue();
                this.right = new ConstExpression(value);
            }
        }
    }

    public IExpression getLeftExpression(){
        return left;
    }

    public IExpression getRightExpression(){
        return right;
    }

    public BinaryComparisonOperation getOperation(){
        return op;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        if(((CypherExpression)right).getValue() == null)
        {
            sb.append("(");
            left.toTextRepresentation(sb);
            if(op == BinaryComparisonOperation.EQUAL)
                sb.append(" is ");
            else
                sb.append(" is not ");
            sb.append(" null)");
        }
        else {
            sb.append("(");
            left.toTextRepresentation(sb);
            sb.append(" ").append(op.getTextRepresentation()).append(" ");
            right.toTextRepresentation(sb);
            sb.append(")");
        }
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof BinaryComparisonExpression)){
            return false;
        }
        return left.equals(((BinaryComparisonExpression)o).left) && right.equals(((BinaryComparisonExpression)o).right)
                && op == ((BinaryComparisonExpression)o).op;
    }

    public void setOperation(BinaryComparisonOperation op){
        this.op = op;
    }
}
