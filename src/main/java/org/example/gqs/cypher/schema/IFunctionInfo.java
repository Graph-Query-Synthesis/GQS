package org.example.gqs.cypher.schema;

import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.standard_ast.CypherType;

import java.util.List;

public interface IFunctionInfo {
    String getName();
    String getSignature();
    List<IParamInfo> getParams();
    CypherType getExpectedReturnType();
    ICypherTypeDescriptor calculateReturnType(List<IExpression> params);
}
