package org.example.gqs.cypher.mutation;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.ast.ICypherClause;
import org.example.gqs.cypher.ast.IMatch;
import org.example.gqs.cypher.ast.IWith;
import org.example.gqs.cypher.dsl.ClauseVisitor;
import org.example.gqs.cypher.dsl.IContext;
import org.example.gqs.cypher.gen.condition.RandomConditionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.mutation.WhereRefillMutator.WhereRefillMutatorContext;

import java.util.ArrayList;
import java.util.List;

public class WhereRefillMutator<S extends CypherSchema<?,?>> extends ClauseVisitor<WhereRefillMutatorContext> implements IClauseMutator  {

    public List<ICypherClause> matchOrWithList = new ArrayList<>();
    public S schema;

    public WhereRefillMutator(IClauseSequence clauseSequence, S schema) {
        super(clauseSequence, new WhereRefillMutatorContext());
        this.schema = schema;
    }

    @Override
    public void mutate() {
        startVisit();
    }

    public static class WhereRefillMutatorContext implements IContext {

    }

    @Override
    public void visitMatch(IMatch matchClause, WhereRefillMutatorContext context) {
        matchOrWithList.add(matchClause);
    }

    @Override
    public void visitWith(IWith withClause, WhereRefillMutatorContext context) {
        matchOrWithList.add(withClause);
    }

    @Override
    public void postProcessing(WhereRefillMutatorContext context) {
        if(matchOrWithList.size() == 0){
            return;
        }

        ICypherClause clause = matchOrWithList.get(new Randomly().getInteger(0, matchOrWithList.size()));
        if(clause instanceof IMatch){
            new RandomConditionGenerator<S>(schema, true).generateMatchCondition(((IMatch) clause).toAnalyzer(), schema);
            return;
        }
        else if(clause instanceof IWith){
            new RandomConditionGenerator<S>(schema, true).generateWithCondition(((IWith) clause).toAnalyzer(), schema);
            return;
        }

        throw new RuntimeException();

    }
}
