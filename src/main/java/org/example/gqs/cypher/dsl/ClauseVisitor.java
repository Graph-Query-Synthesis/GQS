package org.example.gqs.cypher.dsl;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.cypher.standard_ast.expr.IdentifierExpression;

import java.util.*;

public abstract class ClauseVisitor<C extends IContext> {

    protected IClauseSequence clauseSequence;
    private C context;
    private boolean continueVisit = true;
    private long presentIndex = 0;

    public ClauseVisitor(IClauseSequence clauseSequence, C context){
        this.clauseSequence = clauseSequence;
        this.context = context;
    }

    List<Map<String, String>> setCorrectReturn (List<ICypherClause> clauses, List<Map<String, Object>> namespace, long repeatTimes) {
        Return currentReturn = (Return) clauses.get(clauses.size() - 1);
        List<Map<String, Object>> expandedNamespace = currentReturn.expandedNamespace;
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < expandedNamespace.size(); i++) {
            Map<String, Object> currentNamespace = expandedNamespace.get(i);
            result.add(new HashMap<>());
            for (IRet ret : currentReturn.getReturnList()) {
                Object currentResult = null;
                if (ret.getIdentifier() instanceof Alias) {
                    currentResult = currentReturn.getFromNamespace(new IdentifierExpression(ret.getIdentifier()), currentNamespace);
                } else
                    currentResult = currentReturn.getFromNamespace(ret.getExpression(), currentNamespace);

                result.get(i).put(ret.getIdentifier().getName(), currentResult.toString());
            }
        }
        return result;
    }

    public void startVisit(){
        if(this.clauseSequence.getClauseList() == null || this.clauseSequence.getClauseList().size() == 0){
            return;
        }
        List<ICypherClause> clauses = this.clauseSequence.getClauseList();
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> namespace = new ArrayList<>();
        namespace.add(new HashMap<>());
        long repeatTimes = 1;
        for(int i = 0; i < clauses.size(); i++) {
            presentIndex = i;
            if (System.currentTimeMillis() - startTime > (MainOptions.timeout * 1000) && MainOptions.debug == -1) {
                System.out.println("!!!!!!!!!!timeout!!!!!!!!!!!!!!!");
                i = clauses.size() - 1;
            }
            visitClause(clauses.get(i), namespace);
            if (clauses.get(i) instanceof IMatch) {
                namespace = ((Match) clauses.get(i)).getNamespace(namespace);
            } else if (clauses.get(i) instanceof IUnwind) {
                namespace = ((Unwind) clauses.get(i)).getNamespace(namespace);
                repeatTimes *= ((Unwind) clauses.get(i)).repeatTimes;
            } else if (clauses.get(i) instanceof IWith) {
                repeatTimes = ((With) clauses.get(i)).repeatTimes;
            } else if (clauses.get(i) instanceof IReturn) {
                List<Map<String, String>> res = setCorrectReturn(clauses, namespace, repeatTimes);
                ((Return) clauses.get(i)).correctReturn = res;
            }

            if (!continueVisit) {
                postProcessing(context);
                return;
            }
        }
        postProcessing(context);
    }

    public void postProcessing(C context){
        if(MainOptions.mode == "memgraph")
        {
            List<String> indexNodeList = new ArrayList<>(), indexLabelList = new ArrayList<>(), indexPropertyList = new ArrayList<>();
            if(this.clauseSequence.getClauseList() == null || this.clauseSequence.getClauseList().size() == 0){
                return;
            }
            List<ICypherClause> clauses = this.clauseSequence.getClauseList();
            for(ICypherClause clause : clauses)
            {
                if(clause instanceof IMatch)
                {
                    Match currentMatch = (Match) clause;
                    if(currentMatch.indexNode != null)
                    {
                        indexNodeList.add(currentMatch.indexNode);
                        indexLabelList.add(currentMatch.indexLabel);
                        indexPropertyList.add(currentMatch.indexProperty);
                    }
                }
            }
            if(indexNodeList.size() > 0 && clauses.get(0) instanceof Match) {
                ((Match) clauses.get(0)).prefix = "";
            }
        }
        if(false) {
            if (MainOptions.mode == "kuzu") {
                List<ICypherClause> clauses = this.clauseSequence.getClauseList();
                String[] possible = {"db_version() ", "show_tables() "};
                ((Match) clauses.get(0)).prefix = "CALL " + Randomly.fromList(Arrays.asList(possible));
            } else if (MainOptions.mode == "falkordb") {
                List<ICypherClause> clauses = this.clauseSequence.getClauseList();

                String[] possible = {"db.labels() ", "db.relationshipTypes() ", ""};
                ((Match) clauses.get(0)).prefix = "CALL " + Randomly.fromList(Arrays.asList(possible));
            }
        }
    }

    public void reverseVisit(){
        if(this.clauseSequence.getClauseList() == null || this.clauseSequence.getClauseList().size() == 0){
            return;
        }
        List<ICypherClause> clauses = this.clauseSequence.getClauseList();
        for(int i = clauses.size() - 1; i >= 0; i--){
            presentIndex = i;
            visitClause(clauses.get(i));
            if(!continueVisit){
                postProcessing(context);
                return;
            }
        }
        postProcessing(context);
    }

    public IClauseSequence getClauseSequence(){
        return clauseSequence;
    }

    protected long getPresentIndex(){
        return presentIndex;
    }

    protected void stopVisit(){
        continueVisit = false;
    }

    public void visitClause(ICypherClause clause){
        if(clause instanceof IMatch){
            visitMatch((IMatch) clause, context);
        }
        else if(clause instanceof IWith){
            visitWith((IWith) clause, context);
        }
        else if(clause instanceof ICreate){
            visitCreate((ICreate) clause, context);
        }
        else if(clause instanceof IReturn){
            visitReturn((IReturn) clause, context);
        }
        else if(clause instanceof IUnwind){
            visitUnwind((IUnwind) clause, context);
        }
    }

    public void visitClause(ICypherClause clause, List<Map<String, Object>> namespace){
        if(clause instanceof IMatch){
            visitMatch((IMatch) clause, context, namespace);
        }
        else if(clause instanceof IWith){
            visitWith((IWith) clause, context, namespace);
        }
        else if(clause instanceof ICreate){
            visitCreate((ICreate) clause, context);
        }
        else if(clause instanceof IReturn){
            visitReturn((IReturn) clause, context, namespace);
        }
        else if(clause instanceof IUnwind){
            visitUnwind((IUnwind) clause, context);
        }
    }

    public void visitMatch(IMatch matchClause, C context){}
    public void visitMatch(IMatch matchClause, C context, List<Map<String, Object>> namespace){}
    public void visitWith(IWith withClause, C context){}
    public void visitWith(IWith withClause, C context, List<Map<String, Object>> namespace){}
    public void visitReturn(IReturn returnClause, C context){}
    public void visitReturn(IReturn returnClause, C context, List<Map<String, Object>> namespace){}
    public void visitCreate(ICreate createClause, C context){}
    public void visitUnwind(IUnwind unwindClause, C context){}
    public void visitMerge(IMerge mergeClause, C context){}

}
