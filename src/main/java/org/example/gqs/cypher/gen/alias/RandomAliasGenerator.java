package org.example.gqs.cypher.gen.alias;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.analyzer.*;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.gen.expr.RandomExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.Ret;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;
import org.example.gqs.cypher.standard_ast.expr.GetPropertyExpression;
import org.example.gqs.cypher.standard_ast.expr.IdentifierExpression;
import org.example.gqs.cypher.dsl.BasicAliasGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.schema.IPropertyInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RandomAliasGenerator<S extends CypherSchema<?,?>> extends BasicAliasGenerator<S> {
    private boolean overrideOld;
    public RandomAliasGenerator(S schema, IIdentifierBuilder identifierBuilder, boolean overrideOld) {
        super(schema, identifierBuilder);
        this.overrideOld = overrideOld;
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
                long kind = r.getInteger(0, 5);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1 || kind == 2) {
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
                } else if (kind == 3 || kind == 4) {
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
                } else if (kind == 5) {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN, CypherType.NODE, CypherType.RELATION);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new RandomExpressionGenerator<>(returnClause, schema).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                } else {
                    result = Ret.createNewExpressionReturnVal(new ConstExpression(Randomly.smallNumber()));
                }
            } else {
                long kind = r.getInteger(0, 5);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = idAlias.get(r.getInteger(0, sizeOfAlias));
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1 || kind == 2) {
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
                } else if (kind == 3 || kind == 4) {
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
                            new RandomExpressionGenerator<>(returnClause, schema).generateFunction(type));
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

    @Override
    public List<IRet> generateReturnAlias(IReturnAnalyzer returnClause, IIdentifierBuilder identifierBuilder, S schema, List<Map<String, Object>> namespace) {
        throw new RuntimeException("Not implemented");
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

        long numOfExpressions = r.getInteger(1, 6);
        ArrayList<IExpression> orderByExpression = new ArrayList<>();

        for (int i = 0; i < numOfExpressions; i++) {
            Ret result = null;
            if (i == 0) {
                long kind = r.getInteger(0, 5);
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
                } else if (kind == 5) {
                    CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN, CypherType.NODE, CypherType.RELATION);
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new RandomExpressionGenerator<>(withClause, schema).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                } else {

                    ConstExpression exp = new ConstExpression(123123);
                    result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                }
            } else {
                long kind = r.getInteger(0, 6);
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
                            new RandomExpressionGenerator<>(withClause, schema).generateFunction(type));
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
            ConstExpression exp = new ConstExpression(123123);
            results.add(Ret.createNewExpressionAlias(identifierBuilder, exp));
        }
        withClause.setDistinct(Randomly.getBooleanWithRatherLowProbability());
        if (Randomly.getBooleanWithRatherLowProbability()) {
            long numOfOrderBy = r.getInteger(1, results.size() + 1);
            while (orderByExpression.size() > numOfOrderBy) {
                orderByExpression.remove(r.getInteger(0, orderByExpression.size()));
            }
            if (orderByExpression.size() > 0) {
                withClause.setOrderBy(orderByExpression, Randomly.getBoolean());
            }
        }
        return results;
    }

    @Override
    public List<IRet> generateWithAlias(IWithAnalyzer withClause, IIdentifierBuilder identifierBuilder, S schema, List<Map<String, Object>> namespace) {
        throw new RuntimeException("Not implemented");
    }
}
