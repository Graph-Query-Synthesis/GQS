package org.example.gqs.cypher.mutation;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.ast.IMatch;
import org.example.gqs.cypher.dsl.ClauseVisitor;
import org.example.gqs.cypher.dsl.IContext;
import org.example.gqs.cypher.mutation.OptionalRemovalMutator.OptionalRemovalMutatorContext;

import java.util.ArrayList;
import java.util.List;

public class OptionalRemovalMutator extends ClauseVisitor<OptionalRemovalMutatorContext> implements IClauseMutator  {

    public List<IMatch> matchList = new ArrayList<>();

    public OptionalRemovalMutator(IClauseSequence clauseSequence) {
        super(clauseSequence, new OptionalRemovalMutatorContext());
    }

    @Override
    public void mutate() {
        startVisit();
    }

    public static class OptionalRemovalMutatorContext implements IContext {

    }

    @Override

    public void visitMatch(IMatch matchClause, OptionalRemovalMutatorContext context) {
        if(matchClause.isOptional()){
            matchList.add(matchClause);
        }
    }

    @Override
    public void postProcessing(OptionalRemovalMutatorContext context) {
        if(matchList.size() == 0){
            return;
        }

        IMatch match = matchList.get(new Randomly().getInteger(0, matchList.size()));
        match.setOptional(false);
    }
}
