package org.example.gqs.cypher.standard_ast;


import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IContextInfo;
import org.example.gqs.cypher.ast.analyzer.INodeAnalyzer;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.schema.IPropertyInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NodeAnalyzer extends NodeIdentifier implements INodeAnalyzer {
    INodeAnalyzer formerDef = null;
    INodeIdentifier source;
    IExpression sourceExpression;
    IContextInfo contextInfo;

    NodeAnalyzer(INodeIdentifier nodeIdentifier, IContextInfo contextInfo){
        this(nodeIdentifier, contextInfo, null);
    }

    public NodeAnalyzer(INodeIdentifier node)
    {
        super(node.getName(), node.getLabels(), node.getProperties());
        source = node;
    }

    NodeAnalyzer(INodeIdentifier nodeIdentifier, IContextInfo contextInfo, IExpression sourceExpression) {
        super(nodeIdentifier.getName(), nodeIdentifier.getLabels(), nodeIdentifier.getProperties());
        source = nodeIdentifier;
        if(((NodeIdentifier)source).actualNode==null)
        {
            if(contextInfo.getIdentifierByName(nodeIdentifier.getName())!=null)
            {
                ((NodeIdentifier)source).actualNode = ((NodeIdentifier)(((NodeAnalyzer)contextInfo.getIdentifierByName(nodeIdentifier.getName())).source)).actualNode;
            }
            else
            {
                System.out.println("error: node not found in contextInfo");
            }
        }
        this.actualNode = ((NodeIdentifier)source).actualNode;
        this.sourceExpression = sourceExpression;
        this.contextInfo = contextInfo;
    }

    @Override
    public INodeIdentifier getSource() {
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
    public INodeAnalyzer getFormerDef() {
        return formerDef;
    }

    public AbstractNode getActualNode() {
        return ((NodeIdentifier)getSource()).getActualNode();
    }
    @Override
    public void setFormerDef(INodeAnalyzer formerDef) {
        this.formerDef = formerDef;
    }

    @Override
    public List<ILabel> getAllLabelsInDefChain() {
        List<ILabel> labels = new ArrayList<>(this.labels);
        if(formerDef != null){
            labels.addAll(formerDef.getLabels());
            labels = labels.stream().distinct().collect(Collectors.toList());
        }
        return labels;
    }

    @Override
    public List<IProperty> getAllPropertiesInDefChain() {
        List<IProperty> properties = new ArrayList<>(this.properties);
        if(formerDef != null){
            properties.addAll(formerDef.getProperties());
        }
        return properties;
    }

    @Override
    public List<IPropertyInfo> getAllPropertiesAvailable(ICypherSchema schema) {
        List<IPropertyInfo> propertyInfos = new ArrayList<>();
        List<IPropertyInfo> result = new ArrayList<>();
        if (((NodeIdentifier) this.source).actualNode != null) {
            AbstractNode curNode = ((NodeIdentifier) this.source).actualNode;
            for (ILabelInfo labelInfo : curNode.getLabelInfos()) {
                for (IPropertyInfo propertyInfo : labelInfo.getProperties()) {
                    if (curNode.getProperties().containsKey(propertyInfo.getKey())) {
                        result.add(propertyInfo);
                    }
                }
            }
            return result;
        }
        for (ILabel label : getAllLabelsInDefChain()) {
            if (schema.containsLabel(label)) {
                ILabelInfo labelInfo = schema.getLabelInfo(label);
                propertyInfos.addAll(labelInfo.getProperties());
            }
        }
        return propertyInfos;
    }

    public List<IPropertyInfo> existedProperties() {
        List<IPropertyInfo> result = new ArrayList<>();
        if(((NodeIdentifier)this.source).actualNode!= null){
            AbstractNode curNode = ((NodeIdentifier)this.source).actualNode;
            for(ILabelInfo labelInfo : curNode.getLabelInfos())
            {
                for(IPropertyInfo propertyInfo : labelInfo.getProperties()){
                    if(curNode.getProperties().containsKey(propertyInfo.getKey())){
                        result.add(propertyInfo);
                    }
                }
            }
        }
        return result;
    }

    public INodeIdentifier getCopy()
    {
        return (INodeIdentifier) source.getCopy();
    }

    @Override
    public List<IPropertyInfo> getAllPropertiesWithType(ICypherSchema schema, ICypherType type) {
        return existedProperties().stream().filter(p->p.getType()==type).collect(Collectors.toList());
    }


}
