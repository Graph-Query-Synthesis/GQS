package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.IProperty;
import org.example.gqs.cypher.ast.IRelationIdentifier;
import org.example.gqs.cypher.ast.IType;
import org.example.gqs.cypher.schema.IPropertyInfo;

import java.util.List;

public interface IRelationAnalyzer extends IRelationIdentifier, IIdentifierAnalyzer {
    @Override
    IRelationIdentifier getSource();
    @Override
    IRelationAnalyzer getFormerDef();
    void setFormerDef(IRelationAnalyzer formerDef);

    List<IType> getAllRelationTypesInDefChain();

    List<IProperty> getAllPropertiesInDefChain();
    List<IPropertyInfo> getAllPropertiesAvailable(ICypherSchema schema);
    List<IPropertyInfo> getAllPropertiesWithType(ICypherSchema schema, ICypherType type);

    boolean isSingleRelation();
}
