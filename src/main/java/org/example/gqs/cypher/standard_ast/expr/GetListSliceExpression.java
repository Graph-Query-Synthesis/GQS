package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IListDescriptor;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.CypherTypeDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetListSliceExpression extends CypherExpression{
    private IExpression listExpression, leftBound, rightBound;

    public GetListSliceExpression(IExpression listExpression, IExpression leftBound, IExpression rightBound){
        this.listExpression = listExpression;
        this.leftBound = leftBound;
        this.rightBound = rightBound;
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        ICypherTypeDescriptor typeDescriptor = listExpression.analyzeType(schema, identifiers);
        if(typeDescriptor.isList()){
            IListDescriptor listDescriptor = typeDescriptor.getListDescriptor();
            if(listDescriptor.isListLengthUnknown()){
                if(listDescriptor.isMembersWithSameType()){
                    return typeDescriptor;
                }
                else {
                    return new CypherTypeDescriptor(CypherType.UNKNOWN);
                }
            }
            else {
                return new CypherTypeDescriptor(CypherType.UNKNOWN);
            }

        }
        return new CypherTypeDescriptor(CypherType.UNKNOWN);
    }

    @Override
    public IExpression getCopy() {
        return new GetListSliceExpression(listExpression.getCopy(), leftBound.getCopy(), rightBound.getCopy());
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        listExpression.toTextRepresentation(sb);
        sb.append("[");
        if(leftBound!=null){
            leftBound.toTextRepresentation(sb);
        }
        sb.append("...");
        if(rightBound!=null){
            rightBound.toTextRepresentation(sb);
        }
        sb.append("]");
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof GetListSliceExpression)){
            return false;
        }
        return listExpression.equals(((GetListSliceExpression) o).listExpression) &&
                leftBound.equals(((GetListSliceExpression) o).leftBound) &&
                rightBound.equals(((GetListSliceExpression) o).rightBound);
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        if(originalExpression == listExpression){
            this.listExpression = newExpression;
            newExpression.setParentExpression(this);
            return;
        }
        if(originalExpression == leftBound){
            this.leftBound = newExpression;
            newExpression.setParentExpression(this);
            return;
        }
        if(originalExpression == rightBound){
            this.rightBound = newExpression;
            newExpression.setParentExpression(this);
            return;
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        Object listObject = listExpression.getValue(varToProperties);
        Object leftBoundObject = leftBound.getValue(varToProperties);
        Object rightBoundObject = rightBound.getValue(varToProperties);
        if(listObject == ExprVal.UNKNOWN || leftBoundObject == ExprVal.UNKNOWN || rightBoundObject == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }
        List list = (List)listObject;
        long leftBound = (long)leftBoundObject;
        long rightBound = (long)rightBoundObject;
        return list.subList((int) leftBound, (int) rightBound);
    }

    @Override
    public Object getValue() {
        throw new RuntimeException("not implemented GetListSliceExpression getValue");
    }

    @Override
    public Set<IIdentifier> reliedContent() {
        throw new RuntimeException("not implemented GetListSliceExpression reliedContent");
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        throw new RuntimeException("not implemented GetListSliceExpression removeElement");
    }
}
