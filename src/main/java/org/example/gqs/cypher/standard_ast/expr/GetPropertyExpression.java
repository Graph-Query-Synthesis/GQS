package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.*;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.cypher.standard_ast.*;

import java.util.*;

public class GetPropertyExpression extends CypherExpression {
    private IExpression fromExpression;
    private final String propertyName;

    public GetPropertyExpression(IExpression fromExpression, String propertyName){
        this.fromExpression = fromExpression;
        this.propertyName = propertyName;
        fromExpression.setParentExpression(this);
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("(");
        fromExpression.toTextRepresentation(sb);
        sb.append(".").append(propertyName).append(")");
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        ICypherTypeDescriptor fromExpressionType = fromExpression.analyzeType(schema, identifiers);
        if (fromExpressionType.isNode()) {
            INodeAnalyzer nodeAnalyzer = fromExpressionType.getNodeAnalyzer();
            IPropertyInfo propertyInfo = nodeAnalyzer.getAllPropertiesAvailable(schema).stream()
                    .filter(p -> p.getKey().equals(propertyName)).findAny().orElse(null);
            if (propertyInfo != null) {
                return new CypherTypeDescriptor(propertyInfo.getType());
            }
            return new CypherTypeDescriptor(CypherType.UNKNOWN);
        }
        if (fromExpressionType.isRelation()) {
            IRelationAnalyzer relationAnalyzer = fromExpressionType.getRelationAnalyzer();
            IPropertyInfo propertyInfo = relationAnalyzer.getAllPropertiesAvailable(schema).stream()
                    .filter(p -> p.getKey().equals(propertyName)).findAny().orElse(null);
            if (propertyInfo != null) {
                return new CypherTypeDescriptor(propertyInfo.getType());
            }
            return new CypherTypeDescriptor(CypherType.UNKNOWN);
        }
        if (fromExpressionType.isMap()) {
            IMapDescriptor mapDescriptor = fromExpressionType.getMapDescriptor();
            if (!mapDescriptor.isMapSizeUnknown()) {
                if (mapDescriptor.getMapMemberTypes().containsKey(propertyName)) {
                    return mapDescriptor.getMapMemberTypes().get(propertyName);
                }
            }
            return new CypherTypeDescriptor(CypherType.UNKNOWN);
        }
        return new CypherTypeDescriptor(CypherType.UNKNOWN);
    }

    @Override
    public IExpression getCopy() {
        return new GetPropertyExpression(fromExpression.getCopy(), propertyName);
    }
    @Override
    public boolean equals(Object o){
        if(!(o instanceof GetPropertyExpression)){
            return false;
        }
        return fromExpression.equals(((GetPropertyExpression) o).fromExpression) && propertyName.equals(((GetPropertyExpression) o).propertyName);
    }

    public IExpression getFromExpression() {
        return fromExpression;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        if(originalExpression == fromExpression){
            this.fromExpression = newExpression;
            newExpression.setParentExpression(this);
            return;
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        return getValue();
    }

    public Object getValue()
    {
        Object value = null;
        if(fromExpression instanceof IdentifierExpression)
        {
            if(((IdentifierExpression)fromExpression).getIdentifier() instanceof NodeIdentifier)
            {
                if(((IdentifierExpression)fromExpression).getIdentifier() instanceof NodeAnalyzer)
                    value = ((NodeIdentifier)(((NodeAnalyzer)(((((IdentifierExpression)fromExpression).getIdentifier())))).getSource())).actualNode.getProperties().get(propertyName);
                else
                    value = ((NodeIdentifier)(((NodeIdentifier)(((((IdentifierExpression)fromExpression).getIdentifier())))))).actualNode.getProperties().get(propertyName);
            }
            else if(((IdentifierExpression)fromExpression).getIdentifier() instanceof RelationIdentifier)
            {
                if(((IdentifierExpression)fromExpression).getIdentifier() instanceof RelationAnalyzer)
                    value = ((RelationIdentifier)(((RelationAnalyzer)(((((IdentifierExpression)fromExpression).getIdentifier())))).getSource())).actualRelationship.getProperties().get(propertyName);
                else
                    value = ((RelationIdentifier)(((RelationIdentifier)(((((IdentifierExpression)fromExpression).getIdentifier())))))).actualRelationship.getProperties().get(propertyName);

            }
            else
            {
                throw new RuntimeException("Unknown type of identifier in GetPropertyExpression" + ((IdentifierExpression)fromExpression).getIdentifier().getClass().toString());
            }
        }
        else if(fromExpression instanceof GetPropertyExpression)
        {
            value = ((GetPropertyExpression)fromExpression).getValue();
        }
        else
        {
            throw new RuntimeException("Unknown type of expression in GetPropertyExpression" + fromExpression.getClass().toString());
        }

        return value;
    }

    public Set<IIdentifier> reliedContent()
    {
        Set<IIdentifier> result = new HashSet<>();
        if(fromExpression instanceof IdentifierExpression)
        {
            if(((IdentifierExpression)fromExpression).getIdentifier() instanceof NodeIdentifier)
            {
                result.add((((NodeIdentifier)(((((IdentifierExpression)fromExpression).getIdentifier()))))));
            }
            else if(((IdentifierExpression)fromExpression).getIdentifier() instanceof RelationIdentifier)
            {
                result.add((((RelationIdentifier)(((((IdentifierExpression)fromExpression).getIdentifier()))))));
            }
            else if (((IdentifierExpression)fromExpression).getIdentifier() instanceof Alias)
            {
                result.add((Alias)(((IdentifierExpression) fromExpression).getIdentifier()));
            }
            else
            {
                throw new RuntimeException("Unknown type of identifier in GetPropertyExpression");
            }
        }
        else if(fromExpression instanceof GetPropertyExpression)
        {
            result.addAll(((GetPropertyExpression)fromExpression).reliedContent());
        }
        else
        {
            throw new RuntimeException("Unknown type of expression in GetPropertyExpression");
        }

        return result;
    }

    @Override
    public void removeElement (Set<IIdentifier> toRemove) {
        throw new RuntimeException("Remove element got invoked into GetPropertyExpression, not designed for this!");
    }

    public ICypherType getCorrectType()
    {
        Object value;
        if(((IdentifierExpression)fromExpression).getIdentifier() instanceof NodeAnalyzer)
        {
            value = (((NodeIdentifier)(((NodeAnalyzer)(((IdentifierExpression)fromExpression).getIdentifier())).getSource()))).actualNode.getProperties().get(propertyName);
        }
        else if(((IdentifierExpression)fromExpression).getIdentifier() instanceof RelationAnalyzer)
        {
            value = ((RelationIdentifier)(((RelationAnalyzer)(((IdentifierExpression)fromExpression).getIdentifier())).getSource())).actualRelationship.getProperties().get(propertyName);
        }
        else if (((IdentifierExpression)fromExpression).getIdentifier() instanceof NodeIdentifier)
        {
            value = ((NodeIdentifier)((IdentifierExpression)fromExpression).getIdentifier()).actualNode.getProperties().get(propertyName);
        }
        else if (((IdentifierExpression)fromExpression).getIdentifier() instanceof RelationIdentifier)
        {
            value = ((RelationIdentifier)((IdentifierExpression)fromExpression).getIdentifier()).actualRelationship.getProperties().get(propertyName);
        }
        else
        {
            throw new RuntimeException("Unknown type of identifier in GetPropertyExpression" + ((IdentifierExpression)fromExpression).getIdentifier().getClass().toString());
        }
        try
        {
            long number = Long.parseLong(value.toString());
            return CypherType.NUMBER;
        }
        catch (NumberFormatException e)
        {
            return CypherType.STRING;
        }
    }


}
