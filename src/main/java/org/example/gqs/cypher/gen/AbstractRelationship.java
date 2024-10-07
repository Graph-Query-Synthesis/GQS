package org.example.gqs.cypher.gen;

import org.example.gqs.cypher.schema.IRelationTypeInfo;

import java.util.HashMap;
import java.util.Map;

public class AbstractRelationship {
    private IRelationTypeInfo type = null;
    private AbstractNode from;
    private AbstractNode to;

    private long id;
    public Map<String, Object> properties = new HashMap<>();

    public IRelationTypeInfo getType() {
        return type;
    }
    public AbstractRelationship getCopy()
    {
        AbstractRelationship copy = new AbstractRelationship();
        copy.setType(getType());
        copy.setFrom(getFrom().getCopy());
        copy.setTo(getTo().getCopy());
        copy.setId(getId());
        copy.setProperties(new HashMap<>(getProperties()));
        return copy;
    }

    public void setType(IRelationTypeInfo type) {
        this.type = type;
    }

    public AbstractNode getFrom() {
        return from;
    }

    public void setFrom(AbstractNode from) {
        this.from = from;
    }

    public AbstractNode getTo() {
        return to;
    }

    public void setTo(AbstractNode to) {
        this.to = to;
    }

    public int getId() {
        return (int) id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    public boolean equals(Object o)
    {
        if(o instanceof AbstractRelationship)
        {
            AbstractRelationship other = (AbstractRelationship)o;
            return other.getFrom().equals(getFrom()) && other.getTo().equals(getTo()) && other.getType().equals(getType()) && other.getId() == getId();
        }
        return false;
    }
}
