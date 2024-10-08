package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ast.ICypherType;

public interface ICypherTypeDescriptor {
    ICypherType getType();
    boolean isBasicType();
    boolean isNodeOrRelation();
    boolean isNode();
    boolean isRelation();
    boolean isList();
    boolean isMap();
    IListDescriptor getListDescriptor();
    IMapDescriptor getMapDescriptor();
    INodeAnalyzer getNodeAnalyzer();
    IRelationAnalyzer getRelationAnalyzer();
}
