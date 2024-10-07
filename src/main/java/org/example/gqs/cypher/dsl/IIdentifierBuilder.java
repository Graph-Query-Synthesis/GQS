package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.ICopyable;

public interface IIdentifierBuilder extends ICopyable {
    String getNewNodeName();

    String getNewRelationName();

    String getNewAliasName();

    @Override
    IIdentifierBuilder getCopy();
}
