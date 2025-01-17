package org.example.gqs.cypher.ast.analyzer;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.ILabel;
import org.example.gqs.cypher.ast.INodeIdentifier;
import org.example.gqs.cypher.ast.IProperty;
import org.example.gqs.cypher.schema.IPropertyInfo;

import java.util.List;

public interface INodeAnalyzer extends INodeIdentifier, IIdentifierAnalyzer {
    @Override
    INodeIdentifier getSource();
    @Override
    INodeAnalyzer getFormerDef();
    void setFormerDef(INodeAnalyzer formerDef);

    List<ILabel> getAllLabelsInDefChain();

    List<IProperty> getAllPropertiesInDefChain();
    List<IPropertyInfo> getAllPropertiesAvailable(ICypherSchema schema);
    List<IPropertyInfo> getAllPropertiesWithType(ICypherSchema schema, ICypherType type);
}
