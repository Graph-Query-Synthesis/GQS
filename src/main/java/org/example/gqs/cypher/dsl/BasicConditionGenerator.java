package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;
import org.example.gqs.cypher.schema.CypherSchema;

import java.util.List;
import java.util.Map;

public abstract class BasicConditionGenerator<S extends CypherSchema<?,?>> implements IConditionGenerator{

    private final S schema;

    public BasicConditionGenerator(S schema){
        this.schema = schema;
    }

    @Override
    public void fillMatchCondtion(IMatchAnalyzer matchClause) {
        matchClause.setCondition(generateMatchCondition(matchClause, schema));
    }

    public void fillMatchCondtion(IMatchAnalyzer matchClause, List<Map<String, Object>> namespace) {
        matchClause.setCondition(generateMatchCondition(matchClause, schema, namespace));
    }

    @Override
    public void fillWithCondition(IWithAnalyzer withClause) {
        withClause.setCondition(generateWithCondition(withClause, schema));
    }

    public abstract IExpression generateMatchCondition(IMatchAnalyzer matchClause, S schema);
    public abstract IExpression generateMatchCondition(IMatchAnalyzer matchClause, S schema, List<Map<String, Object>> namespace);
    public abstract IExpression generateWithCondition(IWithAnalyzer withClause, S schema);
}
