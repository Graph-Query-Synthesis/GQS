package org.example.gqs.cypher.gen.query;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.dsl.QueryFiller;
import org.example.gqs.cypher.gen.list.RandomListGenerator;
import org.example.gqs.cypher.gen.alias.RandomAliasGenerator;
import org.example.gqs.cypher.gen.condition.RandomConditionGenerator;
import org.example.gqs.cypher.gen.pattern.RandomPatternGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;
import org.example.gqs.cypher.mutation.MutatorType;

import java.util.*;

public class AdvancedQueryGenerator<S extends CypherSchema<G, ?>, G extends CypherGlobalState<?, S>> implements IQueryGenerator<S, G> {
    protected static final long maxSeedClauseLength = 8;
    protected static final long mutationProb = 90;

    public static class Seed {
        IClauseSequence sequence;
        boolean bugDetected;
        long resultLength;
        long selectedTimes;
        long nonEmptyTimes;

        public Seed(IClauseSequence sequence, boolean bugDetected, long resultLength) {
            this.sequence = sequence;
            this.bugDetected = bugDetected;
            this.resultLength = resultLength;
            this.selectedTimes = 0;
            this.nonEmptyTimes = 0;
        }

        public long getWeight() {
            if (selectedTimes > 0) {
                return nonEmptyTimes * 100000 / selectedTimes;
            }
            return 100000;
        }
    }

    protected List<Seed> seeds = new ArrayList<>();
    protected long numOfQueries = 0;

    protected static class MutatorUsageInfo {
        protected long selectedTimes;
        protected long nonEmptyTimes;

        public MutatorUsageInfo() {
            selectedTimes = 0;
            nonEmptyTimes = 0;
        }

        public void incSelectedTimes() {
            selectedTimes++;
        }

        public void incNonEmptyTimes() {
            nonEmptyTimes++;
        }

        public long getWeight() {
            if (selectedTimes > 0) {
                return nonEmptyTimes * 100000 / selectedTimes;
            }
            return 100000;
        }
    }

    protected Map<MutatorType, MutatorUsageInfo> mutatorUsedTimes = new HashMap<>();
    protected MutatorType lastStrategy = null;
    protected Seed lastSelectedSeed = null;

    protected IClauseSequenceBuilder generateClauses(IClauseSequenceBuilder seq, long len, List<String> generateClause) {
        if (len == 0) {
            return seq;
        }
        Randomly r = new Randomly();
        String generate = generateClause.get(r.getInteger(0, generateClause.size()));
        if (generate == "MATCH") {
            return generateClauses(seq.MatchClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND"));
        } else if (generate == "OPTIONAL MATCH") {
            return generateClauses(seq.OptionalMatchClause(), len - 1, Arrays.asList("OPTIONAL MATCH", "WITH"));
        } else if (generate == "WITH") {
            return generateClauses(seq.WithClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND"));
        } else {
            return generateClauses(seq.UnwindClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND"));
        }
    }


    @Override
    public void addExecutionRecord(IClauseSequence clauseSequence, boolean isBugDetected, long resultSize) {
        if (clauseSequence.getClauseList().size() <= maxSeedClauseLength) {
            seeds.add(new Seed(clauseSequence, isBugDetected, resultSize));
        }
        if (resultSize > 0 && lastStrategy != null) {
            mutatorUsedTimes.get(lastStrategy).nonEmptyTimes++;
        }
        if (resultSize > 0 && lastSelectedSeed != null) {
            lastSelectedSeed.nonEmptyTimes++;
        }
    }

    @Override
    public void addNewRecord(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo) {

    }

    protected IClauseSequence selectSeed() {
        Randomly r = new Randomly();
        long totalWeight = 0;
        for (Seed seed : seeds) {
            totalWeight += seed.getWeight();
        }

        long randomNum = r.getInteger(0, totalWeight);

        Seed selectedSeed = seeds.get(0);

        for (Seed seed : seeds) {
            randomNum -= seed.getWeight();
            if (randomNum < 0) {
                selectedSeed = seed;
                break;
            }
        }

        selectedSeed.selectedTimes++;
        lastSelectedSeed = selectedSeed;
        return selectedSeed.sequence;
    }


    protected IClauseSequence mutate(G globalState, IClauseSequence seedSeq) {
        Randomly r = new Randomly();

        for (MutatorType type : MutatorType.values()) {
            if (!mutatorUsedTimes.containsKey(type)) {
                mutatorUsedTimes.put(type, new MutatorUsageInfo());
            }
        }

        long totalWeight = 0;

        for (MutatorType type : MutatorType.values()) {
            totalWeight += mutatorUsedTimes.get(type).getWeight();
        }

        long randomNum = r.getInteger(0, totalWeight);

        MutatorType selectedType = MutatorType.CLAUSE_REFILL;

        for (MutatorType type : MutatorType.values()) {
            randomNum -= mutatorUsedTimes.get(type).getWeight();
            if (randomNum < 0) {
                selectedType = type;
                break;
            }
        }

        MutatorType.mutate(selectedType, globalState, seedSeq);
        mutatorUsedTimes.get(selectedType).incSelectedTimes();
        lastStrategy = selectedType;

        return seedSeq;

    }

    public IClauseSequence generateQuery(G globalState) {
        S schema = globalState.getSchema();
        Randomly r = new Randomly();
        IClauseSequence sequence = null;

        if (r.getInteger(0, 100) > mutationProb || seeds.size() == 0) {
            IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder();
            long numOfClauses = r.getInteger(1, 6);
            sequence = generateClauses(builder.MatchClause(), numOfClauses, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND")).ReturnClause().build();
            new QueryFiller<S>(sequence,
                    new RandomPatternGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomConditionGenerator<>(schema, false),
                    new RandomAliasGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomListGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    schema, builder.getIdentifierBuilder()).startVisit();
            lastStrategy = null;
            lastSelectedSeed = null;
        } else {
            IClauseSequence seedSeq = selectSeed();
            sequence = mutate(globalState, seedSeq.getCopy());
        }
        numOfQueries++;
        System.out.println(numOfQueries);
        return sequence;
    }
}
