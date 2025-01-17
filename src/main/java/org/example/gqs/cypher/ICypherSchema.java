package org.example.gqs.cypher;

import org.example.gqs.cypher.ast.ILabel;
import org.example.gqs.cypher.ast.IType;
import org.example.gqs.cypher.schema.IFunctionInfo;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.schema.IRelationTypeInfo;

import java.util.List;

public interface ICypherSchema {
    boolean containsLabel(ILabel label);
    ILabelInfo getLabelInfo(ILabel label);
    boolean containsRelationType(IType relation);
    IRelationTypeInfo getRelationInfo(IType relation);
    List<IFunctionInfo> getFunctions();

    List<ILabelInfo> getLabelInfos();
    List<IRelationTypeInfo> getRelationshipTypeInfos();
}
