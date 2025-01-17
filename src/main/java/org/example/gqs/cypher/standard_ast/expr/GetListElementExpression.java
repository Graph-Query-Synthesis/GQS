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

public class GetListElementExpression extends CypherExpression{
    private IExpression listExpression, indexExpression;

    public GetListElementExpression(IExpression listExpression, IExpression indexExpression){
        this.listExpression  = listExpression;
        this.indexExpression = indexExpression;
    }
    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        ICypherTypeDescriptor typeDescriptor = listExpression.analyzeType(schema, identifiers);
        if(typeDescriptor.isList()){
            IListDescriptor listDescriptor = typeDescriptor.getListDescriptor();
            if(listDescriptor.isListLengthUnknown()){
                if(listDescriptor.isMembersWithSameType()){
                    return listDescriptor.getSameMemberType();
                }
                else{
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
        return new GetListElementExpression(listExpression.getCopy(), indexExpression.getCopy());
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        if(originalExpression == listExpression){
            this.listExpression = newExpression;
            newExpression.setParentExpression(this);
            return;
        }
        if(originalExpression == indexExpression){
            this.indexExpression = newExpression;
            newExpression.setParentExpression(this);
            return;
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        Object listObject = listExpression.getValue(varToProperties);
        Object indexObject = indexExpression.getValue(varToProperties);
        if(listObject == ExprVal.UNKNOWN || indexObject == ExprVal.UNKNOWN){
            return ExprVal.UNKNOWN;
        }
        List list = (List)listObject;
        long index = (long)indexObject;
        return list.get((int) index);
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        listExpression.toTextRepresentation(sb);
        sb.append("[");
        indexExpression.toTextRepresentation(sb);
        sb.append("]");
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof GetListElementExpression)){
            return false;
        }
        return listExpression.equals(((GetListElementExpression) o).listExpression) &&
                indexExpression.equals(((GetListElementExpression) o).indexExpression);
    }

    @Override
    public Object getValue() {
        throw new RuntimeException("not implemented GetListElementExpression getValue");
    }

    @Override
    public Set<IIdentifier> reliedContent() {
        throw new RuntimeException("not implemented GetListElementExpression reliedContent");
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        throw new RuntimeException("not implemented GetListElementExpression removeElement");

    }
}
