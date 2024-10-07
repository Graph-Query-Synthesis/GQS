package org.example.gqs.cypher.gen.condition;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IRelationAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;
import org.example.gqs.cypher.dsl.BasicConditionGenerator;
import org.example.gqs.cypher.gen.expr.RandomExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.List;
import java.util.Map;

public class RandomConditionGenerator<S extends CypherSchema<?,?>> extends BasicConditionGenerator<S> {
    private boolean overrideOld;
    public RandomConditionGenerator(S schema, boolean overrideOld) {
        super(schema);
        this.overrideOld = overrideOld;
    }

    private static final long NO_CONDITION_RATE = 50, MAX_DEPTH = 1;

    @Override
    public IExpression generateMatchCondition(IMatchAnalyzer matchClause, S schema) {
        IExpression matchCondition = matchClause.getCondition();
        if (matchCondition != null && !overrideOld) {
            return matchCondition;
        }

        Randomly r = new Randomly();
        List<IRelationAnalyzer> relationships = matchClause.getLocalRelationIdentifiers();
        if (r.getInteger(0, 100) < NO_CONDITION_RATE) {
            if (relationships.size() != 0) {
                IExpression result = new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(0)), "id"), new ConstExpression(-1), BinaryComparisonExpression.BinaryComparisonOperation.HIGHER);
                for (int x = 0; x < relationships.size(); x++) {
                    for (int y = x + 1; y < relationships.size(); y++) {
                        result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                    }
                }
                return result;
            }
            return null;
        }
        IExpression result = new RandomExpressionGenerator<>(matchClause, schema).generateCondition(MAX_DEPTH);
        for (int x = 0; x < relationships.size(); x++) {
            for (int y = x + 1; y < relationships.size(); y++) {
                result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
            }
        }
        return result;
    }

    @Override
    public IExpression generateMatchCondition(IMatchAnalyzer matchClause, S schema, List<Map<String, Object>> namespace) {
        return null;
    }

    @Override
    public IExpression generateWithCondition(IWithAnalyzer withClause, S schema) {
        IExpression withCondition = withClause.getCondition();
        if (withCondition != null && !overrideOld) {
            return withCondition;
        }

        Randomly r = new Randomly();


        return new RandomExpressionGenerator<>(withClause, schema).generateCondition(MAX_DEPTH);
    }
}
