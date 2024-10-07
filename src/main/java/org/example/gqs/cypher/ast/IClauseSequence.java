package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.dsl.IIdentifierBuilder;

import java.util.List;

public interface IClauseSequence extends ITextRepresentation, ICopyable {
    List<ICypherClause> getClauseList();
    IIdentifierBuilder getIdentifierBuilder();
    void setClauseList(List<ICypherClause> clauses);

    void addClause(ICypherClause clause);
    void addClauseAt(ICypherClause clause, long index);

    @Override
    IClauseSequence getCopy();

}
