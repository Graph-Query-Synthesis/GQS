package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.*;
import org.example.gqs.cypher.standard_ast.*;

import java.util.*;

public class IdentifierExpression extends CypherExpression {

    private final IIdentifier identifier;

    public IdentifierExpression(IIdentifier identifier){
        this.identifier = identifier;
    }

    public IIdentifier getIdentifier(){
        return identifier;
    }


    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append(identifier.getName());
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        IIdentifierAnalyzer identifierAnalyzer = identifiers.stream().filter(i->i.getName().equals(identifier.getName())).findAny().orElse(null);
        if(identifierAnalyzer != null){
            if(identifierAnalyzer instanceof INodeAnalyzer){
                return new CypherTypeDescriptor((INodeAnalyzer) identifierAnalyzer);
            }
            if(identifierAnalyzer instanceof IRelationAnalyzer){
                if(!((IRelationAnalyzer) identifierAnalyzer).isSingleRelation()) {
                    ListDescriptor listDescriptor = new ListDescriptor(new CypherTypeDescriptor((IRelationAnalyzer) identifierAnalyzer));
                    return new CypherTypeDescriptor(listDescriptor);
                }
                return new CypherTypeDescriptor((IRelationAnalyzer) identifierAnalyzer);
            }
            if(identifierAnalyzer instanceof IAliasAnalyzer){
                return  ((IAliasAnalyzer) identifierAnalyzer).analyzeType(schema);
            }
        }
        return new CypherTypeDescriptor(CypherType.UNKNOWN);
    }

    @Override
    public IExpression getCopy() {
        IIdentifier identifier = null;
        if(this.identifier != null){
            if(this.identifier instanceof NodeAnalyzer)
            {
            }
            identifier = this.identifier.getCopy();
        }
        return new IdentifierExpression(identifier);
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        if(varToProperties.containsKey(identifier.getName())){
            return varToProperties.get(identifier.getName());
        }
        return ExprVal.UNKNOWN;
    }

    public Object getValue() {
        if (identifier instanceof NodeIdentifier) {
            NodeIdentifier nodeIdentifier = (NodeIdentifier) identifier;
            return new GraphObjectVal(nodeIdentifier.getName(), nodeIdentifier.actualNode.getProperties());
        } else if (identifier instanceof RelationIdentifier) {
            RelationIdentifier relationIdentifier = (RelationIdentifier) identifier;
            return new GraphObjectVal(relationIdentifier.getName(), relationIdentifier.actualRelationship.getProperties());
        } else if (identifier instanceof Alias) {
            return ((CypherExpression) ((Alias) identifier).getExpression()).getValue();
        } else {
            throw new RuntimeException("Unknown identifier in IdentifierExpression getValue" + identifier.getClass().toString());
        }
    }

    public Set<IIdentifier> reliedContent()
    {
        Set<IIdentifier> result = new HashSet<>();
        if(identifier instanceof NodeIdentifier)
        {
            NodeIdentifier nodeIdentifier = (NodeIdentifier)identifier;
            result.add(nodeIdentifier);
        }
        else if(identifier instanceof RelationIdentifier)
        {
            RelationIdentifier relationIdentifier = (RelationIdentifier)identifier;
            result.add(relationIdentifier);
        }
        else if(identifier instanceof Alias) {
            Alias alias = (Alias) identifier;
            result.add(alias);
        }
        else
        {
            throw new RuntimeException("Unknown identifier in IdentifierExpression reliedContent" + identifier.getClass().toString());
        }
        return result;
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        throw new RuntimeException("Not implemented IdentifierExpression removeElement");
    }


    @Override
    public boolean equals(Object o){
        if(!(o instanceof IdentifierExpression)){
            return false;
        }
        return identifier.equals(((IdentifierExpression) o).identifier);
    }
}
