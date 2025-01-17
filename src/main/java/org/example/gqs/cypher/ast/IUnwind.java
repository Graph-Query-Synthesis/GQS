package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.ast.analyzer.IUnwindAnalyzer;

public interface IUnwind extends ICypherClause{
    IRet getListAsAliasRet();
    void setListAsAliasRet(IRet listAsAlias);

    @Override
    IUnwindAnalyzer toAnalyzer();
}
