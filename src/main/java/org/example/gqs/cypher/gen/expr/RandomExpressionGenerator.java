package org.example.gqs.cypher.gen.expr;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.Alias;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.expr.*;
import org.example.gqs.neo4j.schema.Neo4jSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.analyzer.IAliasAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IClauseAnalyzer;
import org.example.gqs.cypher.ast.analyzer.INodeAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IRelationAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RandomExpressionGenerator<S extends CypherSchema<?,?>>
{
    IClauseAnalyzer clauseAnalyzer;
    S schema;
    public RandomExpressionGenerator(IClauseAnalyzer clauseAnalyzer, S schema){
        this.clauseAnalyzer = clauseAnalyzer;
        this.schema = schema;
    }

    private IExpression generateNumberAgg() {
        Randomly randomly = new Randomly();
        long randNum = 0;
        if (MainOptions.mode == "neo4j")
            randNum = randomly.getInteger(0, 100);
        else if (MainOptions.mode == "memgraph" || MainOptions.mode == "kuzu")
            randNum = randomly.getInteger(0, 50);
        IExpression param = generateUseVar(CypherType.NUMBER);
        if (param == null) {
            param = generateConstExpression(CypherType.NUMBER);
        }
        if (randNum < 10) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MAX_NUMBER, Arrays.asList(param));
        }
        if (randNum < 20) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MIN_NUMBER, Arrays.asList(param));
        }
        if (randNum < 40) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.AVG, Arrays.asList(param));
        }
        if (randNum < 50) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.COLLECT, Arrays.asList(param));
        }

        if (randNum < 60) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ST_DEV, Arrays.asList(param));
        }
        if (randNum < 70) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ST_DEV_P, Arrays.asList(param));
        }
        if (randNum < 80) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.PERCENTILE_COUNT_NUMBER, Arrays.asList(param));
        }
        if (randNum < 90) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.PERCENTILE_COUNT_STRING, Arrays.asList(param));
        }
        if (randNum < 100) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.PERCENTILE_DISC_NUMBER, Arrays.asList(param));
        }
        return new ConstExpression(0);
    }

    private IExpression generateStringAgg() {
        Randomly randomly = new Randomly();
        long randNum = randomly.getInteger(0, 20);
        IExpression param = generateUseVar(CypherType.STRING);
        if (param == null) {
            param = generateConstExpression(CypherType.STRING);
        }
        if (randNum < 10) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MAX_STRING, Arrays.asList(param));
        }
        if (randNum < 20) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MIN_STRING, Arrays.asList(param));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.COLLECT, Arrays.asList(param));
    }

    public IExpression generateFunction(CypherType type){
        if(type == CypherType.NUMBER){
            return generateNumberAgg();
        }
        return generateStringAgg();
    }

    private IExpression generateConstExpression(CypherType type){
        Randomly randomly = new Randomly();
        switch (type) {
            case NUMBER:
                return new ConstExpression((long) randomly.getInteger());
            case STRING:
                return new ConstExpression(randomly.getString());
            case BOOLEAN:
                return new ConstExpression(randomly.getInteger(0, 100) < 50);
            default:
                return null;
        }
    }

    public IExpression generateCondition(long depth){
        return booleanExpression(depth);
    }

    public IExpression generateListWithBasicType(long depth, CypherType type){
        Randomly randomly = new Randomly();
        long randomNum = randomly.getInteger(1,4);
        List<IExpression> expressions = new ArrayList<>();
        for(int i = 0; i < randomNum; i++) {
            expressions.add(basicTypeExpression(depth, type));
        }
        return new CreateListExpression(expressions);
    }

    private IExpression basicTypeExpression(long depth, CypherType type){
        switch (type){
            case BOOLEAN:
                return booleanExpression(depth);
            case STRING:
                return stringExpression(depth);
            case NUMBER:
                return numberExpression(depth);
            default:
                return null;
        }
    }

    private IExpression generateUseVar(CypherType type){
        Randomly randomly = new Randomly();

        List<IExpression> availableExpressions = new ArrayList<>();


        List<IAliasAnalyzer> aliasAnalyzers = clauseAnalyzer.getAvailableAliases();
        availableExpressions.addAll(aliasAnalyzers.stream().filter(a->a.analyzeType(schema).getType()==type).map(a->new IdentifierExpression(Alias.createIdentifierRef(a)))
                .collect(Collectors.toList()));


        List<INodeAnalyzer> nodeAnalyzers = clauseAnalyzer.getAvailableNodeIdentifiers();
        nodeAnalyzers.addAll(aliasAnalyzers.stream().filter(a->a.analyzeType(schema).getType()==CypherType.NODE).map(
                a-> a.analyzeType(schema).getNodeAnalyzer()
        ).collect(Collectors.toList()));

        List<IRelationAnalyzer> relationAnalyzers = clauseAnalyzer.getAvailableRelationIdentifiers();
        relationAnalyzers.addAll(aliasAnalyzers.stream().filter(a->a.analyzeType(schema).getType()==CypherType.RELATION).map(
                a-> a.analyzeType(schema).getRelationAnalyzer()
        ).collect(Collectors.toList()));

        nodeAnalyzers.stream().forEach(
                n->{
                    n.getAllPropertiesWithType(schema,type).forEach(
                            p-> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(n),
                                        p.getKey()));
                            }
                    );
                }
        );

        nodeAnalyzers.stream().forEach(
                n->{
                    availableExpressions.add(new IdentifierExpression(n));
                }
        );

        relationAnalyzers.stream().forEach(
                r->{
                    r.getAllPropertiesWithType(schema,type).forEach(
                            p-> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(r),
                                        p.getKey()));
                            }
                    );
                }
        );
        relationAnalyzers.stream().forEach(
                n->{
                    availableExpressions.add(new IdentifierExpression(n));
                }
        );

        if(availableExpressions.size() == 0){
            return generateConstExpression(type);
        }

        return availableExpressions.get(randomly.getInteger(0, availableExpressions.size()));
    }

    private IExpression booleanExpression(long depth) {
        Randomly randomly = new Randomly();
        long expressionChoice = randomly.getInteger(0, 100);
        if (depth == 0 || expressionChoice < 30) {
            long randomNum = randomly.getInteger(0, 100);
            if (randomNum < 20) {
                return generateConstExpression(CypherType.BOOLEAN);
            }
            return generateUseVar(CypherType.BOOLEAN);
        }

        if (expressionChoice < 50) {
            return BinaryComparisonExpression.randomComparison(numberExpression(depth - 1), numberExpression(depth - 1));
        }
        if (expressionChoice < 60) {
            return BinaryComparisonExpression.randomComparison(stringExpression(depth - 1), stringExpression(depth - 1));
        }
        if (expressionChoice < 70) {
            return StringMatchingExpression.randomMatching(stringExpression(depth - 1), stringExpression(depth - 1));
        }
        if (expressionChoice < 80) {
            return SingleLogicalExpression.randomLogical(booleanExpression(depth - 1));
        }
        return BinaryLogicalExpression.randomLogical(booleanExpression(depth - 1), booleanExpression(depth - 1));
    }

    private IExpression stringExpression(long depth){
        Randomly randomly = new Randomly();
        long expressionChoice = randomly.getInteger(0, 100);
        if(depth == 0 || expressionChoice < 70) {
            long randomNum = randomly.getInteger(0, 100);
            if (randomNum < 20) {
                return generateConstExpression(CypherType.STRING);
            }
            return generateUseVar(CypherType.STRING);
        }
        return new StringCatExpression(stringExpression(depth - 1), stringExpression(depth - 1));
    }

    private IExpression numberExpression(long depth) {
        Randomly randomly = new Randomly();
        long expressionChoice = randomly.getInteger(0, 100);
        if (depth == 0 || expressionChoice < 50) {
            long randomNum = randomly.getInteger(0, 100);
            if (randomNum < 20) {
                return generateConstExpression(CypherType.NUMBER);
            }
            return generateUseVar(CypherType.NUMBER);
        }
        return generateConstExpression(CypherType.NUMBER);
    }

}
