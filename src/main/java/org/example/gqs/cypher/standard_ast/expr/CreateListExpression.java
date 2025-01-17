package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IListDescriptor;
import org.example.gqs.cypher.standard_ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class CreateListExpression extends CypherExpression {
    private List<IExpression> listElements;
    public List<Object> elementValue;

    public CreateListExpression(List<IExpression> listElements){
        this.listElements = listElements;
    }
    public long getListSize()
    {
        return listElements.size();
    }
    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        IListDescriptor listDescriptor = new ListDescriptor(listElements.stream()
                .map(e->e.analyzeType(schema, identifiers)).collect(Collectors.toList()));
        return new CypherTypeDescriptor(listDescriptor);
    }

    @Override
    public IExpression getCopy() {
        List<IExpression> newListElements = new ArrayList<>();
        listElements.forEach(e-> newListElements.add(e.getCopy()));
        if(elementValue != null)
            elementValue.forEach(e-> newListElements.add(new ConstExpression(e)));
        return new CreateListExpression(newListElements);
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        for(int i = 0; i < listElements.size(); i++){
            if(originalExpression == listElements.get(i)){
                listElements.set(i, newExpression);
                newExpression.setParentExpression(this);
                return;
            }
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        return listElements.stream().map(e -> e.getValue(varToProperties)).collect(Collectors.toList());
    }

    public List<Object> getValue()
    {
        if(elementValue != null)
        {
            return elementValue;
        }
        List<Object> value = new ArrayList<>();
        for(IExpression e : listElements)
        {
            value.add(((CypherExpression)e).getValue());
        }
            return value;
    }

    public Set<IIdentifier> reliedContent()
    {
        Set<IIdentifier> result = new HashSet<>();
        for(IExpression e : listElements)
        {
            result.addAll(((CypherExpression)e).reliedContent());
        }
        return result;
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        List<IExpression> newListElements = new ArrayList<>();
        for(IExpression e : listElements)
        {
            CypherExpression temp = (CypherExpression)e;
            Set<IIdentifier> reliedContent = temp.reliedContent();
            boolean remove = false;
            for(IIdentifier identifier : toRemove){
                if(reliedContent.contains(identifier)){
                    remove = true;
                    break;
                }
            }
            if(!remove)
            {
                newListElements.add(e);
            }
            else {
                if (e instanceof IdentifierExpression) {
                    if (((IdentifierExpression) e).getIdentifier() instanceof Alias) {
                        Alias alias = (Alias) ((IdentifierExpression) e).getIdentifier();
                        if (alias.getExpression() instanceof NodeIdentifier || alias.getExpression() instanceof RelationIdentifier) {
                            newListElements.add(new ConstExpression(999));
                        } else {
                            newListElements.add(new ConstExpression(((CypherExpression) e).getValue()));
                        }
                    }
                } else
                    newListElements.add(new ConstExpression(((CypherExpression) e).getValue()));
            }
        }
        listElements = newListElements;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("[");
        for(int i = 0; i < listElements.size(); i++){
            if(i!=0){
                sb.append(", ");
            }
            listElements.get(i).toTextRepresentation(sb);
        }
        sb.append("]");
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof CreateListExpression)){
            return false;
        }
        if(listElements.size() != ((CreateListExpression) o).listElements.size()){
            return false;
        }
        return listElements.containsAll(((CreateListExpression) o).listElements);
    }
}
