package org.example.gqs.cypher.gen.query;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.*;
import org.example.gqs.cypher.gen.EnumerationSeq;
import org.example.gqs.cypher.gen.alias.EnumerationAliasGenerator;
import org.example.gqs.cypher.gen.condition.EnumerationConditionGenerator;
import org.example.gqs.cypher.gen.list.EnumerationListGenerator;
import org.example.gqs.cypher.gen.pattern.EnumerationPatternGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnumerationQueryGenerator <S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>> implements IQueryGenerator<S, G> {

    private static long MAX_CLAUSE_NUM = 3;
    private EnumerationSeq enumerationSeq;

    public EnumerationQueryGenerator(EnumerationSeq enumerationSeq){
        this.enumerationSeq = enumerationSeq;
    }

    public IClauseSequenceBuilder generateClauses(IClauseSequenceBuilder seq, Clause prev) {

        List<Clause> possibles = new ArrayList<>();

        if(seq.getLength() >= MAX_CLAUSE_NUM){
            return seq;
        }


        switch (prev){
            case EMPTY:
                possibles = Arrays.asList(Clause.MATCH, Clause.OPTIONAL_MATCH, Clause.UNWIND, Clause.WITH);
                break;
            case WITH:
                possibles = Arrays.asList(Clause.MATCH, Clause.OPTIONAL_MATCH, Clause.UNWIND, Clause.WITH, Clause.OVER);
                break;
            case MATCH:
                possibles = Arrays.asList(Clause.MATCH, Clause.OPTIONAL_MATCH, Clause.UNWIND, Clause.WITH, Clause.OVER);
                break;
            case UNWIND:
                possibles = Arrays.asList(Clause.MATCH, Clause.OPTIONAL_MATCH, Clause.UNWIND, Clause.WITH, Clause.OVER);
                break;
            case OPTIONAL_MATCH:
                possibles = Arrays.asList(Clause.MATCH, Clause.OPTIONAL_MATCH, Clause.UNWIND, Clause.WITH, Clause.OVER);
                break;
        }

        Clause present = possibles.get((int)enumerationSeq.getRange(possibles.size()));
        switch (present){
            case MATCH:
                return generateClauses(seq.MatchClause(), present);
            case OPTIONAL_MATCH:
                return generateClauses(seq.OptionalMatchClause(), present);
            case UNWIND:
                return generateClauses(seq.UnwindClause(), present);
            case WITH:
                return generateClauses(seq.WithClause(), present);
            case OVER:
                return seq;
            default:
                throw new RuntimeException();
        }
    }

    public IPatternGenerator createPatternGenerator(G globalState, IIdentifierBuilder identifierBuilder){
        return new EnumerationPatternGenerator<>(globalState.getSchema(), identifierBuilder, enumerationSeq);
    }
    public IConditionGenerator createConditionGenerator(G globalState){
        return new EnumerationConditionGenerator<>(globalState.getSchema(), enumerationSeq);
    }
    public IAliasGenerator createAliasGenerator(G globalState, IIdentifierBuilder identifierBuilder){
        return new EnumerationAliasGenerator<>(globalState.getSchema(), identifierBuilder, enumerationSeq);
    }
    public IListGenerator createListGenerator(G globalState, IIdentifierBuilder identifierBuilder){
        return new EnumerationListGenerator<>(globalState.getSchema(), identifierBuilder, enumerationSeq);
    }


    protected IClauseSequence postProcessing(G globalState, IClauseSequence clauseSequence){
        return clauseSequence;
    }

    private enum Clause{
        MATCH, OPTIONAL_MATCH, WITH, UNWIND, EMPTY, OVER
    }


    public IClauseSequence generateQuery(G globalState){
        S schema = globalState.getSchema();
        IClauseSequence sequence = null;

        IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder();
        sequence = generateClauses(builder.MatchClause(), Clause.EMPTY).ReturnClause().build();
        new QueryFiller<S>(sequence,
                createPatternGenerator(globalState, builder.getIdentifierBuilder()),
                createConditionGenerator(globalState),
                createAliasGenerator(globalState, builder.getIdentifierBuilder()),
                createListGenerator(globalState, builder.getIdentifierBuilder()),
                schema, builder.getIdentifierBuilder()).startVisit();

        return postProcessing(globalState, sequence);
    }

    @Override
    public void addExecutionRecord(IClauseSequence clauseSequence, boolean isBugDetected, long resultSize) {

    }

    @Override
    public void addNewRecord(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo) {

    }
}
