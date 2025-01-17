package org.example.gqs.cypher.gen.condition;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IRelationAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;
import org.example.gqs.cypher.dsl.BasicConditionGenerator;
import org.example.gqs.cypher.gen.expr.NonEmptyExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.Alias;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuidedConditionGenerator<S extends CypherSchema<?,?>> extends BasicConditionGenerator<S> {
    private boolean overrideOld;
    private Map<String, Object> varToVal;

    public GuidedConditionGenerator(S schema, boolean overrideOld, Map<String, Object> varToVal) {
        super(schema);
        this.overrideOld = overrideOld;
        this.varToVal = varToVal;
    }

    private static final long NO_CONDITION_RATE = 0;
    public static final long MAX_DEPTH = 6;

    @Override
    public IExpression generateMatchCondition(IMatchAnalyzer matchClause, S schema) {
        IExpression matchCondition = matchClause.getCondition();
        if (matchCondition != null && !overrideOld) {
            return matchCondition;
        }

        Randomly r = new Randomly();
        List<IRelationAnalyzer> relationships = matchClause.getLocalRelationIdentifiers();
        IExpression result = null;
        if (r.getInteger(0, 100) < NO_CONDITION_RATE) {
            if (relationships.size() != 0) {
                result = new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(0)), "id"), new ConstExpression(-1), BinaryComparisonExpression.BinaryComparisonOperation.HIGHER);
                for (int x = 0; x < r.getInteger(0, relationships.size() / 2); x++) {
                    for (int y = x + 1; y < r.getInteger(0, relationships.size() / 2); y++) {
                        result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                    }
                }
            }
        } else {
            result = new NonEmptyExpressionGenerator<>(matchClause, schema, varToVal).generateCondition(MAX_DEPTH);
            for (int x = 0; x < r.getInteger(0, relationships.size() / 2); x++) {
                for (int y = x + 1; y < r.getInteger(0, relationships.size() / 2); y++) {
                    result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                }
            }
        }
        IExpression equationCondition = new NonEmptyExpressionGenerator<>(matchClause, schema, varToVal).generateEquationWithMatch(matchClause, null);
        if (result == null)
            result = equationCondition;
        else
            result = new BinaryLogicalExpression(result, equationCondition, BinaryLogicalExpression.BinaryLogicalOperation.AND);
        return result;
    }

    public IExpression generateMatchCondition(IMatchAnalyzer matchClause, S schema, List<Map<String, Object>> namespace) {
        if (MainOptions.exp.equals("expression") || MainOptions.exp.equals("both")) {
            IExpression result = new NonEmptyExpressionGenerator<>(matchClause, schema, varToVal).generateSimpleEquationWithMatch(matchClause, namespace);
            return result;
        }

        IExpression matchCondition = matchClause.getCondition();
        if (matchCondition != null && !overrideOld) {
            return matchCondition;
        }

        Randomly r = new Randomly();
        List<IRelationAnalyzer> relationships = matchClause.getLocalRelationIdentifiers();
        IExpression result = null;
        if (r.getInteger(0, 100) < NO_CONDITION_RATE) {
            if (relationships.size() != 0) {
                result = new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(0)), "id"), new ConstExpression(-1), BinaryComparisonExpression.BinaryComparisonOperation.HIGHER);
                if (MainOptions.mode.equals("falkordb") || MainOptions.mode.equals("kuzu")) {
                    for (int x = 0; x < r.getInteger(0, relationships.size() / 2); x++) {
                        for (int y = x + 1; y < r.getInteger(0, relationships.size() / 2); y++) {
                            result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                        }
                    }
                }
            }
        } else {
            result = new NonEmptyExpressionGenerator<>(matchClause, schema, varToVal).generateCondition(MAX_DEPTH);
            if (MainOptions.mode != "falkordb" && MainOptions.mode != "kuzu") {
                for (int x = 0; x < r.getInteger(0, relationships.size() / 2); x++) {
                    for (int y = x + 1; y < r.getInteger(0, relationships.size() / 2); y++) {
                        result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                    }
                }
            } else {
                for (int x = 0; x < relationships.size(); x++) {
                    for (int y = 0; y < relationships.size(); y++) {
                        if (x != y) {
                            result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(new GetPropertyExpression(new IdentifierExpression(relationships.get(x)), "id"), new GetPropertyExpression(new IdentifierExpression(relationships.get(y)), "id"), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                        }
                    }
                }
            }
        }
        IExpression equationCondition = new NonEmptyExpressionGenerator<>(matchClause, schema, varToVal).generateEquationWithMatch(matchClause, namespace);
        if (result == null)
            result = equationCondition;
        else
            result = new BinaryLogicalExpression(result, equationCondition, BinaryLogicalExpression.BinaryLogicalOperation.AND);
        return result;
    }

    IExpression ruleOutBothCondition(IExpression current, Set<Object> constant)
    {
        Randomly r = new Randomly();
        if(current instanceof BinaryLogicalExpression)
        {
            ((BinaryLogicalExpression) current).setLeftExpression(ruleOutBothCondition(((BinaryLogicalExpression) current).getLeftExpression(), constant));
            ((BinaryLogicalExpression) current).setRightExpression(ruleOutBothCondition(((BinaryLogicalExpression) current).getRightExpression(), constant));
        }
        else if(current instanceof BinaryComparisonExpression)
        {
            if(!(((BinaryComparisonExpression)current).getLeftExpression() instanceof ConstExpression) && !(((BinaryComparisonExpression)current).getRightExpression() instanceof ConstExpression))
            {
                Object rightValue = ((CypherExpression)((BinaryComparisonExpression)current).getRightExpression()).getValue();
                ((BinaryComparisonExpression)current).replaceChild(((BinaryComparisonExpression)current).getRightExpression(), new ConstExpression(rightValue));
            }
            else if (constant.contains(((CypherExpression)((BinaryComparisonExpression)(current)).getLeftExpression()).getValue()) || constant.contains(((CypherExpression)((BinaryComparisonExpression)(current)).getRightExpression()).getValue()) || (((BinaryComparisonExpression)current).getRightExpression() instanceof GetPropertyExpression || ((((BinaryComparisonExpression)current).getRightExpression() instanceof IdentifierExpression) && ((IdentifierExpression)((BinaryComparisonExpression)current).getRightExpression()).getIdentifier() instanceof Alias && ((Alias)((IdentifierExpression)((BinaryComparisonExpression)current).getRightExpression()).getIdentifier()).getExpression() instanceof GetPropertyExpression)))
            {
                if(constant.contains(((CypherExpression)((BinaryComparisonExpression)(current)).getLeftExpression()).getValue()))
                {
                    Object leftValue = ((CypherExpression)((BinaryComparisonExpression)current).getLeftExpression()).getValue();
                    Object rightValue = ((CypherExpression)((BinaryComparisonExpression)current).getRightExpression()).getValue();
                    IExpression toReplace = ((BinaryComparisonExpression)current).getLeftExpression();
                    if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.EQUAL)
                    {
                        ((BinaryComparisonExpression)current).setOperation(BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL);
                        if(leftValue instanceof Integer) {
                            while(leftValue == rightValue)
                                leftValue = (int) leftValue + r.getInteger(1, 10);
                        }
                        else if(leftValue instanceof Long) {
                            while(leftValue == rightValue)
                                leftValue = (long) leftValue + r.getInteger(1, 10);
                        }
                        else if(leftValue instanceof String)
                        {
                            while(leftValue.equals(rightValue))
                                leftValue = (String)leftValue + r.getString();
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(leftValue));
                    }
                    else if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.SMALLER || ((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.SMALLER_OR_EQUAL)
                    {
                        if(leftValue instanceof Integer)
                            leftValue = (int)leftValue - r.getInteger(1, 10);
                        else if(leftValue instanceof Long)
                            leftValue = (long)leftValue - r.getInteger(1, 10);
                        else if(leftValue instanceof String)
                        {
                            leftValue = (String)leftValue + r.getString();
                            ((BinaryComparisonExpression)current).setOperation(BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL);
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(leftValue));
                    }
                    else if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.HIGHER || ((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.HIGHER_OR_EQUAL)
                    {
                        if(leftValue instanceof Integer)
                            leftValue = (int)leftValue + r.getInteger(1, 10);
                        else if(leftValue instanceof Long)
                            leftValue = (long)leftValue + r.getInteger(1, 10);
                        else if(leftValue instanceof String)
                        {
                            leftValue = (String)leftValue + r.getString();
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(leftValue));
                    }
                    else if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL)
                    {
                        if(leftValue instanceof Integer) {
                            leftValue = (int) leftValue + r.getInteger(1, 10);
                            while(leftValue == rightValue)
                                leftValue = (int) leftValue + r.getInteger(1, 10);
                        }
                        else if(leftValue instanceof Long) {
                            leftValue = (long) leftValue + r.getInteger(1, 10);
                            while(leftValue == rightValue)
                                leftValue = (long) leftValue + r.getInteger(1, 10);
                        }
                        else if(leftValue instanceof String)
                        {
                            leftValue = (String)leftValue + r.getString();
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(leftValue));
                    }
                }
                if(constant.contains(((CypherExpression)((BinaryComparisonExpression)(current)).getRightExpression()).getValue()) || (((BinaryComparisonExpression)current).getRightExpression() instanceof GetPropertyExpression || ((((BinaryComparisonExpression)current).getRightExpression() instanceof IdentifierExpression) && ((IdentifierExpression)((BinaryComparisonExpression)current).getRightExpression()).getIdentifier() instanceof Alias && ((Alias)((IdentifierExpression)((BinaryComparisonExpression)current).getRightExpression()).getIdentifier()).getExpression() instanceof GetPropertyExpression)))
                {
                    Object leftValue = ((CypherExpression)((BinaryComparisonExpression)current).getLeftExpression()).getValue();
                    Object rightValue = ((CypherExpression)((BinaryComparisonExpression)current).getRightExpression()).getValue();
                    IExpression toReplace = ((BinaryComparisonExpression)current).getRightExpression();
                    if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.EQUAL)
                    {
                        ((BinaryComparisonExpression)current).setOperation(BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL);
                        if(rightValue instanceof Integer) {
                            while(leftValue == rightValue)
                                rightValue = (int) rightValue + r.getInteger(1, 10);
                        }
                        else if(rightValue instanceof Long) {
                            while(leftValue == rightValue)
                                rightValue = (long) rightValue + r.getInteger(1, 10);
                        }
                        else if(rightValue instanceof String)
                        {
                            rightValue = (String)rightValue + r.getString();
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(rightValue));
                    }
                    else if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.SMALLER || ((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.SMALLER_OR_EQUAL)
                    {
                        if(rightValue instanceof Integer)
                            rightValue = (int)rightValue + r.getInteger(1, 10);
                        else if(rightValue instanceof Long)
                            rightValue = (long)rightValue + r.getInteger(1, 10);
                        else if(rightValue instanceof String)
                        {
                            rightValue = (String)rightValue + r.getString();
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(rightValue));
                    }
                    else if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.HIGHER || ((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.HIGHER_OR_EQUAL)
                    {
                        if(rightValue instanceof Integer)
                            rightValue = (int)rightValue - r.getInteger(1, 10);
                        else if(rightValue instanceof Long)
                            rightValue = (long)rightValue - r.getInteger(1, 10);
                        else if(rightValue instanceof String)
                        {
                            rightValue = (String)rightValue + r.getString();
                            ((BinaryComparisonExpression)current).setOperation(BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL);
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(rightValue));
                    }
                    else if(((BinaryComparisonExpression)current).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL)
                    {
                        if(rightValue instanceof Integer) {
                            while(leftValue == rightValue)
                                rightValue = (int) rightValue + r.getInteger(1, 10);
                        }
                        else if(rightValue instanceof Long) {
                            rightValue = (long) rightValue + r.getInteger(1, 10);
                            while(leftValue == rightValue)
                            rightValue = (long) rightValue + r.getInteger(1, 10);
                        }
                        else if(rightValue instanceof String)
                        {
                            rightValue = (String)rightValue + r.getString();
                        }
                        ((BinaryComparisonExpression)current).replaceChild(toReplace, new ConstExpression(rightValue));
                    }
                }

            }
        }
        else if (current instanceof StringMatchingExpression)
        {
            if(!(((StringMatchingExpression)current).getPattern() instanceof ConstExpression) && !(((StringMatchingExpression)current).getSource() instanceof ConstExpression))
            {
                Object rightValue = ((CypherExpression)((StringMatchingExpression)current).getSource()).getValue();
                ((StringMatchingExpression)current).replaceChild(((StringMatchingExpression)current).getSource(), new ConstExpression(rightValue));
            }
        }
        return current;
    }

    @Override
    public IExpression generateWithCondition(IWithAnalyzer withClause, S schema) {
        if (MainOptions.exp.equals("expression") || MainOptions.exp.equals("both")) {
            return new ConstExpression(true);
        }
        IExpression withCondition = withClause.getCondition();
        if (withCondition != null) {
            return withCondition;
        }

        Randomly r = new Randomly();
        if (r.getInteger(0, 100) < NO_CONDITION_RATE) {
            return null;
        }
        IExpression returnExp = new NonEmptyExpressionGenerator<>(withClause, schema, varToVal).generateCondition(MAX_DEPTH);
        if (MainOptions.mode == "thinker") {
            Set<Object> constants = new HashSet<>();
            List<IRet> ret = withClause.getReturnList();
            for (IRet retItem : ret) {
                if (retItem.getExpression() instanceof ConstExpression) {
                    constants.add(((ConstExpression) retItem.getExpression()).getValue());
                }
            }

            returnExp = ruleOutBothCondition(returnExp, constants);
        }
        return returnExp;
    }
}
