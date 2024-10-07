package org.example.gqs.cypher.dsl;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.gen.alias.GuidedAliasGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.dsl.QueryFiller.QueryFillerContext;

import java.util.List;
import java.util.Map;

public class QueryFiller<S extends CypherSchema<?,?>> extends ClauseVisitor<QueryFillerContext<S>>{


    public static class QueryFillerContext<S extends CypherSchema<?,?>> implements IContext{
        private S schema;
        private IIdentifierBuilder identifierBuilder;
        private QueryFillerContext(S schema, IIdentifierBuilder identifierBuilder){
            this.schema = schema;
            this.identifierBuilder = identifierBuilder;
        }
    }

    private IPatternGenerator patternGenerator;
    private IConditionGenerator conditionGenerator;
    private IAliasGenerator aliasGenerator;
    private IListGenerator listGenerator;


    public QueryFiller(IClauseSequence clauseSequence, IPatternGenerator patternGenerator,
                       IConditionGenerator conditionGenerator, IAliasGenerator aliasGenerator,
                       IListGenerator listGenerator,
                       S schema, IIdentifierBuilder identifierBuilder){
        super(clauseSequence, new QueryFillerContext<>(schema, identifierBuilder));
        this.patternGenerator = patternGenerator;
        this.conditionGenerator = conditionGenerator;
        this.aliasGenerator = aliasGenerator;
        this.listGenerator = listGenerator;
    }

    @Override
    public void visitMatch(IMatch matchClause, QueryFillerContext<S> context) {
        if(patternGenerator!=null){
            patternGenerator.fillMatchPattern(matchClause.toAnalyzer());
        }
        if(conditionGenerator!=null){
            conditionGenerator.fillMatchCondtion(matchClause.toAnalyzer());
        }
        boolean useIndex  = Randomly.getBoolean();



































    }

    public void visitMatch(IMatch matchClause, QueryFillerContext<S> context, List<Map<String, Object>> namespace) {
        if(patternGenerator!=null){
            patternGenerator.fillMatchPattern(matchClause.toAnalyzer());
        }
        if(conditionGenerator!=null){
            conditionGenerator.fillMatchCondtion(matchClause.toAnalyzer(), namespace);
        }
        boolean useIndex  = Randomly.getBoolean();



































    }

    @Override
    public void visitWith(IWith withClause, QueryFillerContext<S> context) {
        if(aliasGenerator!=null){
            aliasGenerator.fillWithAlias(withClause.toAnalyzer());
        }
        if(conditionGenerator!=null){
            conditionGenerator.fillWithCondition(withClause.toAnalyzer());
        }
    }

    public void visitWith(IWith withClause, QueryFillerContext<S> context, List<Map<String, Object>> namespace) {
        if(aliasGenerator!=null){
            ((GuidedAliasGenerator)aliasGenerator).fillWithAlias(withClause.toAnalyzer(), namespace);
        }
        if(conditionGenerator!=null){
            conditionGenerator.fillWithCondition(withClause.toAnalyzer());
        }
    }

    @Override
    public void visitReturn(IReturn returnClause, QueryFillerContext<S> context) {
        if(aliasGenerator!=null){
            aliasGenerator.fillReturnAlias(returnClause.toAnalyzer());
        }
    }

    public void visitReturn(IReturn returnClause, QueryFillerContext<S> context, List<Map<String, Object>> namespace) {
        if(aliasGenerator!=null){
            aliasGenerator.fillReturnAlias(returnClause.toAnalyzer(), namespace);
        }
    }

    @Override
    public void visitCreate(ICreate createClause, QueryFillerContext<S> context) {

    }

    @Override
    public void visitUnwind(IUnwind unwindClause, QueryFillerContext<S> context) {
        if(listGenerator!=null){
            listGenerator.fillUnwindList(unwindClause.toAnalyzer());
        }
    }
}
