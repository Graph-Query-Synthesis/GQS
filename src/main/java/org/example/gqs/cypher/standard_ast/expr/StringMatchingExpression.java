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
import java.util.Objects;
import java.util.Set;

public class StringMatchingExpression extends CypherExpression {
    private IExpression source, pattern;
    private StringMatchingOperation op;

    public IExpression getSource() {
        return source;
    }

    public void setSource(IExpression source) {
        this.source = source;
    }

    public IExpression getPattern() {
        return pattern;
    }

    public void setPattern(IExpression pattern) {
        this.pattern = pattern;
    }

    public StringMatchingOperation getOp() {
        return op;
    }

    public void setOp(StringMatchingOperation op) {
        this.op = op;
    }

    public StringMatchingExpression(IExpression source, IExpression pattern, StringMatchingOperation op){
        this.source = source;
        this.pattern = pattern;
        this.op = op;
        source.setParentExpression(this);
        pattern.setParentExpression(this);
    }

    @Override
    public Object getValue() {
        Object sourceObject = ((CypherExpression)source).getValue();
        Object patternObject = ((CypherExpression)pattern).getValue();
        if(sourceObject == ExprVal.UNKNOWN || patternObject == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }
        switch (op){
            case CONTAINS:
                return ((String)sourceObject).contains((String)patternObject);
            case STARTS_WITH:
                return ((String)sourceObject).startsWith((String) patternObject);
            case ENDS_WITH:
                return ((String)sourceObject).endsWith((String) patternObject);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public Set<IIdentifier> reliedContent() {
        Set<IIdentifier> result = ((CypherExpression)source).reliedContent();
        result.addAll(((CypherExpression)pattern).reliedContent());
        return result;
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        CypherExpression sourceExpression = (CypherExpression)source;
        CypherExpression patternExpression = (CypherExpression)pattern;
        Set<IIdentifier> sourceReliedContent = sourceExpression.reliedContent();
        Set<IIdentifier> patternReliedContent = patternExpression.reliedContent();
        boolean remove = false;
        for(IIdentifier identifier : toRemove){
            if(sourceReliedContent.contains(identifier)){
                remove = true;
                break;
            }
            if(patternReliedContent.contains(identifier)){
                remove = true;
                break;
            }
        }
        if(remove){
            this.source = new ConstExpression(((CypherExpression) source).getValue());
            this.pattern = new ConstExpression(((CypherExpression) pattern).getValue());
        }
    }

    public enum StringMatchingOperation{
        CONTAINS("CONTAINS"),
        STARTS_WITH("STARTS WITH"),
        ENDS_WITH("ENDS WITH");

        StringMatchingOperation(String textRepresentation){
            this.TextRepresentation = textRepresentation;
        }

        private final String TextRepresentation;

        public String getTextRepresentation(){
            return this.TextRepresentation;
        }
    }

    public static StringMatchingOperation randomOperation(){
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 90);
        if(operationNum < 30){
            return StringMatchingOperation.CONTAINS;
        }
        if(operationNum < 60 && !Objects.equals(MainOptions.mode, "kuzu")) {
            return StringMatchingOperation.ENDS_WITH;
        }
        return StringMatchingOperation.STARTS_WITH;
    }

    public static StringMatchingExpression randomMatching(IExpression left, IExpression right){
        Randomly randomly = new Randomly();
        long operationNum = randomly.getInteger(0, 90);
        if(operationNum < 30){
            return new  StringMatchingExpression(left, right, StringMatchingOperation.CONTAINS);
        }
        if(operationNum < 60 && !Objects.equals(MainOptions.mode, "kuzu")) {
            return new StringMatchingExpression(left, right, StringMatchingOperation.ENDS_WITH);
        }
        return new  StringMatchingExpression(left, right, StringMatchingOperation.STARTS_WITH);
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        return new CypherTypeDescriptor(CypherType.BOOLEAN);
    }

    @Override
    public IExpression getCopy() {
        return new StringMatchingExpression(source.getCopy(), pattern.getCopy(), op);
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("(");
        source.toTextRepresentation(sb);
        sb.append(" ").append(op.getTextRepresentation()).append(" ");
        pattern.toTextRepresentation(sb);
        sb.append(")");
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        if(originalExpression == source){
            this.source = newExpression;
            newExpression.setParentExpression(this);
            return;
        }
        if(originalExpression == pattern){
            this.pattern = newExpression;
            newExpression.setParentExpression(this);
            return;
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        Object sourceObject = source.getValue(varToProperties);
        Object patternObject = pattern.getValue(varToProperties);
        if(sourceObject == ExprVal.UNKNOWN || patternObject == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }
        switch (op){
            case CONTAINS:
                return ((String)sourceObject).contains((String)patternObject);
            case STARTS_WITH:
                return ((String)sourceObject).startsWith((String) patternObject);
            case ENDS_WITH:
                return ((String)sourceObject).endsWith((String) patternObject);
            default:
                throw new RuntimeException();
        }
    }
}
