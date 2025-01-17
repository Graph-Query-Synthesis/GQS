package org.example.gqs.cypher.gen.expr;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.*;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.gen.assertion.BooleanAssertion;
import org.example.gqs.cypher.gen.assertion.ComparisonAssertion;
import org.example.gqs.cypher.gen.assertion.ExpressionAssertion;
import org.example.gqs.cypher.gen.assertion.StringMatchingAssertion;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.schema.IRelationTypeInfo;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.cypher.standard_ast.expr.*;
import org.example.gqs.neo4j.schema.Neo4jSchema;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static org.example.gqs.cypher.gen.GraphManager.generateEquationForSingleElement;

public class NonEmptyExpressionGenerator<S extends CypherSchema<?, ?>> {
    IClauseAnalyzer clauseAnalyzer;
    S schema;

    private Map<String, Object> varToProperties = new HashMap<>();

    private Randomly randomly = new Randomly();

    public NonEmptyExpressionGenerator(IClauseAnalyzer clauseAnalyzer, S schema, Map<String, Object> varToProperties) {
        this.clauseAnalyzer = clauseAnalyzer;
        this.schema = schema;
        this.varToProperties = varToProperties;
    }


    private IExpression generateNumberAgg() {
        Randomly randomly = new Randomly();

        long randNum = 0;
        if (MainOptions.mode == "neo4j")
            randNum = randomly.getInteger(0, 31);
        else if (MainOptions.mode == "memgraph" || MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
            randNum = randomly.getInteger(0, 31);
        else if (MainOptions.mode == "thinker") {
            randNum = randomly.getInteger(0, 5);
            while (randNum == 2)
                randNum = randomly.getInteger(0, 5);
        } else
            throw new RuntimeException("Unknown mode");

        long aggregationRandom = randomly.getInteger(0, 100);
        IExpression param = generateUseVar(CypherType.NUMBER, null);
        Long res = (long) ((CypherExpression) param).getValue();
        if (res > Integer.MAX_VALUE) {
            if (MainOptions.mode == "thinker") {
                long divided = (long) Integer.MAX_VALUE + Randomly.smallNumber();
                long diff = res / divided;
                param = new ConstExpression(diff);
            } else
                param = new BinaryNumberExpression(param, new ConstExpression((long) Integer.MAX_VALUE + Randomly.smallNumber()), BinaryNumberExpression.BinaryNumberOperation.DIVISION);
        }

        if (res < Integer.MIN_VALUE) {
            if (MainOptions.mode == "thinker") {
                long divided = (long) Integer.MIN_VALUE + Randomly.smallNumber();
                long diff = res / divided;
                param = new ConstExpression(diff);
            } else
                param = new BinaryNumberExpression(param, new ConstExpression((long) Integer.MIN_VALUE + Randomly.smallNumber()), BinaryNumberExpression.BinaryNumberOperation.DIVISION);
        }
        if (param == null) {
            param = generateNumberConst(null);
        }
        if (randNum == 0 || (aggregationRandom > 50 && aggregationRandom < 60)) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MAX_NUMBER, Arrays.asList(param));
        }
        if (randNum == 1 || (aggregationRandom > 60 && aggregationRandom < 70)) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MIN_NUMBER, Arrays.asList(param));
        }
        if (randNum == 2 || (aggregationRandom > 70 && aggregationRandom < 80)) {
            if (MainOptions.mode == "thinker")
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(param));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.AVG, Arrays.asList(param));
        }
        if (randNum == 3 || (aggregationRandom > 80 && aggregationRandom < 90)) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(param));
        }
        if (randNum == 4 || (aggregationRandom > 90 && aggregationRandom < 100)) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.COUNT, Arrays.asList(param));
        }
        if (randNum == 5) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ABS, Arrays.asList(param));
        }
        if (randNum == 6) {
            Long current = (long) ((CypherExpression) param).getValue();
            if (current > 1) {
                long diff = current - Randomly.getNotCachedInteger(-1, 1);
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.MINUS);
            }
            if (current < -1) {
                long diff = Randomly.getNotCachedInteger(-1, 1) - current;
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.ADD);
            }
            if (MainOptions.mode == "kuzu" || MainOptions.mode == "falkordb")
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ABS, Arrays.asList(param));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ACOS, Arrays.asList(param));
        }
        if (randNum == 7) {
            Long current = (long) ((CypherExpression) param).getValue();
            if (current > 1) {
                long diff = current - Randomly.getNotCachedInteger(-1, 1);
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.MINUS);
            }
            if (current < -1) {
                long diff = Randomly.getNotCachedInteger(-1, 1) - current;
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.ADD);
            }
            if (MainOptions.mode == "kuzu" || MainOptions.mode == "falkordb")
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ABS, Arrays.asList(param));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ASIN, Arrays.asList(param));
        }
        if (randNum == 8) {
            if (MainOptions.mode == "kuzu" || MainOptions.mode == "falkordb")
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ABS, Arrays.asList(param));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ATAN, Arrays.asList(param));
        }
        if (randNum == 9) {
            IExpression param1 = generateUseVar(CypherType.NUMBER, null);
            if (param1 == null) {
                param1 = generateNumberConst(null);
            }
            IExpression param2 = generateUseVar(CypherType.NUMBER, null);
            if (param2 == null) {
                param2 = generateNumberConst(null);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ATAN2, Arrays.asList(param1, param2));
        }
        if (randNum == 10) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.CEIL, Arrays.asList(param));
        }
        if (randNum == 11) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.COS, Arrays.asList(param));
        }
        if (randNum == 12) {
            if (MainOptions.mode == "kuzu")
                randNum = 14;
            else
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.E, new ArrayList<>());
        }
        if (randNum == 13) {
            if (MainOptions.mode == "kuzu")
                randNum = 14;
            else {
                long current = (long) ((CypherExpression) param).getValue();
                if (current > 30) {
                    long diff = current - Randomly.getNotCachedInteger(30, min(40L, current));
                    param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.MINUS);
                }
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.EXP, Arrays.asList(param));
            }
        }
        if (randNum == 14) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.FLOOR, Arrays.asList(param));
        }
        if (randNum == 15) {
            long current = (long) ((CypherExpression) param).getValue();
            if (current <= 0) {
                long diff = Randomly.getNotCachedInteger(abs(current - 1), abs(current - 1) + Randomly.getNotCachedInteger(1, 10));
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.ADD);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.LOG, Arrays.asList(param));
        }
        if (randNum == 16) {
            long current = (long) ((CypherExpression) param).getValue();
            if (current <= 0) {
                long diff = Randomly.getNotCachedInteger(abs(current - 1), abs(current - 1) + Randomly.getNotCachedInteger(1, 10));
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.ADD);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.LOG10, Arrays.asList(param));
        }
        if (randNum == 17) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.PI, new ArrayList<>());
        }
        if (randNum == 18) {
            long current = (long) ((CypherExpression) param).getValue();
            if (current <= 0) {
                long diff = Randomly.getNotCachedInteger(abs(current - 1), abs(current - 1) + Randomly.getNotCachedInteger(1, 10));
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.ADD);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.LOG10, Arrays.asList(param));
        }
        if (randNum == 19) {
            if (MainOptions.mode == "kuzu")
                randNum = 15;
            else
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ROUND, Arrays.asList(param));
        }
        if (randNum == 20) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SIGN, Arrays.asList(param));
        }
        if (randNum == 21) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SIN, Arrays.asList(param));
        }
        if (randNum == 22) {
            long current = (long) ((CypherExpression) param).getValue();
            if (current <= 0) {
                long diff = Randomly.getNotCachedInteger(abs(current - 1), abs(current - 1) + Randomly.getNotCachedInteger(1, 10));
                param = new BinaryNumberExpression(param, new ConstExpression(diff), BinaryNumberExpression.BinaryNumberOperation.ADD);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SQRT, Arrays.asList(param));
        }
        if (randNum == 23) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TAN, Arrays.asList(param));
        }
        if (randNum == 24 && MainOptions.mode == "neo4j") {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ST_DEV, Arrays.asList(param));
        }
        if (randNum == 25 && MainOptions.mode == "neo4j") {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ST_DEV_P, Arrays.asList(param));
        }
        if (randNum == 24 && MainOptions.mode == "memgraph") {
            IExpression param1 = generateUseVar(CypherType.NODE, null);
            if (param1 instanceof ConstExpression)
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MAX_NUMBER, Arrays.asList(param1));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.DEGREE, Arrays.asList(param1));
        }
        if (randNum == 25 && MainOptions.mode == "memgraph") {
            IExpression param1 = generateUseVar(CypherType.NODE, null);
            if (param1 instanceof ConstExpression)
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MIN_NUMBER, Arrays.asList(param1));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.OUT_DEGREE, Arrays.asList(param1));
        }
        if (randNum == 26 && MainOptions.mode != "kuzu") {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOINTEGER_INTEGER, Arrays.asList(param));
        }
        if (randNum == 27 && MainOptions.mode == "memgraph") {
            IExpression param1 = generateUseVar(CypherType.NODE, null);
            if (param1 instanceof ConstExpression)
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(param1));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.IN_DEGREE, Arrays.asList(param1));
        }
        if (randNum == 27 && MainOptions.mode == "neo4j") {
            IExpression param1 = generateUseVar(CypherType.NUMBER, null);
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(param1));
        }
        if (randNum == 28) {
            IExpression param1 = generateUseVar(CypherType.LIST, null);
            if (param1 instanceof ConstExpression)
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(param1));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SIZE_LIST, Arrays.asList(param1));
        }
        if (randNum == 29) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SIZE_STRING, Arrays.asList(param1));
        }
        if (randNum == 30 && MainOptions.mode != "kuzu") {
            IExpression param1 = generateUseVar(CypherType.BOOLEAN, null);
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOINTEGER_BOOLEAN, Arrays.asList(param1));
        }
        if (randNum == 31 && MainOptions.mode != "kuzu") {
            IExpression param1 = generateUseVar(CypherType.NUMBER, null);
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOINTEGER_INTEGER, Arrays.asList(param1));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ABS, Arrays.asList(param));
    }

    private IExpression generateNodeAgg() {
        Randomly randomly = new Randomly();
        long randNum = randomly.getInteger(0, 1);
        if (MainOptions.mode == "thinker")
            return new ConstExpression(true);
        if (randNum == 0) {
            IExpression param1 = generateUseVar(CypherType.RELATION, null);
            if (param1 instanceof ConstExpression)
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1L)));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.END_NODE, Arrays.asList(param1));
        }
        if (randNum == 1) {
            IExpression param1 = generateUseVar(CypherType.RELATION, null);
            if (param1 instanceof ConstExpression)
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1L)));
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.START_NODE, Arrays.asList(param1));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1L)));
    }

    private IExpression generateRelationAgg() {
        Randomly randomly = new Randomly();
        long randNum = randomly.getInteger(0, 0);
        if (MainOptions.mode == "thinker")
            return new ConstExpression(true);
        IExpression param = generateUseVar(CypherType.RELATION, null);
        if (param instanceof ConstExpression)
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(param));
        if (randNum == 0) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TYPE, Arrays.asList(param));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1)));
    }

    private IExpression generateBooleanAgg() {
        Randomly randomly = new Randomly();
        long randNum = 0;
        if (MainOptions.mode == "memgraph")
            randNum = randomly.getInteger(0, 3);
        else if (MainOptions.mode == "thinker")
            return new ConstExpression(true);
        else if (MainOptions.mode == "kuzu")
            randNum = randomly.getInteger(2, 3);
        else
            randNum = 3;
        if (randNum == 0) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            if (param1 == null) {
                param1 = generateStringConst(null);
            }
            IExpression param2 = null;
            if (Randomly.getBoolean()) {
                param2 = generateUseVar(CypherType.STRING, null);
            } else {
                String content = (String) ((CypherExpression) param1).getValue();
                long leftIndex = randomly.getInteger(0, content.length());
                long rightIndex = randomly.getInteger(leftIndex, content.length());
                param2 = new ConstExpression(content.substring((int) leftIndex, (int) rightIndex));
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.CONTAINS, Arrays.asList(param1, param2));
        }
        if (randNum == 1) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            if (param1 == null) {
                param1 = generateStringConst(null);
            }
            IExpression param2 = null;
            if (Randomly.getBoolean()) {
                param2 = generateUseVar(CypherType.STRING, null);
            } else {
                String content = (String) ((CypherExpression) param1).getValue();
                long index = randomly.getInteger(0, content.length());
                param2 = new ConstExpression(content.substring((int) index, content.length() - 1));
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.ENDSWITH, Arrays.asList(param1, param2));
        }
        if (randNum == 2) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            if (param1 == null) {
                param1 = generateStringConst(null);
            }
            IExpression param2 = null;
            if (Randomly.getBoolean()) {
                param2 = generateUseVar(CypherType.STRING, null);
            } else {
                String content = (String) ((CypherExpression) param1).getValue();
                long index = randomly.getInteger(0, content.length());
                param2 = new ConstExpression(content.substring(0, (int) index));
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.STARTSWITH, Arrays.asList(param1, param2));
        }
        if (randNum == 3) {
            IExpression param1 = generateUseVar(CypherType.NUMBER, null);
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOBOOLEAN_INTEGER, Arrays.asList(param1));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1)));
    }

    private IExpression generateListAgg() {
        Randomly randomly = new Randomly();
        long randNum = randomly.getInteger(0, 3);
        if (MainOptions.mode == "thinker")
            return new ConstExpression(true);
        if (randNum == 0) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.COLLECT, Arrays.asList(generateUseVar(CypherType.ANY, null)));
        }
        if (randNum == 1) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            if (param1 == null) {
                param1 = generateStringConst(null);
            }
            IExpression param2 = null;
            if (Randomly.getBoolean()) {
                param2 = generateUseVar(CypherType.STRING, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, new String(""), true));
            } else {
                String content = (String) ((CypherExpression) param1).getValue();
                long startindex = randomly.getInteger(0, content.length());
                long index = randomly.getInteger(startindex, content.length());
                if (startindex != index || MainOptions.mode != "memgraph")
                    param2 = new ConstExpression(content.substring((int) startindex, (int) index));
                else
                    param2 = generateStringConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, new String(""), true));
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SPLIT, Arrays.asList(param1, param2));
        }
        if (randNum == 2) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.COLLECT, Arrays.asList(generateUseVar(CypherType.ANY, null)));
        }
        if (randNum == 3) {
            IExpression param1 = generateUseVar(CypherType.NUMBER, null);
            if (param1 == null) {
                param1 = generateNumberConst(null);
            }
            IExpression param2 = generateUseVar(CypherType.NUMBER, null);
            if (param2 == null) {
                param2 = generateNumberConst(null);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.RANGE, Arrays.asList(param1, param2));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1)));
    }

    private IExpression generateStringAgg() {
        Randomly randomly = new Randomly();
        long randNum = randomly.getInteger(2, 16);
        if (MainOptions.mode == "thinker")
            randNum = randomly.getInteger(12, 13);
        else if (MainOptions.mode == "kuzu")
            randNum = randomly.getInteger(2, 14);
        long aggregationRandom = randomly.getInteger(0, 100);
        IExpression param = generateUseVar(CypherType.STRING, null);
        if (param == null) {
            param = generateStringConst(null);
        }
        if (randNum == 2) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            if (param1 == null) {
                param1 = generateStringConst(null);
            }
            IExpression param2 = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
            if (param2 == null) {
                param2 = generateNumberConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
            }
            if ((long) ((CypherExpression) param2).getValue() > 2147483640) {
                param2 = new ConstExpression(1L);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.LEFT, Arrays.asList(param1, param2));
        }
        if (randNum == 3) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.LTRIM, Arrays.asList(param));
        }
        if (randNum == 4) {
            if (MainOptions.mode == "kuzu")
                randNum = 5;
            else {
                IExpression param1 = generateUseVar(CypherType.STRING, null);
                if (param1 == null) {
                    param1 = generateStringConst(null);
                }
                IExpression param2 = null;
                if (Randomly.getBoolean()) {
                    param2 = generateUseVar(CypherType.STRING, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, new String(""), true));
                    if (param2 == null) {
                        param2 = generateStringConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, new String(""), true));
                    }
                } else {
                    String candidate = (String) ((CypherExpression) param1).getValue();
                    long leftIndex = randomly.getInteger(0, candidate.length());
                    long rightIndex = randomly.getInteger(leftIndex, candidate.length());
                    if (leftIndex != rightIndex || MainOptions.mode != "memgraph")
                        param2 = new ConstExpression(candidate.substring((int) leftIndex, (int) rightIndex));
                    else
                        param2 = generateStringConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, new String(""), true));
                }
                IExpression param3 = generateUseVar(CypherType.STRING, null);
                if (param3 == null) {
                    param3 = generateStringConst(null);
                }
                return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.REPLACE, Arrays.asList(param1, param2, param3));
            }
        }


        if (randNum == 5) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.REVERSE, Arrays.asList(param));
        }


        if (randNum == 6) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            if (param1 == null) {
                param1 = generateStringConst(null);
            }
            IExpression param2 = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
            if (param2 == null) {
                param2 = generateNumberConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
            }
            if ((long) ((CypherExpression) param2).getValue() < 0 || (long) ((CypherExpression) param2).getValue() > 2147483640) {
                param2 = new ConstExpression(1L);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.RIGHT, Arrays.asList(param1, param2));
        }


        if (randNum == 7) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.RTRIM, Arrays.asList(param));
        }

        if (randNum == 8) {
            IExpression param1 = generateUseVar(CypherType.STRING, null);
            if (param1 == null) {
                param1 = generateStringConst(null);
            }
            IExpression param2 = null;
            if (MainOptions.mode != "kuzu") {
                param2 = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
                if (param2 == null) {
                    param2 = generateNumberConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
                }
            } else {
                param2 = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 1L, true));
                if (param2 == null) {
                    param2 = generateNumberConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 1L, true));
                }
            }
            if ((long) ((CypherExpression) param2).getValue() < 0 || (long) ((CypherExpression) param2).getValue() > 2147483640) {
                param2 = new ConstExpression(1);
            }
            IExpression param3 = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
            if (param3 == null) {
                param3 = generateNumberConst(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, 0L, true));
            }
            if ((long) ((CypherExpression) param3).getValue() < 0 || (long) ((CypherExpression) param3).getValue() > 2147483640) {
                param3 = new ConstExpression(1);
            }
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUBSTRING, Arrays.asList(param1, param2, param3));
        }
        if (randNum == 9) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOLOWER, Arrays.asList(param));
        }


        if (randNum == 10) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOUPPER, Arrays.asList(param));
        }


        if (randNum == 11) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TRIM, Arrays.asList(param));
        }
        if (randNum == 12 || aggregationRandom > 50 && aggregationRandom <= 75) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MAX_STRING, Arrays.asList(param));
        }
        if (randNum == 13 || aggregationRandom > 75) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.MIN_STRING, Arrays.asList(param));
        }
        if (randNum == 14 && MainOptions.mode != "kuzu") {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOSTRING_STRING, Arrays.asList(param));
        }
        if (randNum == 15 && MainOptions.mode != "kuzu") {
            IExpression param1 = generateUseVar(CypherType.BOOLEAN, null);
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOSTRING_BOOLEAN, Arrays.asList(param1));
        }
        if (randNum == 16 && MainOptions.mode != "kuzu") {
            IExpression param1 = generateUseVar(CypherType.NUMBER, null);
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.TOSTRING_INTEGER, Arrays.asList(param1));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1)));
    }

    public IExpression generateFunction(CypherType type) {
        if (type == CypherType.NUMBER) {
            return generateNumberAgg();
        } else if (type == CypherType.LIST) {
            if (MainOptions.mode == "kuzu")
                return generateNumberAgg();
            return generateListAgg();
        } else if (type == CypherType.NODE) {
            if (MainOptions.mode == "kuzu")
                return generateNumberAgg();
            return generateNodeAgg();
        } else if (type == CypherType.RELATION) {
            if (MainOptions.mode == "kuzu")
                return generateStringAgg();
            return generateRelationAgg();
        } else if (type == CypherType.BOOLEAN) {
            return generateBooleanAgg();
        } else if (type == CypherType.ANY) {
            return generateAnyAgg();
        } else
            return generateStringAgg();
    }

    private IExpression generateAnyAgg() {
        long randNum = randomly.getInteger(0, 8);
        if (MainOptions.mode == "thinker")
            randNum = randomly.getInteger(0, 5);
        if (randNum == 0)
            return generateNumberAgg();
        if (randNum == 1)
            return generateStringAgg();
        if (randNum == 2)
            return generateBooleanAgg();
        if (randNum == 3)
            return generateListAgg();
        if (randNum == 4)
            return generateNodeAgg();
        if (randNum == 5)
            return generateRelationAgg();
        if (randNum == 6) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.HEAD, Arrays.asList(generateUseVar(CypherType.LIST, null)));
        }
        if (randNum == 7) {
            return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.LAST, Arrays.asList(generateUseVar(CypherType.LIST, null)));
        }
        return new CallExpression(Neo4jSchema.Neo4jBuiltInFunctions.SUM, Arrays.asList(new ConstExpression(1)));

    }

    private IExpression generateStringConst(ExpressionAssertion expressionAssertion) {
        if (expressionAssertion == null) {
            return new ConstExpression(randomly.getString());
        }

        if (expressionAssertion instanceof StringMatchingAssertion) {
            StringMatchingAssertion assertion = (StringMatchingAssertion) expressionAssertion;
            Object stringObj = assertion.getString();
            if (stringObj == ExprVal.UNKNOWN) {
                return new ConstExpression(randomly.getString());
            }
            String string = (String) stringObj;
            String candidate = "";
            switch (assertion.getOperation()) {
                case CONTAINS:
                    if (!assertion.isTarget()) {
                        String candidate1 = randomly.getString();
                        while (!assertion.check(candidate1)) {
                            candidate1 = randomly.getString();
                        }
                        return new ConstExpression(candidate1);
                    }
                    long leftIndex = randomly.getInteger(0, string.length());
                    long rightIndex = randomly.getInteger(leftIndex, string.length());
                    if (rightIndex - leftIndex < 4)
                        return new ConstExpression(string);
                    return new ConstExpression(string.substring((int) leftIndex, (int) rightIndex));
                case STARTS_WITH:
                    if (!assertion.isTarget()) {
                        String candidate1 = randomly.getString();
                        while (!assertion.check(candidate1)) {
                            candidate1 = randomly.getString();
                        }
                        return new ConstExpression(candidate1);
                    }
                    rightIndex = randomly.getInteger(0, string.length());
                    if (rightIndex < 4)
                        return new ConstExpression(string);
                    return new ConstExpression(string.substring(0, (int) randomly.getInteger(0L, (long) string.length())));
                case ENDS_WITH:
                    if (!assertion.isTarget()) {
                        String candidate1 = randomly.getString();
                        while (!assertion.check(candidate1)) {
                            candidate1 = randomly.getString();
                        }
                        return new ConstExpression(candidate1);
                    }
                    leftIndex = randomly.getInteger(0, string.length());
                    if (string.length() - leftIndex < 4)
                        return new ConstExpression(string);
                    return new ConstExpression(string.substring((int) randomly.getInteger(0, string.length()), string.length()));
            }
        }

        if (expressionAssertion instanceof ComparisonAssertion) {
            String candidate = "";
            ComparisonAssertion assertion = (ComparisonAssertion) expressionAssertion;
            if (assertion.getLeftOp() == ExprVal.UNKNOWN) {
                return new ConstExpression(randomly.getString());
            }
            if (assertion.getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.EQUAL && assertion.trueTarget() ||
                    assertion.getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL && !assertion.trueTarget()) {
                return new ConstExpression(assertion.getLeftOp());
            }

            candidate = new String(assertion.getLeftOp().toString());
            if (candidate.length() == 0 && assertion.getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.HIGHER && assertion.trueTarget()) {
                throw new RuntimeException("Trying to generate a string that is higher than an empty string");
            } else if (candidate.length() == 0) {
                String res = randomly.getString();
                if (assertion.check(res))
                    return new ConstExpression(res);
                else
                    return new ConstExpression(candidate);
            }

            int randomIndex = randomly.getInteger(0, candidate.length());
            char next = candidate.charAt(randomIndex);
            do {

                int possibleShrink = candidate.charAt(randomIndex) - 33;
                next = (char) (candidate.charAt(randomIndex) - randomly.getInteger(1, possibleShrink));
                while (!(next != '\\' && next != '\"' && next != '\'' && next != '\n' && next != '\r' && next != '\t' && next != '\b' && next != '\f' && next != '\0' && next != '\u001a' && next != '\u0007' && next != '\u0000')) {
                    next = (char) (next + 1);
                }
                if (next > candidate.charAt(randomIndex)) {
                    randomIndex = randomly.getInteger(0, candidate.length());
                } else {
                    break;
                }
            } while (true);
            String newCandidate = candidate.substring(0, randomIndex) + next + candidate.substring(randomIndex + 1);

            if (assertion.check(newCandidate)) {
                return new ConstExpression(newCandidate);
            }



            randomIndex = randomly.getInteger(0, candidate.length());
            do {

                int possibleExpand = 127 - candidate.charAt(randomIndex);
                next = (char) (candidate.charAt(randomIndex) + randomly.getInteger(1, possibleExpand));
                while (!(next != '\\' && next != '\"' && next != '\'' && next != '\n' && next != '\r' && next != '\t' && next != '\b' && next != '\f' && next != '\0' && next != '\u001a' && next != '\u0007' && next != '\u0000')) {
                    next = (char) (next - 1);
                }
                if (next < candidate.charAt(randomIndex)) {
                    randomIndex = randomly.getInteger(0, candidate.length());
                } else {
                    break;
                }
            } while (true);
            newCandidate = candidate.substring(0, randomIndex) + next + candidate.substring(randomIndex + 1);

            if (assertion.check(newCandidate)) {
                return new ConstExpression(newCandidate);
            }
            throw new RuntimeException("Cannot find a string in getRandomString that satisfies the condition" + assertion.toString());
        }

        return new ConstExpression(randomly.getString());
    }

    private IExpression generateNumberConst(ComparisonAssertion comparisonAssertion) {
        if (comparisonAssertion != null) {
            long leftOp = Long.parseLong(comparisonAssertion.getLeftOp().toString());
            BinaryComparisonExpression.BinaryComparisonOperation operation = comparisonAssertion.getOperation();
            if (!comparisonAssertion.trueTarget()) {
                operation = operation.reverse();
            }

            switch (operation) {
                case EQUAL:
                    return new ConstExpression(leftOp);
                case HIGHER:
                    if (MainOptions.mode == "falkordb") {
                        if (leftOp != Integer.MIN_VALUE) {
                            return new ConstExpression(randomly.getLong(Integer.MIN_VALUE, leftOp));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    } else {
                        if (leftOp != Long.MIN_VALUE) {
                            return new ConstExpression(randomly.getLong(Long.MIN_VALUE, leftOp));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    }
                case HIGHER_OR_EQUAL:
                    if (MainOptions.mode == "falkordb") {
                        if (leftOp != Integer.MIN_VALUE) {
                            return new ConstExpression(randomly.getLong(Integer.MIN_VALUE, max(leftOp + 1, leftOp)));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    } else {
                        if (leftOp != Long.MIN_VALUE) {
                            return new ConstExpression(randomly.getLong(Long.MIN_VALUE, max(leftOp + 1, leftOp)));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    }
                case SMALLER:
                    if (MainOptions.mode == "falkordb") {
                        if (leftOp != Integer.MAX_VALUE) {
                            return new ConstExpression(randomly.getLong(leftOp + 1, Integer.MAX_VALUE));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    } else {
                        if (leftOp != Long.MAX_VALUE) {
                            return new ConstExpression(randomly.getLong(leftOp + 1, Long.MAX_VALUE));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    }
                case SMALLER_OR_EQUAL:
                    if (MainOptions.mode == "falkordb") {
                        if (leftOp != Integer.MAX_VALUE) {
                            return new ConstExpression(randomly.getLong(leftOp, Integer.MAX_VALUE));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    } else {
                        if (leftOp != Long.MAX_VALUE) {
                            return new ConstExpression(randomly.getLong(leftOp, Long.MAX_VALUE));
                        } else {
                            return new ConstExpression(leftOp);
                        }
                    }
                case NOT_EQUAL: {
                    long x = 0;
                    if (MainOptions.mode == "falkordb")
                        x = randomly.getLong(Integer.MIN_VALUE, Integer.MAX_VALUE);
                    else
                        x = randomly.getLong(Long.MIN_VALUE, Long.MAX_VALUE);
                    if (randomly.getInteger(0, 100) < 50) {
                        x = x + 1;
                    }
                    if (x != leftOp) {
                        return new ConstExpression(x);
                    }
                    return new ConstExpression(x + 1);
                }
            }
        }
        long number = (long) randomly.getInteger();
        return new ConstExpression(number);
    }

    private IExpression generateBooleanConst(BooleanAssertion booleanAssertion) {
        if (booleanAssertion != null) {
            return new ConstExpression(booleanAssertion.getValue());
        }
        return new ConstExpression(randomly.getInteger(0, 100) < 50);
    }

    public IExpression generateCondition(long depth) {
        return booleanExpression(depth, new BooleanAssertion(true));
    }

    public IExpression generateSimpleEquationWithMatch(IMatchAnalyzer matchClause, List<Map<String, Object>> namespace) {
        IExpression result = new ConstExpression(true);
        List<IPattern> patterns = matchClause.getSource().getPatternTuple();
        for (IPattern pattern : patterns) {
            List<IPatternElement> elements = pattern.getPatternElements();
            for (IPatternElement element : elements) {
                if (element instanceof INodeIdentifier) {
                    NodeIdentifier nodeIdentifier = (NodeIdentifier) element;
                    AbstractNode node = nodeIdentifier.actualNode;
                    IExpression left = new GetPropertyExpression(new IdentifierExpression(nodeIdentifier), "id");
                    IExpression right = new ConstExpression(node.getId());
                    result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(left, right, BinaryComparisonExpression.BinaryComparisonOperation.EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                } else if (element instanceof IRelationIdentifier) {
                    RelationIdentifier relationIdentifier = (RelationIdentifier) element;
                    AbstractRelationship relation = relationIdentifier.actualRelationship;
                    IExpression left = new GetPropertyExpression(new IdentifierExpression(relationIdentifier), "id");
                    IExpression right = new ConstExpression(relation.getId());
                    result = new BinaryLogicalExpression(result, new BinaryComparisonExpression(left, right, BinaryComparisonExpression.BinaryComparisonOperation.EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                }
            }
        }
        return result;
    }

    public IExpression generateEquationWithMatch(IMatchAnalyzer matchClause, List<Map<String, Object>> namespace) {
        if (matchClause instanceof IMatchAnalyzer)
            matchClause = (IMatchAnalyzer) matchClause;
        if (namespace == null)
            throw new RuntimeException("namespace is null in match clause");
        if (MainOptions.exp.equals("expression") || MainOptions.exp.equals("both"))
            return generateSimpleEquationWithMatch(matchClause, namespace);
        IExpression returnExp = null;
        Randomly randomly = new Randomly();
        List<IExpression> leftThing = getAllUseVar(null);
        Map<IIdentifier, List<IExpression>> leftThingMap = new HashMap<>();
        Map<IExpression, String> leftThingToProperty = new HashMap<>();

        Map<String, Object> names = new HashMap<>();
        if (namespace.size() > 0)
            names = namespace.get(0);
        Set<Pair<String, AbstractNode>> definedNode = new HashSet<>();
        Set<Pair<String, AbstractRelationship>> definedRelation = new HashSet<>();
        for (String key : names.keySet()) {
            if (names.get(key) instanceof AbstractNode) {
                AbstractNode node = (AbstractNode) names.get(key);
                String curName = key;
                definedNode.add(new Pair<>(curName, node));
            } else if (names.get(key) instanceof AbstractRelationship) {
                AbstractRelationship relation = (AbstractRelationship) names.get(key);
                String curName = key;
                definedRelation.add(new Pair<>(curName, relation));
            }
        }



        List<IPattern> patterns = matchClause.getSource().getPatternTuple();


        for (IPattern pattern : patterns) {
            List<IPatternElement> elements = pattern.getPatternElements();
            IExpression roundExp = null;
            Set<IIdentifier> settled = new HashSet<>();
            for (int i = 0; i < elements.size(); i++) {
                roundExp = null;
                IIdentifier element = (IIdentifier) elements.get(i);
                IIdentifier previousElement = null;
                if (i > 0)
                    previousElement = (IIdentifier) elements.get(i - 1);
                Map<String, Object> properties = null;
                if (element instanceof INodeIdentifier) {
                    NodeIdentifier nodeIdentifier = (NodeIdentifier) element;
                    AbstractNode node = nodeIdentifier.actualNode;
                    properties = node.getProperties();

                    if (!Randomly.getBooleanWithLowerProbability()) {

                        String curName = nodeIdentifier.getName();
                        if (definedNode.contains(new Pair<>(curName, node))) {
                            settled.add(nodeIdentifier);
                            continue;
                        }
                    }



                    if (!Randomly.getBooleanWithLowerProbability() && previousElement != null && previousElement instanceof IRelationIdentifier) {
                        IRelationIdentifier previousRelation = (RelationIdentifier) previousElement;
                        if (settled.contains(previousRelation)) {
                            settled.add(nodeIdentifier);
                            continue;
                        }
                    }

                    if (Randomly.getBoolean() && nodeIdentifier.actualNode.getLabelInfos().size() > 0) {
                        ILabelInfo label = nodeIdentifier.actualNode.getLabelInfos().get((int) Randomly.getNotCachedInteger(0, nodeIdentifier.actualNode.getLabelInfos().size()));
                        if (nodeIdentifier.getLabels().contains(new Label(label.getName()))) {
                            if (((CypherSchema.CypherLabelInfo) label).indexedProp.size() > 0 && (node.getProperties().get(((CypherSchema.CypherLabelInfo) label).indexedProp.get(0)) instanceof String || node.getProperties().get(((CypherSchema.CypherLabelInfo) label).indexedProp.get(0)) instanceof Long)) {
                                properties = new HashMap<>();
                                properties.put(((CypherSchema.CypherLabelInfo) label).indexedProp.get(0), node.getProperties().get(((CypherSchema.CypherLabelInfo) label).indexedProp.get(0)));
                                ((Match) matchClause).indexNode = nodeIdentifier.getName();
                                ((Match) matchClause).indexLabel = nodeIdentifier.getLabels().get((int) Randomly.getNotCachedInteger(0, nodeIdentifier.getLabels().size())).getName();
                                ((Match) matchClause).indexProperty = ((CypherSchema.CypherLabelInfo) label).indexedProp.get(0);
                            }
                        }
                    }

                } else if (element instanceof IRelationIdentifier) {
                    RelationIdentifier relationIdentifier = (RelationIdentifier) element;
                    AbstractRelationship relation = relationIdentifier.actualRelationship;
                    properties = relation.getProperties();
                    if (!Randomly.getBooleanWithSmallProbability()) {

                        String curName = relationIdentifier.getName();
                        if (definedRelation.contains(new Pair<>(curName, relation))) {
                            settled.add(relationIdentifier);
                            continue;
                        }
                    }

                    if (!Randomly.getBooleanWithLowerProbability() && previousElement != null && previousElement instanceof INodeIdentifier) {
                        INodeIdentifier previousNode = (NodeIdentifier) previousElement;
                        if (settled.contains(previousNode)) {
                            if (((NodeIdentifier) previousNode).actualNode.getRelationships().size() == 1) {
                                settled.add(relationIdentifier);
                                continue;
                            } else {
                                int cnt = 0;
                                if (((RelationIdentifier) element).getDirection() == Direction.RIGHT)
                                    for (AbstractRelationship rel : ((NodeIdentifier) previousNode).actualNode.getRelationships()) {
                                        if (rel.getFrom().equals(((NodeIdentifier) previousNode).actualNode))
                                            cnt++;
                                    }
                                else if (((RelationIdentifier) element).getDirection() == Direction.LEFT)
                                    for (AbstractRelationship rel : ((NodeIdentifier) previousNode).actualNode.getRelationships()) {
                                        if (rel.getTo().equals(((NodeIdentifier) previousNode).actualNode))
                                            cnt++;
                                    }
                                else if (((RelationIdentifier) element).getDirection() == Direction.BOTH) {
                                    AbstractRelationship prepre = null;
                                    if (i > 2 && elements.get(i - 2) instanceof IRelationIdentifier)
                                        prepre = ((RelationIdentifier) elements.get(i - 2)).actualRelationship;
                                    for (AbstractRelationship rel : ((NodeIdentifier) previousNode).actualNode.getRelationships()) {
                                        if ((prepre == null || !prepre.equals(rel)))
                                            cnt++;
                                    }
                                }
                                if (cnt == 1) {
                                    settled.add(relationIdentifier);
                                    continue;
                                }
                            }
                        }
                    }

                    if (Randomly.getBoolean() && relationIdentifier.actualRelationship.getType() != null) {
                        IRelationTypeInfo label = relationIdentifier.actualRelationship.getType();
                        if (relationIdentifier.getTypes().contains(new RelationType(label.getName()))) {
                            if (((CypherSchema.CypherRelationTypeInfo) label).indexedProp.size() > 0 && (relation.getProperties().get(((CypherSchema.CypherRelationTypeInfo) label).indexedProp.get(0)) instanceof String || relation.getProperties().get(((CypherSchema.CypherRelationTypeInfo) label).indexedProp.get(0)) instanceof Long)) {
                                properties = new HashMap<>();
                                properties.put(((CypherSchema.CypherRelationTypeInfo) label).indexedProp.get(0), relation.getProperties().get(((CypherSchema.CypherRelationTypeInfo) label).indexedProp.get(0)));
                                ((Match) matchClause).indexNode = relationIdentifier.getName();
                                ((Match) matchClause).indexLabel = relationIdentifier.actualRelationship.getType().getName();
                                ((Match) matchClause).indexProperty = ((CypherSchema.CypherRelationTypeInfo) label).indexedProp.get(0);
                            }
                        }

                    }

                } else {
                    throw new RuntimeException();
                }


                if (element instanceof INodeIdentifier) {
                    roundExp = generateEquationForSingleElement(((NodeIdentifier) element).actualNode, element.getName());
                    settled.add(element);
                } else if (element instanceof IRelationIdentifier) {
                    boolean ruleoutsuccess = true;
                    if (!Randomly.getBooleanWithRatherLowProbability() && previousElement != null && previousElement instanceof INodeIdentifier) {

                        for (AbstractRelationship ruleout : ((NodeIdentifier) previousElement).actualNode.getRelationships()) {
                            if (ruleout != ((RelationIdentifier) element).actualRelationship) {
                                if (roundExp == null) {

                                    roundExp = generateEquationForSingleElement(((RelationIdentifier) element).actualRelationship, element.getName());
                                    if (roundExp == null) {
                                        roundExp = new ConstExpression(true);
                                        ruleoutsuccess = false;
                                        break;
                                    }
                                } else {

                                    IExpression tempExp = generateEquationForSingleElement(((RelationIdentifier) element).actualRelationship, element.getName());
                                    if (tempExp == null) {
                                        roundExp = new ConstExpression(true);
                                        ruleoutsuccess = false;
                                        break;
                                    }
                                    roundExp = new BinaryLogicalExpression(roundExp, tempExp, BinaryLogicalExpression.BinaryLogicalOperation.AND);

                                }
                            }
                        }
                    } else {
                        roundExp = generateEquationForSingleElement(((RelationIdentifier) element).actualRelationship, element.getName());
                        settled.add(element);
                    }
                    if (!ruleoutsuccess) {
                        roundExp = generateEquationForSingleElement(((RelationIdentifier) element).actualRelationship, element.getName());
                        settled.add(element);
                    }

                } else
                    throw new RuntimeException("In Match clause generate equation, element is not a node or a relation");


                if (returnExp == null) {
                    returnExp = roundExp;
                } else {
                    if (roundExp != null)
                        returnExp = new BinaryLogicalExpression(returnExp, roundExp, BinaryLogicalExpression.BinaryLogicalOperation.AND);
                }
            }

        }
        if (returnExp == null) {
            returnExp = new ConstExpression(true);
        }
        return returnExp;

    }

    public IExpression generateListWithBasicType(long depth, CypherType type) {
        Randomly randomly = new Randomly();
        long randomNum = randomly.getInteger(1, 4);
        List<IExpression> expressions = new ArrayList<>();
        if (MainOptions.mode == "thinker" && type == CypherType.ANY)
            type = Randomly.fromList(Arrays.asList(CypherType.STRING, CypherType.NUMBER, CypherType.BOOLEAN));
        Set a = null;
        if (type == CypherType.STRING)
            a = new HashSet<String>();
        else if (type == CypherType.NUMBER)
            a = new HashSet<Long>();
        else if (type == CypherType.BOOLEAN)
            a = new HashSet<Boolean>();
        for (int i = 0; i < randomNum; i++) {

            if (type == CypherType.ANY)
                type = Randomly.fromList(Arrays.asList(CypherType.STRING, CypherType.NUMBER, CypherType.BOOLEAN));
            CypherExpression expression = (CypherExpression) basicTypeExpression(depth, type);
            if (MainOptions.mode == "thinker") {
                if (!a.contains(expression.getValue())) {
                    expressions.add(expression);
                    a.add(expression.getValue());
                }
            } else {
                expressions.add(expression);
            }
        }
        return new CreateListExpression(expressions);
    }

    public IExpression generateListWithFixedType(long depth, CypherType type) {
        Randomly randomly = new Randomly();
        long randomNum = randomly.getInteger(1, 4);
        List<IExpression> expressions = new ArrayList<>();
        for (int i = 0; i < randomNum; i++) {

            expressions.add(new ConstExpression(1));
        }
        expressions.add(new ConstExpression(2));
        return new CreateListExpression(expressions);
    }

    private boolean duplicateRelied(Set<IIdentifier> reliedContent, Object element) {
        for (IIdentifier id : reliedContent) {
            if (id instanceof NodeIdentifier && element instanceof AbstractNode) {
                if (((NodeIdentifier) id).actualNode.equals(element))
                    return true;
            } else if (id instanceof RelationIdentifier && element instanceof AbstractRelationship) {
                if (((RelationIdentifier) id).actualRelationship.equals(element))
                    return true;
            }
        }
        return false;
    }

    private IExpression basicTypeExpression(long depth, CypherType type) {
        switch (type) {
            case BOOLEAN:
                return booleanExpression(depth, null);
            case STRING:
                return stringExpression(depth, null);
            case NUMBER:
                return numberExpression(depth, null);
            default:
                return null;
        }
    }

    public IExpression generateEquationForRuleOutElement(Object element, String name, Object keep) {
        Map<String, Object> properties = null;
        IExpression elementExp = null;
        AbstractRelationship keepRelation = null;
        if (element instanceof AbstractNode) {
            AbstractNode node = (AbstractNode) element;
            properties = node.getProperties();
            elementExp = new IdentifierExpression(new NodeIdentifier(name, node));
        } else if (element instanceof AbstractRelationship) {
            AbstractRelationship relation = (AbstractRelationship) element;
            properties = relation.getProperties();
            elementExp = new IdentifierExpression(new RelationIdentifier(name, relation));
            keepRelation = (AbstractRelationship) keep;
        } else {
            throw new RuntimeException();
        }
        List<String> keyList = new ArrayList<>(properties.keySet());
        Randomly randomly = new Randomly();
        String randomKey = keyList.get((int) randomly.getInteger(0, keyList.size()));
        IExpression roundExp = new ConstExpression(false);
        if (properties.get(randomKey) instanceof Boolean) {
            int cnt = 0;
            for (String key : properties.keySet()) {
                if (properties.get(key) instanceof Boolean)
                    cnt++;
                else {
                    randomKey = key;
                    break;
                }
            }
        }
        if (properties.get(randomKey) instanceof Long || properties.get(randomKey) instanceof Integer) {
            long randomOp = randomly.getInteger(0, 100);
            if (randomKey.equals("id"))
                randomOp = 1;
            if (((AbstractRelationship) keep).getProperties().containsKey(randomKey) && keepRelation.getProperties().get(randomKey) instanceof Long) {
                Long value = (Long) keepRelation.getProperties().get(randomKey);
                if (value > (Long) properties.get(randomKey))
                {
                    IExpression right = generateUseVar(CypherType.NUMBER, null, new ArrayList<>(Arrays.asList(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, (Long) properties.get(randomKey), true), new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.HIGHER, value, true))));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        right = new ConstExpression(randomly.getLong((Long) properties.get(randomKey), value - 1));
                    }
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.HIGHER);
                } else if (value != (Long) properties.get(randomKey)) {
                    IExpression right = generateUseVar(CypherType.NUMBER, null, new ArrayList<>(Arrays.asList(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.HIGHER, (Long) properties.get(randomKey), true), new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, value, true))));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        right = new ConstExpression(randomly.getLong(value + 1, (Long) properties.get(randomKey)));
                    }
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.SMALLER);
                } else {
                    return null;
                }
            } else {
                long upperlimit = Long.MAX_VALUE, lowerlimit = Long.MIN_VALUE;
                if (MainOptions.mode == "falkordb") {
                    upperlimit = Integer.MAX_VALUE;
                    lowerlimit = Integer.MIN_VALUE;
                }
                if (randomOp < 30)
                {
                    IExpression right = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.EQUAL, properties.get(randomKey), true));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        right = new ConstExpression((Long) properties.get(randomKey));
                    }
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL);
                } else if (randomOp < 60)
                {
                    IExpression right = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, properties.get(randomKey), true));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        right = new ConstExpression(randomly.getLong((Long) properties.get(randomKey), upperlimit));
                    }
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.HIGHER);
                } else
                {
                    IExpression right = generateUseVar(CypherType.NUMBER, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.HIGHER, properties.get(randomKey), true));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        right = new ConstExpression(randomly.getLong(lowerlimit, (Long) properties.get(randomKey)));
                    }
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.SMALLER);
                }
            }
        } else if (properties.get(randomKey) instanceof String) {
            long randomOp = randomly.getInteger(0, 100);
            if (MainOptions.mode == "falkordb")
            {
                randomOp = randomly.getInteger(0, 70);
            }
            if (randomOp < 10)
            {
                roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(properties.get(randomKey)), BinaryComparisonExpression.BinaryComparisonOperation.NOT_EQUAL);
            } else if (randomOp < 30)
            {
                if (((AbstractRelationship) keep).getProperties().containsKey(randomKey) && keepRelation.getProperties().get(randomKey) instanceof String) {
                    String value = (String) keepRelation.getProperties().get(randomKey);
                    IExpression right = generateUseVar(CypherType.STRING, null, new ArrayList<>(Arrays.asList(new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.STARTS_WITH, properties.get(randomKey), false), new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.STARTS_WITH, value, true))));
                    if (right == null || ((CypherExpression) right).getValue().equals(value) || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        String rightString = value.substring(0, (int) randomly.getInteger(0, value.length() - 1));
                        if (rightString.length() < 3)
                            rightString = value;
                        if (((String) properties.get(randomKey)).startsWith(rightString))
                            right = new ConstExpression(value);
                        else
                            right = new ConstExpression(rightString);
                    }
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), right, StringMatchingExpression.StringMatchingOperation.STARTS_WITH);
                } else {
                    IExpression right = generateUseVar(CypherType.STRING, new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.STARTS_WITH, properties.get(randomKey), false));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        String rightString = (String) properties.get(randomKey);
                        String random = randomly.getString();
                        while (rightString.startsWith(random)) {
                            random = randomly.getString();
                        }
                        right = new ConstExpression(random);
                    }
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), right, StringMatchingExpression.StringMatchingOperation.STARTS_WITH);
                }
            } else if (randomOp < 50 && !MainOptions.mode.equals("kuzu"))
            {
                if (((AbstractRelationship) keep).getProperties().containsKey(randomKey) && keepRelation.getProperties().get(randomKey) instanceof String) {
                    String value = (String) keepRelation.getProperties().get(randomKey);
                    IExpression right = generateUseVar(CypherType.STRING, null, new ArrayList<>(Arrays.asList(new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.ENDS_WITH, properties.get(randomKey), false), new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.ENDS_WITH, value, true))));
                    if (right == null || ((CypherExpression) right).getValue().equals(value) || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        if (value.length() < 5)
                            right = new ConstExpression(value);
                        else {
                            String rightString = value.substring((int) randomly.getInteger(0, value.length() - 3), value.length());
                            if (((String) properties.get(randomKey)).endsWith(rightString))
                                right = new ConstExpression(value);
                            else
                                right = new ConstExpression(rightString);
                        }
                    }
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), right, StringMatchingExpression.StringMatchingOperation.ENDS_WITH);
                } else {
                    IExpression right = generateUseVar(CypherType.STRING, new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.ENDS_WITH, properties.get(randomKey), false));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        String rightString = (String) properties.get(randomKey);
                        String random = randomly.getString();
                        while (rightString.endsWith(random)) {
                            random = randomly.getString();
                        }
                        right = new ConstExpression(random);
                    }
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), right, StringMatchingExpression.StringMatchingOperation.ENDS_WITH);
                }
            } else if (randomOp < 70)
            {
                if (((AbstractRelationship) keep).getProperties().containsKey(randomKey) && keepRelation.getProperties().get(randomKey) instanceof String) {
                    String value = (String) keepRelation.getProperties().get(randomKey);
                    IExpression right = generateUseVar(CypherType.STRING, null, new ArrayList<>(Arrays.asList(new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.CONTAINS, properties.get(randomKey), false), new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.CONTAINS, value, true))));
                    if (right == null || ((CypherExpression) right).getValue().equals(value) || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        if (value.length() < 5)
                            right = new ConstExpression(value);
                        else {
                            int startIndex = (int) randomly.getInteger(0, value.length() - 4);
                            int endIndex = (int) randomly.getInteger(startIndex, randomly.getInteger(startIndex + 3, value.length()));
                            if (((String) properties.get(randomKey)).contains(value.substring(startIndex, endIndex)))
                                right = new ConstExpression(value);
                            else
                                right = new ConstExpression(value.substring(startIndex, endIndex));
                        }
                    }
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), right, StringMatchingExpression.StringMatchingOperation.CONTAINS);
                } else {
                    IExpression right = generateUseVar(CypherType.STRING, new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.CONTAINS, properties.get(randomKey), false));
                    if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                        String rightString = (String) properties.get(randomKey);
                        String random = randomly.getString();
                        while (rightString.contains(random)) {
                            random = randomly.getString();
                        }
                        right = new ConstExpression(random);
                    }
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), right, StringMatchingExpression.StringMatchingOperation.CONTAINS);
                }
            } else {
                if (((AbstractRelationship) keep).getProperties().containsKey(randomKey) && keepRelation.getProperties().get(randomKey) instanceof String) {
                    String value = (String) keepRelation.getProperties().get(randomKey);
                    if (value.compareTo((String) properties.get(randomKey)) > 0)
                    {
                        IExpression right = generateUseVar(CypherType.STRING, null, new ArrayList<>(Arrays.asList(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, (String) properties.get(randomKey), true), new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.HIGHER, value, true))));
                        if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                            right = new ConstExpression(randomly.getString((String) properties.get(randomKey), value));
                        }
                        roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.HIGHER_OR_EQUAL);
                    } else {
                        IExpression right = generateUseVar(CypherType.STRING, null, new ArrayList<>(Arrays.asList(new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.HIGHER, (String) properties.get(randomKey), true), new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, value, true))));
                        if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                            right = new ConstExpression(randomly.getString(value, (String) properties.get(randomKey)));
                        }
                        roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.SMALLER_OR_EQUAL);
                    }
                } else {
                    if (randomOp < 85) {
                        IExpression right = generateUseVar(CypherType.STRING, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.SMALLER, properties.get(randomKey), true));
                        if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                            right = new ConstExpression(((CypherExpression) right).getValue());
                        }
                        roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.HIGHER);
                    } else {
                        IExpression right = generateUseVar(CypherType.STRING, new ComparisonAssertion(BinaryComparisonExpression.BinaryComparisonOperation.HIGHER, properties.get(randomKey), true));
                        if (right == null || duplicateRelied(((CypherExpression) right).reliedContent(), keep)) {
                            right = new ConstExpression(((CypherExpression) right).getValue());
                        }
                        roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), right, BinaryComparisonExpression.BinaryComparisonOperation.SMALLER);
                    }
                }
            }
        } else {
            throw new RuntimeException();
        }
        if (!((AbstractRelationship) keep).getProperties().containsKey(randomKey)) {
            roundExp = new BinaryLogicalExpression(roundExp, new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(null), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.OR);
        }
        return roundExp;
    }

    private IExpression generateUseVar(CypherType type, ExpressionAssertion assertion) {
        Randomly randomly = new Randomly();

        List<IExpression> availableExpressions = new ArrayList<>();


        List<IAliasAnalyzer> aliasAnalyzers;
        if (clauseAnalyzer instanceof With && MainOptions.mode == "memgraph")
        {
            aliasAnalyzers = clauseAnalyzer.getAvailableAliases();
        } else if (clauseAnalyzer instanceof With && ((With) clauseAnalyzer).usedAggregation == false && ((With) clauseAnalyzer).isDistinct() == false) {
            aliasAnalyzers = ((With) clauseAnalyzer).getExtendableAliases();
        } else {
            aliasAnalyzers = clauseAnalyzer.getAvailableAliases();
        }
        availableExpressions.addAll(aliasAnalyzers.stream().filter(a -> a.analyzeType(schema).getType() == type).map(a -> new IdentifierExpression(Alias.createIdentifierRef(a, a.getExpression())))
                .collect(Collectors.toList()));
        List<INodeAnalyzer> nodeAnalyzers;
        if (clauseAnalyzer instanceof With && MainOptions.mode == "memgraph") {
            nodeAnalyzers = clauseAnalyzer.getAvailableNodeIdentifiers();
        } else if (clauseAnalyzer instanceof With && ((With) clauseAnalyzer).usedAggregation == false && ((With) clauseAnalyzer).isDistinct() == false) {
            nodeAnalyzers = ((With) clauseAnalyzer).getExtendableNodeIdentifiers();
        } else {
            nodeAnalyzers = clauseAnalyzer.getAvailableNodeIdentifiers();
        }




        for (IAliasAnalyzer aliasAnalyzer : aliasAnalyzers) {
            if (aliasAnalyzer.analyzeType(schema).getType() == CypherType.NODE) {
                NodeAnalyzer nodeAnalyzer = (NodeAnalyzer) aliasAnalyzer.analyzeType(schema).getNodeAnalyzer();
                if (nodeAnalyzer != null)
                    nodeAnalyzers.add(nodeAnalyzer);
                else {
                    if (aliasAnalyzer.getExpression() instanceof CallExpression) {
                        Object temp = ((CallExpression) aliasAnalyzer.getExpression()).getValue();
                        if (temp instanceof AbstractNode) {
                            nodeAnalyzers.add(new NodeAnalyzer(new NodeIdentifier(aliasAnalyzer.getName(), (AbstractNode) temp)));
                        }
                    } else {
                        System.out.println("Unknown alias analyzer with null node analyzer");
                    }
                }

            }
        }
        List<IRelationAnalyzer> relationAnalyzers;
        if (clauseAnalyzer instanceof With && MainOptions.mode == "memgraph") {
            relationAnalyzers = clauseAnalyzer.getAvailableRelationIdentifiers();
        } else if (clauseAnalyzer instanceof With && ((With) clauseAnalyzer).usedAggregation == false && ((With) clauseAnalyzer).isDistinct() == false) {
            relationAnalyzers = ((With) clauseAnalyzer).getExtendableRelationIdentifiers();
        } else {
            relationAnalyzers = clauseAnalyzer.getAvailableRelationIdentifiers();
        }




        for (IAliasAnalyzer aliasAnalyzer : aliasAnalyzers) {
            if (aliasAnalyzer.analyzeType(schema).getType() == CypherType.RELATION) {
                RelationAnalyzer relationAnalyzer = (RelationAnalyzer) aliasAnalyzer.analyzeType(schema).getRelationAnalyzer();
                if (relationAnalyzer != null)
                    relationAnalyzers.add(relationAnalyzer);
                else {
                    if (aliasAnalyzer.getExpression() instanceof CallExpression) {
                        Object temp = ((CallExpression) aliasAnalyzer.getExpression()).getValue();
                        if (temp instanceof AbstractRelationship) {
                            relationAnalyzers.add(new RelationAnalyzer(new RelationIdentifier(aliasAnalyzer.getName(), (AbstractRelationship) temp)));
                        }
                    } else {
                        System.out.println("Unknown alias analyzer with null relation analyzer");
                    }
                }

            }
        }

        nodeAnalyzers.stream().forEach(
                n -> {
                    n.getAllPropertiesWithType(schema, type).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(n),
                                        p.getKey()));
                            }
                    );
                }
        );
        if (type == CypherType.NODE) {
            nodeAnalyzers.stream().forEach(
                    n -> {

                        availableExpressions.add(new IdentifierExpression(n));

                    }
            );
        }

        relationAnalyzers.stream().forEach(
                r -> {
                    r.getAllPropertiesWithType(schema, type).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(r),
                                        p.getKey()));
                            }
                    );
                }
        );
        if (type == CypherType.RELATION) {
            relationAnalyzers.stream().forEach(
                    n -> {

                        availableExpressions.add(new IdentifierExpression(n));

                    }
            );
        }

        List<IExpression> checkedAvailableExpressions = availableExpressions.stream().filter(e -> {
            if (e instanceof CreateListExpression)
                return false;
            if (e instanceof IdentifierExpression) {
                if (((IdentifierExpression) e).getIdentifier() instanceof Alias) {
                    if (((Alias) ((IdentifierExpression) e).getIdentifier()).getExpression() instanceof CreateListExpression)
                        return false;
                    Alias temp = (Alias) ((IdentifierExpression) e).getIdentifier();
                    if (temp.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) temp.getExpression()).functionName) && ((CallExpression) temp.getExpression()).getElementValue() != null && ((CallExpression) temp.getExpression()).getElementValue().size() > 1)
                        return false;
                    else if (MainOptions.mode == "thinker" && temp.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) temp.getExpression()).functionName))
                        return false;
                    else {
                        if (assertion == null)
                            return true;
                        else
                            return assertion.check(e.getValue(varToProperties));
                    }

                } else {

                }
            }
            if (e.getValue(varToProperties) == null) {
                return false;
            }
            if (assertion == null) {
                return true;
            }
            if (assertion instanceof StringMatchingAssertion || assertion instanceof ComparisonAssertion) {
                if (assertion instanceof StringMatchingAssertion) {
                    if (((StringMatchingAssertion) assertion).getString().equals(e)) {
                        return false;
                    }
                }
                if (assertion instanceof ComparisonAssertion) {
                    if (((ComparisonAssertion) assertion).getOperation() == BinaryComparisonExpression.BinaryComparisonOperation.EQUAL && ((ComparisonAssertion) assertion).getLeftOp().equals(e)) {
                        return false;
                    }
                }
            }
            return assertion.check(e.getValue(varToProperties));
        }).collect(Collectors.toList());

        if (checkedAvailableExpressions.size() == 0) {
            switch (type) {
                case BOOLEAN:
                    return generateBooleanConst((BooleanAssertion) assertion);
                case NUMBER:
                    return generateNumberConst((ComparisonAssertion) assertion);
                case STRING:
                    return generateStringConst(assertion);
                default:
                    return generateNumberConst((ComparisonAssertion) assertion);
            }
        }

        return checkedAvailableExpressions.get((int) randomly.getInteger(0, checkedAvailableExpressions.size()));
    }

    private IExpression generateUseVar(CypherType type, Object holder, List<ExpressionAssertion> assertion) {
        Randomly randomly = new Randomly();

        List<IExpression> availableExpressions = new ArrayList<>();


        List<IAliasAnalyzer> aliasAnalyzers;
        if (clauseAnalyzer instanceof With && ((With) clauseAnalyzer).usedAggregation == false && ((With) clauseAnalyzer).isDistinct() == false) {
            aliasAnalyzers = ((With) clauseAnalyzer).getExtendableAliases();
        } else {
            aliasAnalyzers = clauseAnalyzer.getAvailableAliases();
        }
        availableExpressions.addAll(aliasAnalyzers.stream().filter(a -> a.analyzeType(schema).getType() == type).map(a -> new IdentifierExpression(Alias.createIdentifierRef(a, a.getExpression())))
                .collect(Collectors.toList()));
        List<INodeAnalyzer> nodeAnalyzers;
        if (clauseAnalyzer instanceof With && ((With) clauseAnalyzer).usedAggregation == false && ((With) clauseAnalyzer).isDistinct() == false) {
            nodeAnalyzers = ((With) clauseAnalyzer).getExtendableNodeIdentifiers();
        } else {
            nodeAnalyzers = clauseAnalyzer.getAvailableNodeIdentifiers();
        }




        for (IAliasAnalyzer aliasAnalyzer : aliasAnalyzers) {
            if (aliasAnalyzer.analyzeType(schema).getType() == CypherType.NODE) {
                NodeAnalyzer nodeAnalyzer = (NodeAnalyzer) aliasAnalyzer.analyzeType(schema).getNodeAnalyzer();
                if (nodeAnalyzer != null)
                    nodeAnalyzers.add(nodeAnalyzer);
                else {
                    if (aliasAnalyzer.getExpression() instanceof CallExpression) {
                        Object temp = ((CallExpression) aliasAnalyzer.getExpression()).getValue();
                        if (temp instanceof AbstractNode) {
                            nodeAnalyzers.add(new NodeAnalyzer(new NodeIdentifier(aliasAnalyzer.getName(), (AbstractNode) temp)));
                        }
                    } else {
                        System.out.println("Unknown alias analyzer with null node analyzer");
                    }
                }

            }
        }
        List<IRelationAnalyzer> relationAnalyzers;
        if (clauseAnalyzer instanceof With && ((With) clauseAnalyzer).usedAggregation == false && ((With) clauseAnalyzer).isDistinct() == false) {
            relationAnalyzers = ((With) clauseAnalyzer).getExtendableRelationIdentifiers();
        } else {
            relationAnalyzers = clauseAnalyzer.getAvailableRelationIdentifiers();
        }




        for (IAliasAnalyzer aliasAnalyzer : aliasAnalyzers) {
            if (aliasAnalyzer.analyzeType(schema).getType() == CypherType.RELATION) {
                RelationAnalyzer relationAnalyzer = (RelationAnalyzer) aliasAnalyzer.analyzeType(schema).getRelationAnalyzer();
                if (relationAnalyzer != null)
                    relationAnalyzers.add(relationAnalyzer);
                else {
                    if (aliasAnalyzer.getExpression() instanceof CallExpression) {
                        Object temp = ((CallExpression) aliasAnalyzer.getExpression()).getValue();
                        if (temp instanceof AbstractRelationship) {
                            relationAnalyzers.add(new RelationAnalyzer(new RelationIdentifier(aliasAnalyzer.getName(), (AbstractRelationship) temp)));
                        }
                    } else {
                        System.out.println("Unknown alias analyzer with null relation analyzer");
                    }
                }

            }
        }

        nodeAnalyzers.stream().forEach(
                n -> {
                    n.getAllPropertiesWithType(schema, type).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(n),
                                        p.getKey()));
                            }
                    );
                }
        );
        if (type == CypherType.NODE) {
            nodeAnalyzers.stream().forEach(
                    n -> {

                        availableExpressions.add(new IdentifierExpression(n));

                    }
            );
        }

        relationAnalyzers.stream().forEach(
                r -> {
                    r.getAllPropertiesWithType(schema, type).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(r),
                                        p.getKey()));
                            }
                    );
                }
        );
        if (type == CypherType.RELATION) {
            relationAnalyzers.stream().forEach(
                    n -> {

                        availableExpressions.add(new IdentifierExpression(n));

                    }
            );
        }

        List<IExpression> checkedAvailableExpressions = availableExpressions.stream().filter(e -> {
            if (e instanceof CreateListExpression)
                return false;
            if (e instanceof IdentifierExpression) {
                if (((IdentifierExpression) e).getIdentifier() instanceof Alias) {
                    if (((Alias) ((IdentifierExpression) e).getIdentifier()).getExpression() instanceof CreateListExpression)
                        return false;
                    Alias temp = (Alias) ((IdentifierExpression) e).getIdentifier();
                    if (temp.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) temp.getExpression()).functionName) && ((CallExpression) temp.getExpression()).getElementValue() != null && ((CallExpression) temp.getExpression()).getElementValue().size() > 1)
                        return false;
                    else {
                        if (assertion == null)
                            return true;
                        else {
                            for (ExpressionAssertion eA : assertion) {
                                if (!eA.check(e.getValue(varToProperties)))
                                    return false;
                            }
                            return true;
                        }
                    }

                } else {

                }
            }
            if (e.getValue(varToProperties) == null) {
                return false;
            }
            if (assertion == null) {
                return true;
            }
            for (ExpressionAssertion eA : assertion) {
                if (!eA.check(e.getValue(varToProperties)))
                    return false;
            }
            return true;
        }).collect(Collectors.toList());

        if (checkedAvailableExpressions.size() == 0) {
            return null;
        }

        return checkedAvailableExpressions.get((int) randomly.getInteger(0, checkedAvailableExpressions.size()));
    }

    private List<IExpression> getAllUseVar(ExpressionAssertion assertion) {
        List<IExpression> availableExpressions = new ArrayList<>();


        List<IAliasAnalyzer> aliasAnalyzers = clauseAnalyzer.getAvailableAliases();
        availableExpressions.addAll(aliasAnalyzers.stream().filter(a -> a.analyzeType(schema).getType() == CypherType.NUMBER).map(a -> new IdentifierExpression(Alias.createIdentifierRef(a, a.getExpression())))
                .collect(Collectors.toList()));
        availableExpressions.addAll(aliasAnalyzers.stream().filter(a -> a.analyzeType(schema).getType() == CypherType.STRING).map(a -> new IdentifierExpression(Alias.createIdentifierRef(a, a.getExpression())))
                .collect(Collectors.toList()));


        List<INodeAnalyzer> nodeAnalyzers = clauseAnalyzer.getAvailableNodeIdentifiers();

        for (IAliasAnalyzer aliasAnalyzer : aliasAnalyzers) {
            if (aliasAnalyzer.analyzeType(schema).getType() == CypherType.NODE) {
                NodeAnalyzer nodeAnalyzer = (NodeAnalyzer) aliasAnalyzer.analyzeType(schema).getNodeAnalyzer();
                if (nodeAnalyzer != null)
                    nodeAnalyzers.add(nodeAnalyzer);
                else {
                    if (aliasAnalyzer.getExpression() instanceof CallExpression) {
                        Object temp = ((CallExpression) aliasAnalyzer.getExpression()).getValue();
                        if (temp instanceof AbstractNode) {
                            nodeAnalyzers.add(new NodeAnalyzer(new NodeIdentifier(aliasAnalyzer.getName(), (AbstractNode) temp)));
                        }
                    } else {
                        System.out.println("Unknown alias analyzer with null node analyzer");
                    }
                }

            }
        }

        List<IRelationAnalyzer> relationAnalyzers = clauseAnalyzer.getAvailableRelationIdentifiers();


        for (IAliasAnalyzer aliasAnalyzer : aliasAnalyzers) {
            if (aliasAnalyzer.analyzeType(schema).getType() == CypherType.RELATION) {
                RelationAnalyzer relationAnalyzer = (RelationAnalyzer) aliasAnalyzer.analyzeType(schema).getRelationAnalyzer();
                if (relationAnalyzer != null)
                    relationAnalyzers.add(relationAnalyzer);
                else {
                    if (aliasAnalyzer.getExpression() instanceof CallExpression) {
                        Object temp = ((CallExpression) aliasAnalyzer.getExpression()).getValue();
                        if (temp instanceof AbstractRelationship) {
                            relationAnalyzers.add(new RelationAnalyzer(new RelationIdentifier(aliasAnalyzer.getName(), (AbstractRelationship) temp)));
                        }
                    } else {
                        System.out.println("Unknown alias analyzer with null relation analyzer");
                    }
                }

            }
        }

        nodeAnalyzers.stream().forEach(
                n -> {
                    n.getAllPropertiesWithType(schema, CypherType.NUMBER).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(n),
                                        p.getKey()));
                            }
                    );
                }
        );
        nodeAnalyzers.stream().forEach(
                n -> {
                    n.getAllPropertiesWithType(schema, CypherType.STRING).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(n),
                                        p.getKey()));
                            }
                    );
                }
        );
        nodeAnalyzers.stream().forEach(
                n -> {
                    availableExpressions.add(new IdentifierExpression(n));
                }
        );

        relationAnalyzers.stream().forEach(
                r -> {
                    r.getAllPropertiesWithType(schema, CypherType.NUMBER).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(r),
                                        p.getKey()));
                            }
                    );
                }
        );
        relationAnalyzers.stream().forEach(
                r -> {
                    r.getAllPropertiesWithType(schema, CypherType.STRING).forEach(
                            p -> {
                                availableExpressions.add(new GetPropertyExpression(new IdentifierExpression(r),
                                        p.getKey()));
                            }
                    );
                }
        );
        relationAnalyzers.stream().forEach(
                r -> {
                    availableExpressions.add(new IdentifierExpression(r));
                }
        );

        List<IExpression> checkedAvailableExpressions = availableExpressions.stream().filter(e -> {
            if (e.getValue(varToProperties) == null) {
                return false;
            }
            if (assertion == null) {
                return true;
            }
            return assertion.check(e.getValue(varToProperties));
        }).collect(Collectors.toList());

        if (checkedAvailableExpressions.size() == 0) {
            return null;
        }

        return checkedAvailableExpressions;
    }

    private IExpression booleanExpression(long depth, BooleanAssertion booleanAssertion) {
        Randomly randomly = new Randomly();
        long expressionChoice = randomly.getInteger(0, 100);
        if (depth == 0 || expressionChoice < 10) {

            long randomNum = randomly.getInteger(0, 100);
            if (randomNum < 20) {
                return generateBooleanConst(booleanAssertion);
            }
            return new BinaryComparisonExpression(generateUseVar(CypherType.BOOLEAN, booleanAssertion), new ConstExpression(true), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);

        }



        boolean target = booleanAssertion == null ? randomly.getInteger(0, 100) < 50 : booleanAssertion.getValue();

        if (expressionChoice < 20 || (expressionChoice < 30 && MainOptions.mode == "falkordb")) {
            IExpression numberExpr = numberExpression(depth - 1, null);
            if (numberExpr.getValue(varToProperties) == ExprVal.UNKNOWN) {
                return BinaryComparisonExpression.randomComparison(numberExpr, numberExpression(depth - 1, null));
            }

            BinaryComparisonExpression.BinaryComparisonOperation op = BinaryComparisonExpression.randomOperation();
            return new BinaryComparisonExpression(numberExpr, numberExpression(depth - 1,
                    new ComparisonAssertion(op, numberExpr.getValue(varToProperties), target)), op);
        }
        if (expressionChoice < 30 && MainOptions.mode != "falkordb") {
            IExpression stringExpr = stringExpression(depth - 1, null);
            Object value = stringExpr.getValue(varToProperties).toString();
            if (value == ExprVal.UNKNOWN) {

                return generateUseVar(CypherType.STRING, null);
            }
            String strValue = (String) value;

            BinaryComparisonExpression.BinaryComparisonOperation op = BinaryComparisonExpression.randomOperation();
            ComparisonAssertion opChecker = new ComparisonAssertion(op, strValue, target);
            IExpression right = stringExpression(depth - 1, opChecker);
            Object rightValue = right.getValue(varToProperties);
            if (rightValue == ExprVal.UNKNOWN) {
                return generateUseVar(CypherType.STRING, null);
            }
            String rightStrValue = (String) rightValue;

            boolean currentRes = opChecker.check(rightStrValue);
            if (currentRes) {
                return new BinaryComparisonExpression(stringExpr, right, op);
            } else {
                return new BinaryComparisonExpression(stringExpr, right, op.reverse());
            }
        }
        if (expressionChoice < 40 && MainOptions.mode != "thinker") {
            IExpression stringExpr = stringExpression(depth - 1, null);
            if (stringExpr.getValue(varToProperties) == ExprVal.UNKNOWN) {
                return StringMatchingExpression.randomMatching(stringExpr, stringExpression(depth - 1, null));
            }

            StringMatchingExpression.StringMatchingOperation op = StringMatchingExpression.randomOperation();
            IExpression anotherString = stringExpression(depth - 1, new StringMatchingAssertion(op, stringExpr.getValue(varToProperties), target));
            String currentString = (String) stringExpr.getValue(varToProperties);
            String newString = (String) anotherString.getValue(varToProperties);
            IExpression returnValue = null;
            if (op.getTextRepresentation() == "STARTS WITH") {
                if (currentString.startsWith(newString))
                    returnValue = new StringMatchingExpression(stringExpr, anotherString, op);
                else if (newString.startsWith(currentString))
                    returnValue = new StringMatchingExpression(anotherString, stringExpr, op);
                else
                    returnValue = booleanExpression(depth - 1, new BooleanAssertion(true));
            } else if (op.getTextRepresentation() == "ENDS WITH") {
                if (currentString.endsWith(newString))
                    returnValue = new StringMatchingExpression(stringExpr, anotherString, op);
                else if (newString.endsWith(currentString))
                    returnValue = new StringMatchingExpression(anotherString, stringExpr, op);
                else
                    returnValue = booleanExpression(depth - 1, new BooleanAssertion(true));
            } else if (op.getTextRepresentation() == "CONTAINS") {
                if (currentString.contains(newString))
                    returnValue = new StringMatchingExpression(stringExpr, anotherString, op);
                else if (newString.contains(currentString))
                    returnValue = new StringMatchingExpression(anotherString, stringExpr, op);
                else
                    returnValue = booleanExpression(depth - 1, new BooleanAssertion(true));
            } else {
                returnValue = booleanExpression(depth - 1, new BooleanAssertion(true));
            }
            if (target == false) {
                if (MainOptions.mode == "thinker")
                    returnValue = new BinaryComparisonExpression(returnValue, new ConstExpression(false), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);
                else
                    returnValue = new SingleLogicalExpression(returnValue, SingleLogicalExpression.SingleLogicalOperation.NOT);
            }
            return returnValue;
        }
        if (expressionChoice < 50 && MainOptions.mode != "thinker") {
            target = !target;
            return new SingleLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(target)), SingleLogicalExpression.SingleLogicalOperation.NOT);
        }

        BinaryLogicalExpression.BinaryLogicalOperation op = BinaryLogicalExpression.randomOp();
        switch (op) {
            case AND:
                if (target) {
                    return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(true)),
                            booleanExpression(depth - 1, new BooleanAssertion(true)),
                            op);
                } else {
                    if (randomly.getInteger(0, 100) < 50) {
                        return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(false)),
                                booleanExpression(depth - 1, null),
                                op);
                    } else {
                        return new BinaryLogicalExpression(booleanExpression(depth - 1, null),
                                booleanExpression(depth - 1, new BooleanAssertion(false)),
                                op);
                    }
                }
            case OR:
                if (target) {
                    if (randomly.getInteger(0, 100) < 50) {
                        if (MainOptions.mode == "thinker")
                            return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(true)),
                                    booleanExpression(depth - 1, new BooleanAssertion(true)),
                                    op);
                        else
                            return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(true)),
                                    booleanExpression(depth - 1, null),
                                    op);
                    } else {
                        if (MainOptions.mode == "thinker")
                            return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(true)),
                                    booleanExpression(depth - 1, new BooleanAssertion(true)),
                                    op);
                        else
                            return new BinaryLogicalExpression(booleanExpression(depth - 1, null),
                                    booleanExpression(depth - 1, new BooleanAssertion(true)),
                                    op);
                    }
                } else {
                    return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(false)),
                            booleanExpression(depth - 1, new BooleanAssertion(false)),
                            op);
                }
            case XOR:
                if (target) {
                    if (randomly.getInteger(0, 100) < 50) {
                        return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(true)),
                                booleanExpression(depth - 1, new BooleanAssertion(false)),
                                op);
                    } else {
                        return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(false)),
                                booleanExpression(depth - 1, new BooleanAssertion(true)),
                                op);
                    }
                } else {
                    if (randomly.getInteger(0, 100) < 50) {
                        return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(false)),
                                booleanExpression(depth - 1, new BooleanAssertion(false)),
                                op);
                    } else {
                        return new BinaryLogicalExpression(booleanExpression(depth - 1, new BooleanAssertion(true)),
                                booleanExpression(depth - 1, new BooleanAssertion(true)),
                                op);
                    }
                }
            default:
                throw new RuntimeException();
        }
    }

    private IExpression stringExpression(long depth, ExpressionAssertion expressionAssertion) {
        Randomly randomly = new Randomly();
        long expressionChoice = randomly.getInteger(0, 100);
        if (depth == 0 || expressionChoice < 30) {

            long randomNum = randomly.getInteger(0, 100);
            if (randomNum < 20) {
                return generateStringConst(expressionAssertion);
            }
            return generateUseVar(CypherType.STRING, expressionAssertion);
        }






        else {
            StringCatExpression temp;
            if (expressionAssertion instanceof StringMatchingAssertion) {
                if (((StringMatchingAssertion) expressionAssertion).isTarget()) {
                    switch (((StringMatchingAssertion) expressionAssertion).getOperation()) {
                        case STARTS_WITH:


                            return stringExpression(depth - 1, expressionAssertion);
                        case ENDS_WITH:


                            return stringExpression(depth - 1, expressionAssertion);
                        case CONTAINS:
                            long randNum = randomly.getInteger(0, 100);
                            Object stringObj = ((StringMatchingAssertion) expressionAssertion).getString();
                            if (stringObj == ExprVal.UNKNOWN) {
                                return new StringCatExpression(stringExpression(depth - 1, null),
                                        stringExpression(depth - 1, null));
                            }
                            String string = (String) stringObj;
                            if (randNum < 30) {
                                return new StringCatExpression(stringExpression(depth - 1, null),
                                        stringExpression(depth - 1, expressionAssertion));
                            } else if (randNum < 60 || string.length() == 0) {
                                return new StringCatExpression(stringExpression(depth - 1, expressionAssertion),
                                        stringExpression(depth - 1, null));
                            } else {
                                long index = randomly.getInteger(0, string.length());
                                String first = string.substring(0, (int) index);
                                String second = string.substring((int) index, string.length());
                                return new StringCatExpression(stringExpression(depth - 1, new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.ENDS_WITH, first, true)),
                                        stringExpression(depth - 1, new StringMatchingAssertion(StringMatchingExpression.StringMatchingOperation.STARTS_WITH, second, true)));
                            }
                    }
                } else {

                    return new StringCatExpression(stringExpression(depth - 1, null), stringExpression(depth - 1, null));
                }
            }

            if (MainOptions.mode != "thinker")
                return new StringCatExpression(stringExpression(depth - 1, null), stringExpression(depth - 1, null));
            else
                return generateUseVar(CypherType.STRING, expressionAssertion);
        }

    }

    private IExpression numberExpression(long depth, ComparisonAssertion comparisonAssertion) {
        Randomly randomly = new Randomly();
        long expressionChoice = randomly.getInteger(0, 100);
        if (depth == 0 || expressionChoice < 70) {

            long randomNum = randomly.getInteger(0, 100);
            if (randomNum < 5) {
                return generateNumberConst(comparisonAssertion);
            }
            return generateUseVar(CypherType.NUMBER, comparisonAssertion);
        }
        return generateNumberConst(comparisonAssertion);

    }

}
