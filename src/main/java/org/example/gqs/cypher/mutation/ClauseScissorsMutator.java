package org.example.gqs.cypher.mutation;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.dsl.ClauseVisitor;
import org.example.gqs.cypher.dsl.IContext;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.mutation.ClauseScissorsMutator.ClauseScissorsMutatorContext;
import org.example.gqs.cypher.standard_ast.Return;

import java.util.List;
import java.util.stream.Collectors;

public class ClauseScissorsMutator <S extends CypherSchema<?,?>> extends ClauseVisitor<ClauseScissorsMutatorContext<S>> implements IClauseMutator {

    Randomly randomly;

    public ClauseScissorsMutator(IClauseSequence clauseSequence) {
        super(clauseSequence, new ClauseScissorsMutatorContext<>());
        randomly = new Randomly();
    }

    @Override
    public void visitMatch(IMatch matchClause, ClauseScissorsMutatorContext<S> context) {
        return;
    }

    @Override
    public void visitWith(IWith withClause, ClauseScissorsMutatorContext<S> context) {
        if(randomly.getInteger(0, 100) < 50){
            long presentInedx = getPresentIndex();
            if(presentInedx != 0){
                Return returnClause = new Return();
                returnClause.setReturnList(withClause.getReturnList().stream().map(r->r.getCopy()).collect(Collectors.toList()));
                List<ICypherClause> clauses = getClauseSequence().getClauseList();
                clauses.get((int) presentInedx).setPrevClause(null);
                clauses.get((int) (presentInedx - 1)).setNextClause(returnClause);
                clauses.set((int) presentInedx, returnClause);
                getClauseSequence().setClauseList(clauses.subList(0, (int) (presentInedx + 1)));
                stopVisit();
                return;
            }
        }
    }

    public void mutate(){
        reverseVisit();
    }

    @Override
    public void visitReturn(IReturn returnClause, ClauseScissorsMutatorContext<S> context) {
        return;
    }

    @Override
    public void visitCreate(ICreate createClause, ClauseScissorsMutatorContext<S> context) {
        return;
    }

    @Override
    public void visitUnwind(IUnwind unwindClause, ClauseScissorsMutatorContext<S> context) {
        return;
    }

    public static class ClauseScissorsMutatorContext<S extends CypherSchema<?,?>> implements IContext {
        private ClauseScissorsMutatorContext(){

        }

    }
}
