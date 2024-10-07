package org.example.gqs.cypher.gen.condition;

import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IRelationAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;
import org.example.gqs.cypher.dsl.BasicConditionGenerator;
import org.example.gqs.cypher.gen.EnumerationSeq;
import org.example.gqs.cypher.gen.expr.EnumerationExpressionGenerator;
import org.example.gqs.cypher.gen.expr.RandomExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.List;
import java.util.Map;

public class EnumerationConditionGenerator<S extends CypherSchema<?,?>> extends BasicConditionGenerator<S> {
    private EnumerationSeq enumerationSeq;
    public EnumerationConditionGenerator(S schema, EnumerationSeq enumerationSeq) {
        super(schema);
        this.enumerationSeq = enumerationSeq;
    }

    private static final long MAX_DEPTH = 10;

    @Override
    public IExpression generateMatchCondition(IMatchAnalyzer matchClause, S schema) {
        List<IRelationAnalyzer> relationships = matchClause.getLocalRelationIdentifiers();

        if(enumerationSeq.getDecision()){
            if(relationships.size() != 0){
                IExpression result = new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(0)), "id"), new ConstExpression(-1), BinaryComparisonExpression.BinaryComparisonOperation.HIGHER);
                for(int x = 0; x < relationships.size(); x++){
                    for(int y = x + 1; y < relationships.size(); y++){
                        result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                    }
                }
                return result;
            }
            return null;
        }
        IExpression result = new EnumerationExpressionGenerator<>(matchClause, enumerationSeq, schema).generateCondition(MAX_DEPTH);

        for(int x = 0; x < relationships.size(); x++){
            for(int y = x + 1; y < relationships.size(); y++){
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
        if(enumerationSeq.getDecision()){
            return null;
        }
        return new RandomExpressionGenerator<>(withClause, schema).generateCondition(MAX_DEPTH);
    }
}
