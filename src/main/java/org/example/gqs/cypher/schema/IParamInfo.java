package org.example.gqs.cypher.schema;

import org.example.gqs.cypher.standard_ast.CypherType;

public interface IParamInfo {
    boolean isOptionalLength();
    CypherType getParamType();
}
