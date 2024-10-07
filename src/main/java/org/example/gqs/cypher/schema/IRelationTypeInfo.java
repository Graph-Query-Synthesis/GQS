package org.example.gqs.cypher.schema;

import org.example.gqs.cypher.ast.ICypherType;

import java.util.List;

public interface IRelationTypeInfo extends IPatternElementInfo{
    String getName();
    List<IPropertyInfo> getProperties();
    boolean hasPropertyWithType(ICypherType type);
    List<IPropertyInfo> getPropertiesWithType(ICypherType type);
}
