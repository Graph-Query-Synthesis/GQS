package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IContextInfo;
import org.example.gqs.cypher.ast.analyzer.IRelationAnalyzer;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.schema.IPropertyInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RelationAnalyzer extends RelationIdentifier implements IRelationAnalyzer {
    IRelationAnalyzer formerDef = null;
    IRelationIdentifier source;
    IExpression sourceExpression = null;
    IContextInfo contextInfo;

    RelationAnalyzer(IRelationIdentifier relationIdentifier, IContextInfo contextInfo, IExpression sourceExpression){
        this(relationIdentifier.getName(), relationIdentifier.getTypes().get(0), relationIdentifier.getDirection(),
                relationIdentifier.getProperties(), relationIdentifier.getLengthLowerBound(), relationIdentifier.getLengthUpperBound());
        source = relationIdentifier;
        if(((RelationIdentifier)source).actualRelationship==null)
        {
            if(contextInfo.getIdentifierByName(relationIdentifier.getName())!=null)
            {
                ((RelationIdentifier)source).actualRelationship = ((RelationIdentifier)(((RelationAnalyzer)contextInfo.getIdentifierByName(relationIdentifier.getName())).source)).actualRelationship;
            }
            else
            {
                System.out.println("error: node not found in contextInfo");
            }
        }
        this.actualRelationship = ((RelationIdentifier)source).actualRelationship;
        this.contextInfo = contextInfo;
        this.sourceExpression = sourceExpression;
        this.lengthLowerBound = source.getLengthLowerBound();
        this.lengthUpperBound = source.getLengthUpperBound();
    }

    RelationAnalyzer(IRelationIdentifier relationIdentifier, IContextInfo contextInfo){
        this(relationIdentifier, contextInfo, null);
    }

    public RelationAnalyzer(IRelationIdentifier node)
    {
        super(node.getName(), node.getTypes().get(0), node.getDirection(), node.getProperties(), node.getLengthLowerBound(), node.getLengthUpperBound());
        source = node;
    }

    RelationAnalyzer(String name, IType relationType, Direction direction, List<IProperty> properties, long lengthLowerBound, long lengthUpperBound) {
        super(name, relationType, direction, properties, lengthLowerBound, lengthUpperBound);
    }

    @Override
    public IRelationIdentifier getSource() {
        return source;
    }

    @Override
    public IExpression getSourceRefExpression() {
        return sourceExpression;
    }

    @Override
    public IContextInfo getContextInfo() {
        return contextInfo;
    }

    @Override
    public IRelationAnalyzer getFormerDef() {
        return formerDef;
    }

    @Override
    public void setFormerDef(IRelationAnalyzer formerDef) {
        this.formerDef = formerDef;
    }

    @Override
    public List<IType> getAllRelationTypesInDefChain() {
        if(this.relationType == null){
            if(formerDef != null){
                return formerDef.getTypes();
            }
            return new ArrayList<>();
        }
        return new ArrayList<IType>(Arrays.asList(this.relationType));
    }

    @Override
    public List<IProperty> getAllPropertiesInDefChain() {
        List<IProperty> properties = new ArrayList<>(this.properties);
        if(formerDef != null){
            properties.addAll(formerDef.getProperties());
            properties = properties.stream().distinct().collect(Collectors.toList());
        }
        return properties;
    }

    @Override
    public List<IPropertyInfo> getAllPropertiesAvailable(ICypherSchema schema) {
        List<IPropertyInfo> result = new ArrayList<>();
        if (((RelationIdentifier) this.source).actualRelationship != null) {
            AbstractRelationship curRelation = ((RelationIdentifier) this.source).actualRelationship;
            List<IPropertyInfo> allProperties = curRelation.getType().getProperties();
            for (IPropertyInfo propertyInfo : allProperties) {
                if (curRelation.getProperties().containsKey(propertyInfo.getKey())) {
                    result.add(propertyInfo);
                }
            }
            return result;
        }

        List<IType> relationTypes = getAllRelationTypesInDefChain();
        if (relationTypes.size() > 0) {
            IType relationType = relationTypes.get(0);
            if (schema.containsRelationType(relationType)) {
                return schema.getRelationInfo(relationType).getProperties();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<IPropertyInfo> getAllPropertiesWithType(ICypherSchema schema, ICypherType type) {
        return existedProperties().stream().filter(p->p.getType()==type).collect(Collectors.toList());
    }

    @Override
    public boolean isSingleRelation() {
        if(getFormerDef() != null){
            return getFormerDef().isSingleRelation();
        }

        return this.lengthLowerBound == 1 && this.lengthUpperBound == 1;
    }

    public IRelationIdentifier getCopy()
    {
        return (IRelationIdentifier) source.getCopy();
    }

    public List<IPropertyInfo> existedProperties() {
        List<IPropertyInfo> result = new ArrayList<>();
        if (((RelationIdentifier) this.source).actualRelationship != null) {
            AbstractRelationship curRelation = ((RelationIdentifier) this.source).actualRelationship;
            List<IPropertyInfo> allProperties = curRelation.getType().getProperties();
            for (IPropertyInfo propertyInfo : allProperties) {
                if (curRelation.getProperties().containsKey(propertyInfo.getKey())) {
                    result.add(propertyInfo);
                }
            }
        }
        return result;
    }


}
