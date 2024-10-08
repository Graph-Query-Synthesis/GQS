package org.example.gqs.cypher.ast;

import org.example.gqs.cypher.ast.analyzer.IClauseAnalyzer;

public interface ICypherClause extends ITextRepresentation, ICopyable{
    void setNextClause(ICypherClause next);
    ICypherClause getNextClause();
    void setPrevClause(ICypherClause prev);
    ICypherClause getPrevClause();
    IClauseAnalyzer toAnalyzer();

    @Override
    ICypherClause getCopy();

}
