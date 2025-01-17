package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.ILabel;
import org.example.gqs.cypher.ast.INodeIdentifier;
import org.example.gqs.cypher.ast.IProperty;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NodeIdentifier implements INodeIdentifier {
    protected String name;
    public List<ILabel> labels;
    protected List<IProperty> properties;
    public AbstractNode actualNode;

    public AbstractNode getActualNode() {
        return actualNode;
    }


    public static NodeIdentifier createNodeRef(INodeIdentifier nodeIdentifier){
        if(nodeIdentifier instanceof NodeAnalyzer)
            return new NodeIdentifier(nodeIdentifier.getName(), new ArrayList<>(), new ArrayList<>(), ((NodeIdentifier) ((NodeAnalyzer) nodeIdentifier).getSource()).actualNode);
        else
            return new NodeIdentifier(nodeIdentifier.getName(), new ArrayList<>(), new ArrayList<>(), ((NodeIdentifier) nodeIdentifier).actualNode);
    }

    public static NodeIdentifier createNewNamedNode(IIdentifierBuilder identifierBuilder, List<ILabel> labels, List<IProperty> properties){
        return new NodeIdentifier(identifierBuilder.getNewNodeName(), labels, properties);
    }

    public static NodeIdentifier createNewAnonymousNode(List<ILabel> labels, List<IProperty> properties){
        return new NodeIdentifier("", labels, properties);
    }

    NodeIdentifier(String name, List<ILabel> labels, List<IProperty> properties){
        this.name = name;
        this.labels = labels;
        this.properties = properties;
    }

    NodeIdentifier(String name, List<ILabel> labels, List<IProperty> properties, AbstractNode actualNode){
        this.name = name;
        this.labels = labels;
        this.properties = properties;
        this.actualNode = actualNode;
    }

    public NodeIdentifier(String name, AbstractNode actualNode)
    {
        this.name = name;
        this.actualNode = actualNode;
        this.labels = actualNode.getLabels();
        Map<String, Object> originalProperties = actualNode.getProperties();
        this.properties = new ArrayList<>();
        for (Map.Entry<String, Object> entry : originalProperties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            IProperty property = null;
            ConstExpression constExpression = new ConstExpression(value);
            property = new Property(key, (CypherType) constExpression.getType(), constExpression);
            this.properties.add(property);
        }
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public ICypherType getType() {
        return CypherType.NODE;
    }

    @Override
    public boolean equals(IIdentifier i2) {
        if(i2 instanceof NodeIdentifier){
            NodeIdentifier node = (NodeIdentifier) i2;
            if(node.getActualNode().equals(this.getActualNode())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public INodeIdentifier getCopy() {
        NodeIdentifier node = new NodeIdentifier(name, new ArrayList<>(), new ArrayList<>());
        if(labels != null){
            node.labels.addAll(labels);
        }
        if(properties != null){
            node.properties = properties.stream().map(p->p.getCopy()).collect(Collectors.toList());
        }
        if(actualNode != null)
            node.actualNode = actualNode;
        else
            System.out.println("error: actualNode is null");
        return node;
    }

    @Override
    public List<IProperty> getProperties() {
        return properties;
    }

    @Override
    public List<ILabel> getLabels() {
        return labels;
    }

    @Override
    public void setProperties(List<IProperty> properties) {
        this.properties = properties;
    }


    @Override
    public INodeIdentifier createRef() {
        return new NodeIdentifier(this.name, null, null);
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("(");
        if (name != null) {
            sb.append(name);
        }
        {
            if (labels != null) {
                for (ILabel label : labels) {
                    if (label.getName() != null && label.getName().length() != 0) {
                        sb.append(" :").append(label.getName());
                    }
                }
            }
        }
        if (properties != null && properties.size() != 0) {
            sb.append("{");
            for (int i = 0; i < properties.size(); i++) {
                properties.get(i).toTextRepresentation(sb);
                if (i != properties.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
        }
        sb.append(")");
    }

    @Override
    public boolean isAnonymous() {
        return getName() == null || getName().length() == 0;
    }

    @Override
    public int hashCode(){
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof NodeIdentifier)){
            return false;
        }
        if(getName().equals(((NodeIdentifier)o).getName()) && getLabels().equals(((NodeIdentifier)o).getLabels()) && getProperties().equals(((NodeIdentifier)o).getProperties())){
            return true;
        }
        return false;
    }

    public static class NodeBuilder {
        private String curNodeName = "";
        private List<ILabel> curNodeLabels;
        private List<IProperty> curNodeProperties;

        static NodeBuilder newNodeBuilder(String name){
            return new NodeBuilder(name);
        }

        public static NodeBuilder newNodeBuilder(IIdentifierBuilder identifierBuilder){
            return new NodeBuilder(identifierBuilder.getNewNodeName());
        }

        private NodeBuilder(String name){
            curNodeLabels = new ArrayList<>();
            curNodeProperties = new ArrayList<>();
            this.curNodeName = name;
        }


        public NodeBuilder withLabels(ILabel ...labels){
            curNodeLabels.addAll(Arrays.asList(labels));
            curNodeLabels = curNodeLabels.stream().distinct().collect(Collectors.toList());
            return this;
        }

        public NodeBuilder withProperties(IProperty ...properties){
            curNodeProperties.addAll(Arrays.asList(properties));
            curNodeProperties = curNodeProperties.stream().distinct().collect(Collectors.toList());
            return this;
        }


        public INodeIdentifier build(){
            return new NodeIdentifier(curNodeName, curNodeLabels, curNodeProperties);
        }
    }
}
