package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.ast.analyzer.IReturnAnalyzer;

import java.util.List;

public interface IReturn extends ICypherClause{
    List<IRet> getReturnList();
    void setReturnList(List<IRet> returnList);


    void setDistinct(boolean distinct);
    boolean isDistinct();

    void setOrderBy(List<IExpression> expression, boolean isDesc);
    void setOrderBy(List<IExpression> expression, List<Boolean> isDesc);
    List<IExpression> getOrderByExpressions();
    List<Boolean> isOrderByDesc();


    void setLimit(IExpression expression);
    IExpression getLimit();

    void setSkip(IExpression expression);
    IExpression getSkip();

    @Override
    IReturnAnalyzer toAnalyzer();
}
