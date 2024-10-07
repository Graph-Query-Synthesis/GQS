package org.example.gqs.cypher.gen;

import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.ILabel;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.standard_ast.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AbstractNode implements IIdentifier {
    private List<ILabelInfo> labelInfos = new ArrayList<>();
    private long id;

    private Map<String, Object> properties = new HashMap<>();

    private List<AbstractRelationship> relationships = new ArrayList<>();

    public int getId() {
        return (int) id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public List<ILabelInfo> getLabelInfos() {
        return labelInfos;
    }

    public List<ILabel> getLabels(){
        return new ArrayList<>(labelInfos.stream().map(l->new Label(l.getName())).collect(Collectors.toList()));
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setLabelInfos(List<ILabelInfo> labelInfos) {
        this.labelInfos = labelInfos;
    }

    public void addRelationship(AbstractRelationship relationship){
        this.relationships.add(relationship);
    }

    public List<AbstractRelationship> getRelationships(){
        return relationships;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public ICypherType getType() {
        return null;
    }

    @Override
    public boolean equals(IIdentifier i2) {
        if(i2 instanceof AbstractNode){
            AbstractNode other = (AbstractNode) i2;
            if(other.getId() == this.getId())
                return true;
        }
        return false;
    }

    @Override
    public AbstractNode getCopy() {
        AbstractNode copy = new AbstractNode();
        copy.setId(id);
        copy.setLabelInfos(new ArrayList<>(labelInfos));
        copy.setProperties(new HashMap<>(properties));
        copy.setRelationships(new ArrayList<>(relationships));
        return copy;
    }

    private void setRelationships(ArrayList<AbstractRelationship> abstractRelationships) {
        this.relationships = abstractRelationships;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {

    }
    public boolean equals(Object o){
        if(o instanceof AbstractNode){
            AbstractNode other = (AbstractNode) o;
            if(other.getId() == this.getId() && other.getLabelInfos().equals(this.getLabelInfos()) && other.getProperties().equals(this.getProperties()) && other.getRelationships().equals(this.getRelationships()))
                return true;
        }
        return false;
    }
}
