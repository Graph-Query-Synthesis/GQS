package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.schema.CypherSchema;

public interface IQueryGenerator <S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>>{
    IClauseSequence generateQuery(G globalState);
    void addExecutionRecord(IClauseSequence clauseSequence, boolean isBugDetected, long resultSize);

    void addNewRecord(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo);

}
