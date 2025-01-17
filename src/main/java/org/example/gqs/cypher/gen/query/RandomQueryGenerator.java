package org.example.gqs.cypher.gen.query;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.*;
import org.example.gqs.cypher.gen.alias.RandomAliasGenerator;
import org.example.gqs.cypher.gen.condition.RandomConditionGenerator;
import org.example.gqs.cypher.gen.list.RandomListGenerator;
import org.example.gqs.cypher.gen.pattern.RandomPatternGenerator;
import org.example.gqs.cypher.mutation.ClauseScissorsMutator;
import org.example.gqs.cypher.mutation.WhereRemovalMutator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RandomQueryGenerator<S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>> extends ModelBasedQueryGenerator<S,G> {

    private static final long maxSeedClauseLength = 8;
    private long numOfQueries = 0;

    public static class Seed{
        IClauseSequence sequence;
        boolean bugDetected;
        long resultLength;

        public Seed(IClauseSequence sequence, boolean bugDetected, long resultLength){
            this.sequence = sequence;
            this.bugDetected = bugDetected;
            this.resultLength = resultLength;
        }
    }

    protected List<Seed> seeds = new ArrayList<>();
    @Override
    public IPatternGenerator createPatternGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new RandomPatternGenerator<S>(globalState.getSchema(), identifierBuilder, false);
    }

    @Override
    public IConditionGenerator createConditionGenerator(G globalState) {
        return new RandomConditionGenerator<S>(globalState.getSchema(), false);
    }

    @Override
    public IAliasGenerator createAliasGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new RandomAliasGenerator<>(globalState.getSchema(), identifierBuilder, false);
    }

    @Override
    public IListGenerator createListGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new RandomListGenerator<>(globalState.getSchema(), identifierBuilder, false);
    }

    @Override
    public boolean shouldDoMutation(G globalState) {
        return numOfQueries >= globalState.getOptions().getNrQueries();
    }

    @Override
    protected IClauseSequence postProcessing(G globalState, IClauseSequence clauseSequence) {
        numOfQueries++;
        return clauseSequence;
    }

    @Override
    public IClauseSequence doMutation(G globalState) {
        Randomly r = new Randomly();
        IClauseSequence sequence = null;
        S schema = globalState.getSchema();
        IClauseSequence seedSeq = seeds.get(r.getInteger(0, seeds.size())).sequence;
        long kind = r.getInteger(1, 3);
        if (kind == 1) {
            IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder(seedSeq);
            long numOfClauses = Randomly.smallNumber();
            sequence = generateClauses(builder.WithClause(), numOfClauses, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND")).ReturnClause().build();
            new QueryFiller<S>(sequence,
                    new RandomPatternGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomConditionGenerator<>(schema, false),
                    new RandomAliasGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomListGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    schema, builder.getIdentifierBuilder()).startVisit();
        } else if (kind == 2) {
            WhereRemovalMutator mutator = new WhereRemovalMutator<>(seedSeq);
            mutator.mutate();
            sequence = mutator.getClauseSequence();
        } else if (kind == 3) {
            ClauseScissorsMutator mutator = new ClauseScissorsMutator(seedSeq);
            mutator.mutate();
            sequence = mutator.getClauseSequence();
        }
        return sequence;
    }

    @Override
    public void addExecutionRecord(IClauseSequence clauseSequence, boolean isBugDetected, long resultSize) {
        if (clauseSequence.getClauseList().size() <= maxSeedClauseLength) {
            seeds.add(new RandomQueryGenerator.Seed(clauseSequence, isBugDetected, resultSize));
        }
    }

    @Override
    public void addNewRecord(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo) {
        return;
    }
}
