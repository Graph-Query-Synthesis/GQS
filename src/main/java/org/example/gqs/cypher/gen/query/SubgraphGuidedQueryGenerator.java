package org.example.gqs.cypher.gen.query;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.*;
import org.example.gqs.cypher.gen.alias.GuidedAliasGenerator;
import org.example.gqs.cypher.gen.condition.GuidedConditionGenerator;
import org.example.gqs.cypher.gen.list.GuidedListGenerator;
import org.example.gqs.cypher.gen.SubgraphManager;
import org.example.gqs.cypher.gen.pattern.GuidedPatternGenerator;
import org.example.gqs.cypher.schema.CypherSchema;

import java.util.*;

public class SubgraphGuidedQueryGenerator<S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>> extends ModelBasedQueryGenerator<S,G> {

    protected SubgraphManager subgraphManager;
    protected Map<String, Object> varToProperties = new HashMap<>();

    public SubgraphGuidedQueryGenerator(SubgraphManager subgraphManager){
        this.subgraphManager = subgraphManager;
    }

    private static final long maxSeedClauseLength = 8;
    private long numOfQueries = 0;
    @Override
    public IPatternGenerator createPatternGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new GuidedPatternGenerator<S>(globalState.getSchema(), varToProperties, subgraphManager, identifierBuilder, false);
    }

    @Override
    public IConditionGenerator createConditionGenerator(G globalState) {
        return new GuidedConditionGenerator<>(globalState.getSchema(), false, varToProperties);
    }

    @Override
    public IAliasGenerator createAliasGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new GuidedAliasGenerator<>(globalState.getSchema(), identifierBuilder, false, varToProperties);
    }

    @Override
    public IListGenerator createListGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new GuidedListGenerator<>(globalState.getSchema(), identifierBuilder, false, varToProperties);
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
        throw new RuntimeException();
    }

    @Override
    public void addExecutionRecord(IClauseSequence clauseSequence, boolean isBugDetected, long resultSize) {
        return;
    }

    @Override
    public void addNewRecord(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo) {
        return;
    }
}
