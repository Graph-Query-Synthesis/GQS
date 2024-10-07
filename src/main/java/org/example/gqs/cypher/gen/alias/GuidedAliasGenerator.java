package org.example.gqs.cypher.gen.alias;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.*;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.gen.expr.NonEmptyExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.Ret;
import org.example.gqs.cypher.standard_ast.expr.*;
import org.example.gqs.cypher.dsl.BasicAliasGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.schema.IPropertyInfo;

import java.util.*;

import static java.lang.Math.max;
import static org.example.gqs.cypher.gen.condition.GuidedConditionGenerator.MAX_DEPTH;

public class GuidedAliasGenerator<S extends CypherSchema<?,?>> extends BasicAliasGenerator<S> {
    private boolean overrideOld;
    private Map<String, Object> varToVal;
    public GuidedAliasGenerator(S schema, IIdentifierBuilder identifierBuilder, boolean overrideOld, Map<String, Object> varToVal) {
        super(schema, identifierBuilder);
        this.overrideOld = overrideOld;
        this.varToVal = varToVal;
    }

    @Override
    public List<IRet> generateReturnAlias(IReturnAnalyzer returnClause, IIdentifierBuilder identifierBuilder, S schema) {
        if (returnClause.getReturnList().size() > 0 && !overrideOld) {
            return returnClause.getReturnList();
        }

        List<IRet> results = new ArrayList<>();
        List<INodeAnalyzer> idNode = returnClause.getExtendableNodeIdentifiers();
        List<IRelationAnalyzer> idRelation = returnClause.getExtendableRelationIdentifiers();
        List<IAliasAnalyzer> idAlias = returnClause.getExtendableAliases();
        Randomly r = new Randomly();
        long sizeOfAlias = idAlias.size();
        long sizeOfNode = idNode.size();
        long sizeOfRelation = idRelation.size();

        long numOfExpressions = r.getInteger(1, 6);
        ArrayList<IExpression> orderByExpression = new ArrayList<>();

        for (int i = 0; i < numOfExpressions; i++) {
            Ret result = null;
            if (i == 0) {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        if (alias.getExpression() instanceof CallExpression && (((CallExpression) alias.getExpression()).getValue() instanceof AbstractNode || ((CallExpression) alias.getExpression()).getValue() instanceof CreateListExpression || ((CallExpression) alias.getExpression()).getValue() instanceof AbstractRelationship)) {
                            results.add(Ret.createNewExpressionAlias(identifierBuilder, new ConstExpression(Randomly.smallNumber())));
                        } else {
                            result = Ret.createAliasRef(alias);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 1 || kind == 2) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            if (!(((NodeIdentifier) (((NodeAnalyzer) ie.getIdentifier()).getSource())).actualNode.getProperties().containsKey(prop.getKey()))) {
                                continue;
                            }
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 3 || kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                if (!(((RelationIdentifier) (((RelationAnalyzer) ie.getIdentifier()).getSource())).actualRelationship.getProperties().containsKey(prop.getKey()))) {
                                    continue;
                                }
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(returnClause, schema, varToVal).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                }
            } else {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        if (alias.getExpression() instanceof CallExpression && (((CallExpression) alias.getExpression()).getValue() instanceof AbstractNode || ((CallExpression) alias.getExpression()).getValue() instanceof CreateListExpression || ((CallExpression) alias.getExpression()).getValue() instanceof AbstractRelationship)) {
                            results.add(Ret.createNewExpressionAlias(identifierBuilder, new ConstExpression(Randomly.smallNumber())));
                        } else {
                            result = Ret.createAliasRef(alias);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 1 || kind == 2) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 3 || kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(returnClause, schema, varToVal).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                }
            }
            if (result != null) {
                boolean flag = true;
                for (IRet res : results) {
                    if (res.equals(result)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    results.add(result);
                }
            }
        }
        if (results.isEmpty()) {
            results.add(Ret.createNewExpressionAlias(identifierBuilder, new ConstExpression(Randomly.smallNumber())));
        }
        returnClause.setDistinct(Randomly.getBooleanWithRatherLowProbability());
        if (Randomly.getBooleanWithRatherLowProbability()) {
            long numOfOrderBy = r.getInteger(1, results.size() + 1);
            while (orderByExpression.size() > numOfOrderBy) {
                orderByExpression.remove(r.getInteger(0, orderByExpression.size()));
            }
            if (orderByExpression.size() > 0) {
                returnClause.setOrderBy(orderByExpression, Randomly.getBoolean());
            }
        }
        return results;
    }

    public List<IRet> generateReturnAlias(IReturnAnalyzer returnClause, IIdentifierBuilder identifierBuilder, S schema, List<Map<String, Object>> namespace) {
        if (returnClause.getReturnList().size() > 0 && !overrideOld) {
            return returnClause.getReturnList();
        }

        List<IRet> results = new ArrayList<>();
        List<INodeAnalyzer> idNode = returnClause.getExtendableNodeIdentifiers();
        List<IRelationAnalyzer> idRelation = returnClause.getExtendableRelationIdentifiers();
        List<IAliasAnalyzer> idAlias = returnClause.getExtendableAliases();
        Randomly r = new Randomly();
        long sizeOfAlias = idAlias.size();
        long sizeOfNode = idNode.size();
        long sizeOfRelation = idRelation.size();

        long numOfExpressions = r.getInteger(1, 6);
        ArrayList<IExpression> orderByExpression = new ArrayList<>();

        for (int i = 0; i < numOfExpressions; i++) {
            Ret result = null;
            if (i == 0) {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        if (alias.getExpression() instanceof CallExpression && !MainOptions.isScalarFunction(((CallExpression) alias.getExpression()).functionName)) {
                            results.add(Ret.createNewExpressionAlias(identifierBuilder, new ConstExpression(Randomly.smallNumber())));
                        } else {
                            result = Ret.createAliasRef(alias);
                            boolean orderbyFlag = true;
                            if (alias.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) alias.getExpression()).functionName)) {
                                if (((CallExpression) alias.getExpression()).functionName == "collect") {
                                    orderbyFlag = false;
                                } else
                                    orderbyFlag = true;
                            } else if (alias.getExpression() instanceof CallExpression) {
                                if (!MainOptions.isScalarFunction(((CallExpression) alias.getExpression()).functionName)) {
                                    orderbyFlag = false;
                                } else {
                                    orderbyFlag = true;
                                }
                            } else if (alias.getExpression() instanceof CreateListExpression)
                                orderbyFlag = true;
                            else if (((Alias) alias).getValue() instanceof AbstractNode || ((Alias) alias).getValue() instanceof AbstractRelationship) {
                                orderbyFlag = false;
                            } else {
                                orderbyFlag = true;
                            }
                            if (orderbyFlag)
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 1 || kind == 2) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            if (!(((NodeIdentifier) (((NodeAnalyzer) ie.getIdentifier()).getSource())).actualNode.getProperties().containsKey(prop.getKey()))) {
                                continue;
                            }
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 3 || kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                if (!(((RelationIdentifier) (((RelationAnalyzer) ie.getIdentifier()).getSource())).actualRelationship.getProperties().containsKey(prop.getKey()))) {
                                    continue;
                                }
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(returnClause, schema, varToVal).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                }
            } else {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        if ((alias.getExpression() instanceof CallExpression && !MainOptions.isScalarFunction(((CallExpression) alias.getExpression()).functionName)) || ((Alias) alias).getValue() instanceof AbstractNode || ((Alias) alias).getValue() instanceof AbstractRelationship) {
                            results.add(Ret.createNewExpressionAlias(identifierBuilder, new ConstExpression(Randomly.smallNumber())));
                        } else {
                            result = Ret.createAliasRef(alias);
                            boolean orderbyFlag = true;
                            if (alias.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) alias.getExpression()).functionName)) {
                                if (((CallExpression) alias.getExpression()).functionName == "collect") {
                                    orderbyFlag = false;
                                } else
                                    orderbyFlag = true;
                            } else if (alias.getExpression() instanceof CallExpression) {
                                if (!MainOptions.isScalarFunction(((CallExpression) alias.getExpression()).functionName)) {
                                    orderbyFlag = false;
                                } else {
                                    orderbyFlag = true;
                                }
                            } else if (alias.getExpression() instanceof CreateListExpression)
                                orderbyFlag = true;
                            else if (((Alias) alias).getValue() instanceof AbstractNode || ((Alias) alias).getValue() instanceof AbstractRelationship) {
                                orderbyFlag = false;
                            } else {
                                orderbyFlag = true;
                            }
                            if (orderbyFlag)
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 1 || kind == 2) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 3 || kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(returnClause, schema, varToVal).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                }
            }
            if (result != null) {
                boolean flag = true;
                for (IRet res : results) {
                    if (res.equals(result)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    results.add(result);
                }
            }
        }
        if (results.isEmpty()) {
            results.add(Ret.createNewExpressionAlias(identifierBuilder, new ConstExpression(Randomly.smallNumber())));
        }
        returnClause.setDistinct(Randomly.getBooleanWithRatherLowProbability());
        boolean orderByFlag = Randomly.getBooleanWithRatherLowProbability();
        Set<IExpression> mustOrderBy = new HashSet<>();
        for (IRet ret : results) {
            if (ret.getIdentifier() instanceof Alias) {
                if (((Alias) ret.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) ((Alias) ret.getIdentifier()).getExpression()).functionName)) {
                    orderByFlag = true;
                    mustOrderBy.add(new IdentifierExpression(ret.getIdentifier()));

                } else if (((Alias) ret.getIdentifier()).getValue() instanceof List) {
                    orderByFlag = true;
                    mustOrderBy.add(new IdentifierExpression(ret.getIdentifier()));
                }
            }
        }
        if (orderByFlag) {
            long numOfOrderBy = r.getInteger(1, results.size() + 1);
            while (orderByExpression.size() > numOfOrderBy) {
                orderByExpression.remove(r.getInteger(0, orderByExpression.size()));
            }
            for (IExpression exp : mustOrderBy) {
                if (!orderByExpression.contains(exp))
                    orderByExpression.add(exp);
            }
            if (orderByExpression.size() > 0) {
                List<Boolean> isDesc = new ArrayList<>();
                for (int i = 0; i < orderByExpression.size(); i++) {
                    isDesc.add(Randomly.getBoolean());
                }
                returnClause.setOrderBy(orderByExpression, isDesc);
            }
        }
        if (((Return) returnClause).orderBy != null && ((Return) returnClause).orderBy.size() > 0) {
            if (Randomly.getBooleanWithRatherLowProbability() && MainOptions.mode != "thinker") {
                returnClause.setLimit(new ConstExpression(Randomly.getNotCachedInteger(1, 100)));
            }
            if (Randomly.getBooleanWithRatherLowProbability() && MainOptions.mode != "thinker") {
                returnClause.setSkip(new ConstExpression(Randomly.getNotCachedInteger(0, 100)));
            }
        }
        if (MainOptions.mode == "thinker" && ((Return) returnClause).isDistinct() == true && orderByFlag) {
            ((Return) returnClause).setDistinct(false);
        }
        if (returnClause.isDistinct()) {
            for (IRet ret : results) {
                if (ret.getIdentifier() instanceof Alias) {
                    ((Alias) ret.getIdentifier()).isDistinct = true;
                }
            }
        }
        ((Return) (returnClause)).setReturnList(results);
        ((Return) returnClause).getNamespace(namespace);


        return results;
    }

    @Override
    public List<IRet> generateWithAlias(IWithAnalyzer withClause, IIdentifierBuilder identifierBuilder, S schema) {
        List<IRet> withAlias = withClause.getReturnList();
        if (withAlias.size() > 0 && !overrideOld) {
            return withAlias;
        }

        List<IRet> results = new ArrayList<>();
        List<INodeAnalyzer> idNode = withClause.getExtendableNodeIdentifiers();
        List<IRelationAnalyzer> idRelation = withClause.getExtendableRelationIdentifiers();
        List<IAliasAnalyzer> idAlias = withClause.getExtendableAliases();
        Randomly r = new Randomly();
        long sizeOfAlias = idAlias.size();
        long sizeOfNode = idNode.size();
        long sizeOfRelation = idRelation.size();
        long propOfNode = 0;
        long propOfRelation = 0;
        long numOfExpressions = r.getInteger(1, MainOptions.withClauseSize);
        ArrayList<IExpression> orderByExpression = new ArrayList<>();
        boolean markAggregation = false;
        for (int i = 0; i < numOfExpressions; i++) {
            Ret result = null;
            if (i == 0) {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        result = Ret.createNodeRef(node);
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        for (int j = 0; j < props.size(); j++) {
                            IPropertyInfo prop = props.get(j);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            orderByExpression.add(exp);
                        }
                    }
                } else if (kind == 2) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        result = Ret.createRelationRef(relation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            for (int j = 0; j < props.size(); j++) {
                                IPropertyInfo prop = props.get(j);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                orderByExpression.add(exp);
                            }
                        }
                    }
                } else if (kind == 3) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                        }
                    }
                } else if (kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            }
                        }
                    }
                } else {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN, CypherType.NODE, CypherType.RELATION, CypherType.LIST);

                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(withClause, schema, varToVal).generateFunction(type));
                    if (result.getIdentifier() instanceof Alias && ((Alias) result.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) ((Alias) result.getIdentifier()).getExpression()).functionName))
                        markAggregation = true;
                }
            } else {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        result = Ret.createNodeRef(node);
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        for (int j = 0; j < props.size(); j++) {
                            IPropertyInfo prop = props.get(j);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            orderByExpression.add(exp);
                        }
                    }
                } else if (kind == 2) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        result = Ret.createRelationRef(relation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            for (int j = 0; j < props.size(); j++) {
                                IPropertyInfo prop = props.get(j);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                orderByExpression.add(exp);
                            }
                        }
                    }
                } else if (kind == 3) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN, CypherType.NODE, CypherType.RELATION);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(withClause, schema, varToVal).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    if (result.getIdentifier() instanceof Alias && ((Alias) result.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) ((Alias) result.getIdentifier()).getExpression()).functionName))
                        markAggregation = true;
                }
            }

            if (result != null) {
                boolean flag = true;
                for (IRet res : results) {
                    if (res.equals(result)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    results.add(result);
                }
            }
        }
        if (markAggregation)
            ((With) withClause).usedAggregation = true;
        if (results.isEmpty()) {
            ConstExpression exp = new ConstExpression(Randomly.smallNumber());
            results.add(Ret.createNewExpressionAlias(identifierBuilder, exp));
        }
        if (numOfExpressions != 1)
            withClause.setDistinct(Randomly.getBooleanWithRatherLowProbability());
        if (Randomly.getBooleanWithRatherLowProbability()) {
            long numOfOrderBy = r.getInteger(max(1, results.size() - 2), results.size() + 1);
            while (orderByExpression.size() > numOfOrderBy) {
                orderByExpression.remove(r.getInteger(0, orderByExpression.size()));
            }
            if (orderByExpression.size() > 0) {
                withClause.setOrderBy(orderByExpression, Randomly.getBoolean());
            }
        }
        if (((With) withClause).orderBy != null && ((With) withClause).orderBy.size() > 0) {

            if (Randomly.getBooleanWithRatherLowProbability() && MainOptions.mode != "thinker") {
                withClause.setLimit(new ConstExpression(Randomly.smallNumber()));
            }
            if (Randomly.getBooleanWithRatherLowProbability() && MainOptions.mode != "thinker") {
                withClause.setSkip(new ConstExpression(Randomly.smallNumber()));
            }
        }
        if (withClause.isDistinct()) {
            for (IRet ret : results) {
                if (ret.getIdentifier() instanceof Alias) {
                    ((Alias) ret.getIdentifier()).isDistinct = true;
                }
            }
        }
        results.forEach(
                ret -> {
                    if (ret.isAlias() && ret.getExpression() != null) {
                        if (withClause.isDistinct() && ret.getExpression() instanceof CreateListExpression) {
                            List<Object> listSet = new ArrayList<>(new HashSet<>(((CreateListExpression) ret.getExpression()).getValue()));
                            varToVal.put(ret.getIdentifier().getName(), listSet);
                        } else
                            varToVal.put(ret.getIdentifier().getName(), ret.getExpression().getValue(varToVal));
                    }
                }
        );
        return results;
    }
    public List<IRet> generateWithAlias(IWithAnalyzer withClause, IIdentifierBuilder identifierBuilder, S schema, List<Map<String, Object>> namespace) {
        List<IRet> withAlias = withClause.getReturnList();
        if (withAlias.size() > 0 && !overrideOld) {
            return withAlias;
        }

        List<IRet> results = new ArrayList<>();
        List<INodeAnalyzer> idNode = withClause.getExtendableNodeIdentifiers();
        List<IRelationAnalyzer> idRelation = withClause.getExtendableRelationIdentifiers();
        List<IAliasAnalyzer> idAlias = withClause.getExtendableAliases();
        Randomly r = new Randomly();
        long sizeOfAlias = idAlias.size();
        long sizeOfNode = idNode.size();
        long sizeOfRelation = idRelation.size();
        long propOfNode = 0;
        long propOfRelation = 0;
        long numOfExpressions = r.getInteger(1, MainOptions.withClauseSize);
        ArrayList<IExpression> orderByExpression = new ArrayList<>();
        ArrayList<String> disableOrderBy = new ArrayList<>();
        boolean markAggregation = false;
        for (int i = 0; i < numOfExpressions; i++) {
            Ret result = null;
            if (i == 0) {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        result = Ret.createAliasRef(alias);
                        if (MainOptions.mode == "kuzu" && alias.getExpression() instanceof GetPropertyExpression) {
                            disableOrderBy.add(alias.getName());
                        }
                        boolean orderbyFlag = true;
                        if (alias.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) alias.getExpression()).functionName)) {
                            if (((CallExpression) alias.getExpression()).functionName == "collect") {
                                orderbyFlag = false;
                            } else
                                orderbyFlag = true;
                        } else if (alias.getExpression() instanceof CallExpression) {
                            if (!MainOptions.isScalarFunction(((CallExpression) alias.getExpression()).functionName)) {
                                orderbyFlag = false;
                            } else {
                                orderbyFlag = true;
                            }
                        } else if (alias.getExpression() instanceof CreateListExpression)
                            orderbyFlag = true;
                        else if (((Alias) alias).getValue() instanceof AbstractNode || ((Alias) alias).getValue() instanceof AbstractRelationship || ((Alias) alias).getValue() instanceof List) {
                            orderbyFlag = false;
                        } else {
                            orderbyFlag = true;
                        }
                        if (orderbyFlag)
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1) {
                    if (sizeOfNode > 0) {
                        INodeIdentifier node = idNode.get(r.getInteger(0, sizeOfNode));
                        result = Ret.createNodeRef(node);
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        for (int j = 0; j < props.size(); j++) {
                            IPropertyInfo prop = props.get(j);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            orderByExpression.add(exp);
                        }
                    }
                } else if (kind == 2) {
                    if (sizeOfRelation > 0) {
                        IRelationIdentifier relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        result = Ret.createRelationRef(relation);
                        if (relation.getLengthLowerBound() == 1 && relation.getLengthUpperBound() == 1) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            for (int j = 0; j < props.size(); j++) {
                                IPropertyInfo prop = props.get(j);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                orderByExpression.add(exp);
                            }
                        }
                    }
                } else if (kind == 3) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = ((NodeAnalyzer) node).existedProperties();
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            if (MainOptions.mode == "kuzu")
                                orderByExpression.add(exp);
                            else
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = ((RelationAnalyzer) relation).existedProperties();
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                if (MainOptions.mode == "kuzu")
                                    orderByExpression.add(exp);
                                else
                                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else if (kind >= 5 && kind <= 9) {
                    CypherType type;
                    if (MainOptions.mode != "thinker")
                        type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN, CypherType.NODE, CypherType.RELATION, CypherType.LIST);
                    else
                        type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING);

                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(withClause, schema, varToVal).generateFunction(type));
                    if (type != CypherType.NODE && type != CypherType.RELATION && type != CypherType.LIST && type != CypherType.BOOLEAN) {
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                    markAggregation = true;
                } else {
                    result = Ret.createStar();
                    numOfExpressions = 1;
                }
            } else {
                long kind = r.getInteger(0, 10);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        result = Ret.createAliasRef(alias);
                        if (MainOptions.mode == "kuzu" && alias.getExpression() instanceof GetPropertyExpression) {
                            disableOrderBy.add(alias.getName());
                        }
                        boolean orderbyFlag = true;
                        if (alias.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) alias.getExpression()).functionName)) {
                            if (((CallExpression) alias.getExpression()).functionName == "collect") {
                                orderbyFlag = false;
                            } else
                                orderbyFlag = true;
                        } else if (alias.getExpression() instanceof CallExpression) {
                            if (!MainOptions.isScalarFunction(((CallExpression) alias.getExpression()).functionName)) {
                                orderbyFlag = false;
                            } else {
                                orderbyFlag = true;
                            }
                        } else if (alias.getExpression() instanceof CreateListExpression)
                            orderbyFlag = true;
                        else if (((Alias) alias).getValue() instanceof AbstractNode || ((Alias) alias).getValue() instanceof AbstractRelationship) {
                            orderbyFlag = false;
                        } else {
                            orderbyFlag = true;
                        }
                        if (orderbyFlag)
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        result = Ret.createNodeRef(node);
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        for (int j = 0; j < props.size(); j++) {
                            IPropertyInfo prop = props.get(j);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            orderByExpression.add(exp);
                        }
                    }
                } else if (kind == 2) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        result = Ret.createRelationRef(relation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            for (int j = 0; j < props.size(); j++) {
                                IPropertyInfo prop = props.get(j);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                orderByExpression.add(exp);
                            }
                        }
                    }
                } else if (kind == 3) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = idNode.get(r.getInteger(0, sizeOfNode));
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        if (props.size() > 0) {
                            IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            if (MainOptions.mode == "kuzu")
                                orderByExpression.add(exp);
                            else
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = idRelation.get(r.getInteger(0, sizeOfRelation));
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            if (props.size() > 0) {
                                IPropertyInfo prop = props.get(r.getInteger(0, props.size()));
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                if (MainOptions.mode == "kuzu")
                                    orderByExpression.add(exp);
                                else
                                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type;
                    if (MainOptions.mode != "thinker")
                        type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN, CypherType.NODE, CypherType.RELATION, CypherType.LIST);
                    else
                        type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new NonEmptyExpressionGenerator<>(withClause, schema, varToVal).generateFunction(type));
                    if (type != CypherType.NODE && type != CypherType.RELATION && type != CypherType.LIST && type != CypherType.BOOLEAN) {
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                    markAggregation = true;
                }
            }

            if (result != null) {
                boolean flag = true;
                for (IRet res : results) {
                    if (res.equals(result) || res.getIdentifier().getName().equals(result.getIdentifier().getName())) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    results.add(result);
                }
            }


        }

        if (markAggregation)
            ((With) withClause).usedAggregation = true;
        if (results.isEmpty()) {
            ConstExpression exp = new ConstExpression(Randomly.smallNumber());
            results.add(Ret.createNewExpressionAlias(identifierBuilder, exp));
        }

        if (MainOptions.mode == "memgraph") {
            for (IExpression e : orderByExpression) {
                if (e instanceof IdentifierExpression)
                    continue;
                results.add(Ret.createNewExpressionAlias(identifierBuilder, e));
            }
        }
        if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu") {
            IExpression returnExp = new NonEmptyExpressionGenerator<>(withClause, schema, varToVal).generateCondition(MAX_DEPTH);
            withClause.setCondition(returnExp);
            Set<IIdentifier> expIdentifier = ((CypherExpression) returnExp).reliedContent();
            for (IExpression e : orderByExpression) {
                if (e instanceof IdentifierExpression)
                    continue;
                results.add(Ret.createNewExpressionAlias(identifierBuilder, e));
            }

            Set<String> retIdentifier = new HashSet<>();
            for (IRet ret : results) {
                retIdentifier.add(ret.getIdentifier().getName());
            }
            for (IIdentifier id : expIdentifier) {
                if (!retIdentifier.contains(id.getName())) {
                    if (id instanceof NodeIdentifier)
                        results.add(Ret.createNodeRef((NodeIdentifier) id));
                    else if (id instanceof RelationIdentifier)
                        results.add(Ret.createRelationRef((RelationIdentifier) id));
                    else if (id instanceof Alias)
                        results.add(Ret.createAliasRef((Alias) id));
                    retIdentifier.add(id.getName());
                }
            }
        }


        if (numOfExpressions != 1)
            withClause.setDistinct(Randomly.getBooleanWithRatherLowProbability());
        boolean orderByFlag = Randomly.getBooleanWithRatherLowProbability();
        Set<IExpression> mustOrderBy = new HashSet<>();
        for (IRet ret : results) {
            if (ret.getIdentifier() instanceof Alias) {
                if (((Alias) ret.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) ((Alias) ret.getIdentifier()).getExpression()).functionName) && MainOptions.isScalarFunction(((CallExpression) ((Alias) ret.getIdentifier()).getExpression()).functionName)) {
                    orderByFlag = true;
                    if (MainOptions.mode == "kuzu" && ((Alias) ret.getIdentifier()).getExpression() instanceof GetPropertyExpression && !disableOrderBy.contains(((Alias) ret.getIdentifier()).getName()))
                        mustOrderBy.add(((Alias) ret.getIdentifier()).getExpression());
                    else
                        mustOrderBy.add(new IdentifierExpression(ret.getIdentifier()));
                } else if (!(((Alias) ret.getIdentifier()).getExpression() instanceof CallExpression && !MainOptions.isScalarFunction(((CallExpression) ((Alias) ret.getIdentifier()).getExpression()).functionName))) {
                    orderByFlag = true;
                    if (MainOptions.mode == "kuzu" && ((Alias) ret.getIdentifier()).getExpression() instanceof GetPropertyExpression && !disableOrderBy.contains(((Alias) ret.getIdentifier()).getName()))
                        mustOrderBy.add(((Alias) ret.getIdentifier()).getExpression());
                    else
                        mustOrderBy.add(new IdentifierExpression(ret.getIdentifier()));
                }

            }
            if (ret.isAll() == true) {
                Map<String, Object> deduplicated = new HashMap<>();
                Set<String> added = new HashSet<>();
                for (int i = 0; i < namespace.size(); i++) {
                    if (i == 0) {
                        for (String key : namespace.get(i).keySet()) {
                            deduplicated.put(key, namespace.get(i).get(key));
                        }
                    } else {
                        for (String key : namespace.get(i).keySet()) {
                            if (deduplicated.containsKey(key)) {
                                if (!deduplicated.get(key).equals(namespace.get(i).get(key)) && !added.contains(key)) {
                                    added.add(key);
                                    mustOrderBy.add(new IdentifierExpression(new Alias(key, new ConstExpression(namespace.get(i).get(key)))));
                                }
                            } else {
                                throw new RuntimeException("Find inconsistent between different lines of namespace");
                            }
                        }
                    }

                }
            }
        }
        if (orderByFlag) {
            long numOfOrderBy = r.getInteger(1, results.size() + 1);
            while (orderByExpression.size() > numOfOrderBy) {
                orderByExpression.remove(r.getInteger(0, orderByExpression.size()));
            }
            for (IExpression exp : mustOrderBy) {
                if (!orderByExpression.contains(exp))
                    orderByExpression.add(exp);
            }
            if (orderByExpression.size() > 0) {
                List<Boolean> isDesc = new ArrayList<>();
                for (int i = 0; i < orderByExpression.size(); i++) {
                    isDesc.add(Randomly.getBoolean());
                }
                withClause.setOrderBy(orderByExpression, isDesc);
            }
        }
        if (((With) withClause).orderBy != null && ((With) withClause).orderBy.size() > 0 && (MainOptions.mode != "thinker" || MainOptions.mode == "kuzu")) {

            if (Randomly.getBooleanWithRatherLowProbability()) {
                withClause.setLimit(new ConstExpression(Randomly.getNotCachedInteger(1, 100)));
            } else {
                withClause.setSkip(new ConstExpression(Randomly.getNotCachedInteger(0, 100)));
            }
        }
        if (MainOptions.mode == "thinker" && ((With) withClause).isDistinct() == true && orderByFlag) {
            ((With) withClause).setDistinct(false);
        }
        if (withClause.isDistinct()) {
            for (IRet ret : results) {
                if (ret.getIdentifier() instanceof Alias) {
                    ((Alias) ret.getIdentifier()).isDistinct = true;
                }
            }
        }

        ((With) (withClause)).setReturnList(results);
        ((With) withClause).getNamespace(namespace);
        results.forEach(
                ret -> {
                    if (ret.isAlias() && ret.getExpression() != null) {
                        if (withClause.isDistinct() && ret.getExpression() instanceof CreateListExpression) {
                            List<Object> listSet = new ArrayList<>(new HashSet<>(((CreateListExpression) ret.getExpression()).getValue()));
                            varToVal.put(ret.getIdentifier().getName(), listSet);
                        } else {
                            if (!(ret.getExpression() instanceof CallExpression))
                                varToVal.put(ret.getIdentifier().getName(), ret.getExpression().getValue(varToVal));
                            else if (!(ret.getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) ret.getExpression()).functionName) && ((List) (((CallExpression) ret.getExpression()).getElementValue())).size() > 1))
                                varToVal.put(ret.getIdentifier().getName(), ret.getExpression().getValue(varToVal));
                        }
                    }
                }
        );

        return results;
    }
}
