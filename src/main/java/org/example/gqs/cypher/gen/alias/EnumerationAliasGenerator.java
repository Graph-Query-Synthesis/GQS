package org.example.gqs.cypher.gen.alias;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.analyzer.*;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.gen.EnumerationSeq;
import org.example.gqs.cypher.gen.expr.EnumerationExpressionGenerator;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EnumerationAliasGenerator<S extends CypherSchema<?,?>> extends BasicAliasGenerator<S> {
    private EnumerationSeq enumerationSeq;

    public EnumerationAliasGenerator(S schema, IIdentifierBuilder identifierBuilder, EnumerationSeq enumerationSeq) {
        super(schema, identifierBuilder);
        this.enumerationSeq = enumerationSeq;
    }

    @Override
    public List<IRet> generateReturnAlias(IReturnAnalyzer returnClause, IIdentifierBuilder identifierBuilder, S schema) {

        List<IRet> results = new ArrayList<>();
        List<INodeAnalyzer> idNode = returnClause.getExtendableNodeIdentifiers();
        List<IRelationAnalyzer> idRelation = returnClause.getExtendableRelationIdentifiers();
        List<IAliasAnalyzer> idAlias = returnClause.getExtendableAliases();


        long sizeOfAlias = idAlias.size();
        long sizeOfNode = idNode.size();
        long sizeOfRelation = idRelation.size();

        long numOfExpressions = enumerationSeq.getRange(2) + 1;
        ArrayList<IExpression> orderByExpression = new ArrayList<>();

        for (int i = 0; i < numOfExpressions; i++) {
            Ret result = null;
            if (i == 0) {
                long kind = enumerationSeq.getRange(6);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = enumerationSeq.getElement(idAlias);
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1 || kind == 2) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = enumerationSeq.getElement(idNode);
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);

                        if (props.size() > 0) {
                            IPropertyInfo prop = enumerationSeq.getElement(props);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 3 || kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = enumerationSeq.getElement(idRelation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            if (props.size() > 0) {
                                IPropertyInfo prop = enumerationSeq.getElement(props);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else if (kind == 5) {
                    CypherType type = enumerationSeq.getElement(Arrays.asList(CypherType.LIST));
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new EnumerationExpressionGenerator<>(returnClause, enumerationSeq, schema).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                } else {
                    result = Ret.createStar();
                }
            } else {
                long kind = enumerationSeq.getRange(5);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = enumerationSeq.getElement(idAlias);
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                }
                else if (kind == 1 || kind == 2) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = enumerationSeq.getElement(idNode);
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        if (props.size() > 0) {
                            IPropertyInfo prop = enumerationSeq.getElement(props);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 3 || kind == 4){
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = enumerationSeq.getElement(idRelation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            if (props.size() > 0) {
                                IPropertyInfo prop = enumerationSeq.getElement(props);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type = enumerationSeq.getElement(Arrays.asList(CypherType.LIST));
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new EnumerationExpressionGenerator<>(returnClause, enumerationSeq, schema).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                }
            }
            if (result != null) {
                boolean flag = true;
                for (IRet res: results) {
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
        returnClause.setDistinct(enumerationSeq.getDecision());

        if (enumerationSeq.getDecision()) {
            long numOfOrderBy = enumerationSeq.getRange(results.size()) + 1;
            while (orderByExpression.size() > numOfOrderBy) {
                orderByExpression.remove(enumerationSeq.getRange(orderByExpression.size()));
            }
            if (orderByExpression.size() > 0) {
                returnClause.setOrderBy(orderByExpression, enumerationSeq.getDecision());
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
        List<IRet> results = new ArrayList<>();
        List<INodeAnalyzer> idNode = withClause.getExtendableNodeIdentifiers();
        List<IRelationAnalyzer> idRelation = withClause.getExtendableRelationIdentifiers();
        List<IAliasAnalyzer> idAlias = withClause.getExtendableAliases();


        long sizeOfAlias = idAlias.size();
        long sizeOfNode = idNode.size();
        long sizeOfRelation = idRelation.size();
        long propOfNode = 0;
        long propOfRelation = 0;

        long numOfExpressions = enumerationSeq.getRange(5) + 1;
        ArrayList<IExpression> orderByExpression = new ArrayList<>();

        for (int i = 0; i < numOfExpressions; i++) {
            Ret result = null;
            if (i == 0) {
                long kind = enumerationSeq.getRange(6);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = enumerationSeq.getElement(idAlias);
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = enumerationSeq.getElement(idNode);
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
                        IRelationAnalyzer relation = enumerationSeq.getElement(idRelation);
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
                        INodeAnalyzer node = enumerationSeq.getElement(idNode);
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        if (props.size() > 0) {
                            IPropertyInfo prop = enumerationSeq.getElement(props);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = enumerationSeq.getElement(idRelation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            if (props.size() > 0) {
                                IPropertyInfo prop = enumerationSeq.getElement(props);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else if (kind == 5) {
                    CypherType type = enumerationSeq.getElement(Arrays.asList(CypherType.LIST));
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new EnumerationExpressionGenerator<>(withClause, enumerationSeq, schema).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                } else {
                    result = Ret.createStar();
                }
            } else {
                long kind = enumerationSeq.getRange(6);
                if (kind == 0) {
                    if (sizeOfAlias > 0) {
                        IAliasAnalyzer alias = enumerationSeq.getElement(idAlias);
                        result = Ret.createAliasRef(alias);
                        orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                    }
                } else if (kind == 1) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = enumerationSeq.getElement(idNode);
                        result = Ret.createNodeRef(node);
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        for(int j = 0; j < props.size(); j++) {
                            IPropertyInfo prop = props.get(j);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            orderByExpression.add(exp);
                        }
                    }
                } else if (kind == 2) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = enumerationSeq.getElement(idRelation);
                        result = Ret.createRelationRef(relation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            for(int j = 0; j < props.size(); j++) {
                                IPropertyInfo prop = props.get(j);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                orderByExpression.add(exp);
                            }
                        }
                    }
                } else if (kind == 3) {
                    if (sizeOfNode > 0) {
                        INodeAnalyzer node = enumerationSeq.getElement(idNode);
                        List<IPropertyInfo> props = node.getAllPropertiesAvailable(schema);
                        if (props.size() > 0) {
                            IPropertyInfo prop = enumerationSeq.getElement(props);
                            IdentifierExpression ie = new IdentifierExpression(node);
                            GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                            result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                            orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                        }
                    }
                } else if (kind == 4) {
                    if (sizeOfRelation > 0) {
                        IRelationAnalyzer relation = enumerationSeq.getElement(idRelation);
                        if (relation.isSingleRelation()) {
                            List<IPropertyInfo> props = relation.getAllPropertiesAvailable(schema);
                            if (props.size() > 0) {
                                IPropertyInfo prop = enumerationSeq.getElement(props);
                                IdentifierExpression ie = new IdentifierExpression(relation);
                                GetPropertyExpression exp = new GetPropertyExpression(ie, prop.getKey());
                                result = Ret.createNewExpressionAlias(identifierBuilder, exp);
                                orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                            }
                        }
                    }
                } else {
                    CypherType type = enumerationSeq.getElement(Arrays.asList(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN, CypherType.NODE, CypherType.RELATION));
                    result = Ret.createNewExpressionAlias(identifierBuilder,
                            new EnumerationExpressionGenerator<>(withClause, enumerationSeq, schema).generateFunction(type));
                    orderByExpression.add(new IdentifierExpression(result.getIdentifier()));
                }
            }
            if (result != null) {
                boolean flag = true;
                for (IRet res: results) {
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
            results.add(Ret.createStar());
        }
        withClause.setDistinct(enumerationSeq.getDecision());

        if (enumerationSeq.getDecision()) {
            long numOfOrderBy = enumerationSeq.getRange(results.size()) + 1;
            while (orderByExpression.size() > numOfOrderBy) {
                orderByExpression.remove(enumerationSeq.getRange(orderByExpression.size()));
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
