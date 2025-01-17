package org.example.gqs.cypher.gen.query;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.*;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;

import java.util.Arrays;
import java.util.List;

public abstract class ModelBasedQueryGenerator<S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>> implements IQueryGenerator<S, G> {


    public IClauseSequenceBuilder generateClauses(IClauseSequenceBuilder seq, long len, List<String> generateClause) {
        if (len == 0) {
            return seq;
        }
        Randomly r = new Randomly();
        String generate = generateClause.get(r.getInteger(0, generateClause.size()));
        if (generate == "MATCH") {
            if (MainOptions.mode == "thinker" || MainOptions.mode == "kuzu")
                return generateClauses(seq.MatchClause(), len - 1, Arrays.asList("MATCH", "WITH", "WITH", "WITH", "UNWIND"));
            else if (MainOptions.mode == "falkordb" && MainOptions.exp == "ablation")
                return generateClauses(seq.MatchClause(), len - 1, Arrays.asList("MATCH", "WITH", "WITH", "WITH"));
            else
                return generateClauses(seq.MatchClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND", "WITH", "UNWIND", "WITH", "UNWIND"));
        } else if (generate == "OPTIONAL MATCH") {
            if (MainOptions.mode == "memgraph" || MainOptions.mode == "falkordb")
                return generateClauses(seq.OptionalMatchClause(), len - 1, Arrays.asList("WITH", "OPTIONAL MATCH", "WITH", "WITH"));
            else if (MainOptions.mode == "thinker")
                return generateClauses(seq.OptionalMatchClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "WITH"));
            else
                return generateClauses(seq.OptionalMatchClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND"));
        } else if (generate == "WITH") {
            if (MainOptions.mode == "thinker")
                return generateClauses(seq.WithClause(), len - 1, Arrays.asList("MATCH", "WITH", "WITH", "WITH"));
            else if (MainOptions.mode == "kuzu")
                return generateClauses(seq.WithClause(), len - 1, Arrays.asList("MATCH", "WITH", "WITH", "WITH", "UNWIND", "UNWIND"));
            else if (MainOptions.mode == "falkordb" && MainOptions.exp == "ablation")
                return generateClauses(seq.WithClause(), len - 1, Arrays.asList("MATCH", "WITH", "WITH", "WITH"));
            else
                return generateClauses(seq.WithClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND", "WITH", "UNWIND", "WITH", "UNWIND"));
        } else {
            if (MainOptions.mode == "thinker")
                return generateClauses(seq.MatchClause(), len - 1, Arrays.asList("MATCH", "WITH", "WITH", "WITH"));
            else if (MainOptions.mode == "kuzu")
                return generateClauses(seq.MatchClause(), len - 1, Arrays.asList("MATCH", "WITH", "WITH", "WITH", "UNWIND"));
            else
                return generateClauses(seq.UnwindClause(), len - 1, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND", "WITH", "UNWIND", "WITH", "UNWIND"));
        }
    }

    public IClauseSequenceBuilder generateUnwindClauses(IClauseSequenceBuilder seq, long len, List<String> generateClause) {
        if (len == 0) {
            return seq;
        }
        Randomly r = new Randomly();
        String generate = generateClause.get(r.getInteger(0, generateClause.size()));
        return generateUnwindClauses(seq.UnwindClause(), len - 1, Arrays.asList("UNWIND"));
    }

    public abstract IPatternGenerator createPatternGenerator(G globalState, IIdentifierBuilder identifierBuilder);
    public abstract IConditionGenerator createConditionGenerator(G globalState);
    public abstract IAliasGenerator createAliasGenerator(G globalState, IIdentifierBuilder identifierBuilder);
    public abstract IListGenerator createListGenerator(G globalState, IIdentifierBuilder identifierBuilder);

    public abstract boolean shouldDoMutation(G globalState);

    public abstract IClauseSequence doMutation(G globalState);

    protected void beforeGeneration(G globalState){

    }

    protected IClauseSequence postProcessing(G globalState, IClauseSequence clauseSequence){
        return clauseSequence;
    }


    public IClauseSequence generateQuery(G globalState){
        S schema = globalState.getSchema();
        Randomly r = new Randomly();
        IClauseSequence sequence = null;

        if (!shouldDoMutation(globalState)) {
            IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder();
            long numOfClauses = r.getInteger(1, globalState.getOptions().getMaxClauseSize());
            if (MainOptions.mode == "thinker")
                sequence = generateClauses(builder.MatchClause(), numOfClauses, Arrays.asList("MATCH", "WITH")).ReturnClause().build();
            else if ((MainOptions.mode == "falkordb" && MainOptions.exp != "ablation") || MainOptions.mode == "kuzu")
                sequence = generateClauses(builder.MatchClause(), numOfClauses, Arrays.asList("MATCH", "WITH", "UNWIND")).ReturnClause().build();
            else if (MainOptions.mode == "falkordb" && MainOptions.exp == "ablation")
                sequence = generateClauses(builder.MatchClause(), numOfClauses, Arrays.asList("MATCH", "WITH")).ReturnClause().build();
            else
                sequence = generateClauses(builder.MatchClause(), numOfClauses, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND")).ReturnClause().build();
            new QueryFiller<S>(sequence,
                    createPatternGenerator(globalState, builder.getIdentifierBuilder()),
                    createConditionGenerator(globalState),
                    createAliasGenerator(globalState, builder.getIdentifierBuilder()),
                    createListGenerator(globalState, builder.getIdentifierBuilder()),
                    schema, builder.getIdentifierBuilder()).startVisit();
        } else {
            sequence = doMutation(globalState);
        }
        return postProcessing(globalState, sequence);
    }


}
