package org.example.gqs.cypher.standard_ast;

import org.example.gqs.MainOptions;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.*;
import java.util.stream.Collectors;

public class With extends CypherClause implements IWithAnalyzer {

    private boolean distinct = false;
    private IExpression condition = null, skip = null, limit = null;
    public List<IExpression> orderBy = new ArrayList<>();
    List<Boolean> isOrderByDesc = new ArrayList<>();
    public boolean usedAggregation = false;
    public long repeatTimes = 1;

    public With(){
        super(false);
    }

    public void updateProvideAndRequire() {
        provide = new HashSet<>();
        require = new HashSet<>();
        for (IRet ret : getReturnList()) {
            if (ret.getIdentifier() instanceof Alias) {
                provide.add((IIdentifier) ((Alias) ret.getIdentifier()));
                IIdentifier identifier = ret.getIdentifier();
                if (identifier instanceof Alias && ((Alias) (identifier)).getExpression() instanceof CallExpression && (((CallExpression) ((Alias) (identifier)).getExpression()).getValue() instanceof AbstractNode || ((CallExpression) ((Alias) (identifier)).getExpression()).getValue() instanceof AbstractRelationship)) {
                    if (((CallExpression) ((Alias) (identifier)).getExpression()).getValue() instanceof AbstractNode) {
                        provide.add(new NodeIdentifier(identifier.getName(), (AbstractNode) ((CallExpression) ((Alias) (identifier)).getExpression()).getValue()));
                    } else {
                        provide.add(new RelationIdentifier(identifier.getName(), (AbstractRelationship) ((CallExpression) ((Alias) (identifier)).getExpression()).getValue()));
                    }
                }
            } else if (ret.getIdentifier() instanceof Alias) {
                provide.add((IIdentifier) ((Alias) ret.getIdentifier()));
            } else if (ret.getIdentifier() instanceof IIdentifier) {
                provide.add((IIdentifier) ret.getIdentifier());
            } else {
                throw new RuntimeException("In WITH updating provide: Alias is not an identifier");
            }
        }
        for (IRet ret : getReturnList()) {
            if (ret.getIdentifier() instanceof Alias && ret.getExpression() instanceof GetPropertyExpression) {
                require.addAll((((GetPropertyExpression) ret.getExpression()).reliedContent()));
                require.add(ret.getIdentifier());
            } else if (ret.getExpression() == null && ret.getIdentifier() != null) {
                require.add((IIdentifier) ret.getIdentifier());
            } else if (ret.getExpression() instanceof IdentifierExpression && ret.getIdentifier() instanceof Alias) {
                require.add((IIdentifier) (((IdentifierExpression) ret.getExpression()).getIdentifier()));
                require.add((IIdentifier) ret.getIdentifier());
            } else if (ret.getIdentifier() instanceof Alias && ret.getExpression() != null) {
                require.addAll(((CypherExpression) ret.getExpression()).reliedContent());
                require.add((IIdentifier) ret.getIdentifier());
            } else {
                throw new RuntimeException("In WITH updating require: missing any of these content. The ret.getExpression() is " + ret.getExpression() + " and the ret.getIdentifier() is " + ret.getIdentifier());
            }
        }
        if (condition != null) {
            require.addAll(((CypherExpression) condition).reliedContent());
        }
        if (skip != null) {
            require.addAll(((CypherExpression) skip).reliedContent());
        }
        if (limit != null) {
            require.addAll(((CypherExpression) limit).reliedContent());
        }
        if (orderBy != null) {
            for (IExpression expression : orderBy) {
                require.addAll(((CypherExpression) expression).reliedContent());
            }
        }
    }

    public List<Map<String, Object>> getNamespace(List<Map<String, Object>> expandedNamespace) {
        expandedNamespace = fromExpandNamespace(expandedNamespace, orderBy, isOrderByDesc, limit, skip, distinct, getReturnList(), this);
        Set<String> keptAlias = new HashSet<>();
        for (IRet ret : getReturnList()) {
            if (ret.getIdentifier() instanceof Alias) {
                keptAlias.add(ret.getIdentifier().getName());
            } else if (ret.getIdentifier() instanceof IIdentifier) {
                keptAlias.add(ret.getIdentifier().getName());
            } else {
                throw new RuntimeException("In WITH getNamespace: Identifier is not a valid identifier, but a " + ret.getIdentifier().getClass().getName());
            }
        }
        List<Map<String, Object>> newNamespace = new ArrayList<>();
        for (int i = 0; i < expandedNamespace.size(); i++) {
            newNamespace.add(new HashMap<>());
            for (String key : expandedNamespace.get(i).keySet()) {
                if (keptAlias.contains(key)) {
                    newNamespace.get(i).put(key, expandedNamespace.get(i).get(key));
                }
            }
        }
        expandedNamespace.clear();
        expandedNamespace.addAll(newNamespace);
        return expandedNamespace;
    }


    public Map<String, Object> getNamespace(Map<String, Object> originalNamespace) {
        Map<String, Object> namespace = new HashMap<>();
        List<Map<String, Object>> expandedNamespace = new ArrayList<>();
        repeatTimes = this.expandNamespace(originalNamespace, orderBy, isOrderByDesc, limit, skip, distinct, getReturnList(), expandedNamespace, this);
        for (IRet ret : getReturnList()) {
            if (ret.getIdentifier() instanceof Alias && ((Alias) ret.getIdentifier()).getExpression() instanceof CreateListExpression) {
                List<Object> elementValue = new ArrayList<>();
                for (int i = 0; i < expandedNamespace.size(); i++) {
                    Map<String, Object> currentNamespace = expandedNamespace.get(i);
                    Object currentData = getFromNamespace(new IdentifierExpression((Alias) ret.getIdentifier()), currentNamespace);
                    elementValue.add(currentData);
                }
                ((CreateListExpression) ((Alias) ret.getIdentifier()).getExpression()).elementValue = elementValue;
            }
        }
        boolean usedAggregation = false;
        for (IRet ret : getReturnList()) {
            if (ret.getIdentifier() instanceof Alias && ((Alias) ret.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression) ((Alias) ret.getIdentifier()).getExpression()).functionName)) {
                List<Object> elementValue = new ArrayList<>();
                for (int i = 0; i < expandedNamespace.size(); i++) {
                    Map<String, Object> currentNamespace = expandedNamespace.get(i);
                    Object currentData = getFromNamespace(new IdentifierExpression((Alias) ret.getIdentifier()), currentNamespace);
                    elementValue.add(currentData);
                }
                usedAggregation = true;
                ((CallExpression) ((Alias) ret.getIdentifier()).getExpression()).setElementValue(elementValue);
            }
        }


        for (IRet ret : getReturnList()) {
            if (ret.getIdentifier() instanceof Alias) {
                namespace.put(ret.getIdentifier().getName(), ((Alias) ret.getIdentifier()));
            } else if (ret.getIdentifier() instanceof IIdentifier) {
                if (ret.getIdentifier() instanceof NodeIdentifier) {
                    namespace.put(ret.getIdentifier().getName(), ((NodeIdentifier) ret.getIdentifier()).actualNode);
                } else if (ret.getIdentifier() instanceof RelationIdentifier) {
                    namespace.put(ret.getIdentifier().getName(), ((RelationIdentifier) ret.getIdentifier()).actualRelationship);
                } else {
                    throw new RuntimeException("In WITH getNamespace: Identifier is not a valid identifier, but a " + ret.getIdentifier().getClass().getName());
                }
            } else {
                throw new RuntimeException("In WITH getNamespace, found something that is not alias or identifier, but a " + ret.getIdentifier().getClass().getName());
            }
        }
        if (!usedAggregation && distinct == false) {
            for (String key : originalNamespace.keySet()) {
                if (!namespace.containsKey(key) && originalNamespace.get(key) instanceof Alias && ((Alias) originalNamespace.get(key)).getExpression() instanceof CreateListExpression) {
                    CreateListExpression current = (CreateListExpression) ((Alias) originalNamespace.get(key)).getExpression();
                    if (current.elementValue != null && current.elementValue.size() > 0) {
                        current.elementValue = new ArrayList<>(Collections.nCopies(current.elementValue.size(), 0L));
                    } else {
                        current.elementValue = new ArrayList<>(Collections.nCopies(current.getValue().size(), 0L));
                    }
                    namespace.put(key, ((Alias) originalNamespace.get(key)));
                }
            }
        }
        originalNamespace.clear();
        originalNamespace.putAll(namespace);
        return namespace;
    }

    @Override
    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public void setDistinct(boolean isDistinct) {
        this.distinct = isDistinct;
    }

    @Override
    public List<IRet> getReturnList() {
        return symtab.getAliasDefinitions();
    }

    @Override
    public void setReturnList(List<IRet> returnList) {
        this.symtab.setAliasDefinition(returnList);
    }

    @Override
    public IExpression getCondition() {
        return condition;
    }

    @Override
    public void setCondition(IExpression condtion) {
        this.condition = condtion;
    }

    @Override
    public void setOrderBy(List<IExpression> expression, boolean isDesc) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<IExpression> getOrderByExpressions() {
        return orderBy;
    }

    @Override
    public List<Boolean> isOrderByDesc() {
        return isOrderByDesc;
    }

    @Override
    public void setOrderBy(List<IExpression> expressions, List<Boolean> isDesc) {
        orderBy = expressions;
        isOrderByDesc = isDesc;
    }
    public List<IExpression> getOrderBy() {
        return orderBy;
    }

    @Override
    public void setLimit(IExpression expression) {
        limit = expression;
    }

    @Override
    public IExpression getLimit() {
        return limit;
    }

    @Override
    public void setSkip(IExpression expression) {
        skip = expression;
    }

    @Override
    public IExpression getSkip() {
        return skip;
    }

    @Override
    public IWithAnalyzer toAnalyzer() {
        return this;
    }

    @Override
    public ICypherClause getCopy() {
        With with = new With();
        with.distinct = distinct;
        if(condition != null){
            with.condition = condition.getCopy();
        }
        else {
            with.condition = null;
        }
        if(symtab != null){
            with.symtab.setPatterns(symtab.getPatterns().stream().map(p->p.getCopy()).collect(Collectors.toList()));
            with.symtab.setAliasDefinition(symtab.getAliasDefinitions().stream().map(a->a.getCopy()).collect(Collectors.toList()));
        }
        if(skip != null){
            with.skip = skip.getCopy();
        }
        if(limit != null){
            with.limit = limit.getCopy();
        }
        with.orderBy = new ArrayList<>(orderBy.stream().map(e->e.getCopy()).collect(Collectors.toList()));
        with.isOrderByDesc = this.isOrderByDesc;
        with.distinct = this.distinct;
        if(require != null){
            with.require = new HashSet<>();
            for(IIdentifier identifier: require){
                with.require.add(identifier.getCopy());
            }
        }
        if(provide != null){
            with.provide = new HashSet<>();
            for(IIdentifier identifier: provide){
                with.provide.add(identifier.getCopy());
            }
        }
        if(usedAggregation){
            with.usedAggregation = true;
        }
        with.repeatTimes = repeatTimes;
        return with;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("WITH ");
        if (distinct) {
            sb.append("DISTINCT ");
        }
        List<IRet> returnList = getReturnList();
        for (int i = 0; i < returnList.size(); i++) {
            returnList.get(i).toTextRepresentation(sb);
            if (i != returnList.size() - 1) {
                sb.append(", ");
            }
        }
        if (orderBy != null && orderBy.size() != 0) {
            sb.append(" ORDER BY ");
            for (int i = 0; i < orderBy.size(); i++) {
                orderBy.get(i).toTextRepresentation(sb);
                if (isOrderByDesc.get(i)) {
                    sb.append(" DESC");
                }
                if (i != orderBy.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        if (skip != null) {
            sb.append(" SKIP ");
            skip.toTextRepresentation(sb);
        }
        if (limit != null) {
            sb.append(" LIMIT ");
            limit.toTextRepresentation(sb);
        }
        if (condition != null) {
            sb.append(" WHERE ");
            condition.toTextRepresentation(sb);
        }
    }

    @Override
    public List<IPattern> getLocalPatternContainsIdentifier(IIdentifier identifier) {
        return new ArrayList<>();
    }

    @Override
    public IWith getSource() {
        return this;
    }
}
