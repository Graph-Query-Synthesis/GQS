package org.example.gqs.cypher;

public interface ICypherSupport {

    boolean supportMatch();
    boolean supportUnwind();
    boolean supportInQueryCall();

    boolean supportWith();

    boolean supportCreate();
    boolean supportMerge();
    boolean supportSet();
    boolean supportDelete();
    boolean supportRemove();

    boolean supportOr();
    boolean supportXor();
    boolean supportAnd();
    boolean supportNot();
    boolean supportComparison();
    boolean supportAddOrSub();
    boolean supportMulOrDiv();
    boolean supportPowerOf();
    boolean supportUnaryAddOrSub();
    boolean supportCheckNull();
    boolean supportListIn();
    boolean supportListGetByIndex();
    boolean supportListGetBySlice();
    boolean supportStringStartsWith();
    boolean supportStringEndsWith();
    boolean supportStringContains();

    boolean supportNumber();
    boolean supportString();
    boolean supportList();
    boolean supportMap();

    boolean supportMatchWhere();
    boolean supportOptionalMatch();

    boolean supportWithDistinct();
    boolean supportWithOrderBy();
    boolean supportWithLimit();
    boolean supportWithSkip();
    boolean supportWithWhere();

    boolean supportReturnDistinct();
    boolean supportReturnOrderBy();
    boolean supportReturnLimit();
    boolean supportReturnSkip();

}
