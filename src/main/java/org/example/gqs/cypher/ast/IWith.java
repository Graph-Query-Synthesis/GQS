package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;

import java.util.List;

public interface IWith extends ICypherClause{
    boolean isDistinct();
    void setDistinct(boolean isDistinct);
    List<IRet> getReturnList();
    void setReturnList(List<IRet> returnList);
    IExpression getCondition();
    void setCondition(IExpression condtion);

    void setOrderBy(List<IExpression> expression, boolean isDesc);
    void setOrderBy(List<IExpression> expression, List<Boolean> isDesc);
    List<IExpression> getOrderByExpressions();
    List<Boolean> isOrderByDesc();

    void setLimit(IExpression expression);
    IExpression getLimit();

    void setSkip(IExpression expression);
    IExpression getSkip();


    @Override
    IWithAnalyzer toAnalyzer();
}
