package org.example.gqs.cypher.standard_ast;

import org.example.gqs.MainOptions;
import org.example.gqs.cypher.ast.ICypherClause;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.ast.analyzer.*;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.*;

public abstract class CypherClause implements IClauseAnalyzer {
    protected final Symtab symtab;
    protected ICypherClause nextClause = null, prevClause = null;
    List<IAliasAnalyzer> extendableAliases = null;
    public Set<IIdentifier> provide, require;
    public boolean canSkip = false;

    public abstract void updateProvideAndRequire();

    public CypherClause(boolean extendParent){
        symtab = new Symtab(this, extendParent);
    }

    @Override
    public void setNextClause(ICypherClause next) {
        this.nextClause = next;
        if(next != null) {
            next.setPrevClause(this);
        }
    }
    public Object getFromNamespace(IExpression expression, Map<String, Object> namespace)
    {
        Object currentValue;
        if(expression instanceof IdentifierExpression && ((IdentifierExpression)expression).getIdentifier() instanceof Alias)
        {
            String aliasname = ((Alias)((IdentifierExpression)expression).getIdentifier()).getName();
            if(namespace.get(aliasname) instanceof AbstractNode || namespace.get(aliasname) instanceof AbstractRelationship)
            {
                currentValue = namespace.get(aliasname);
            }
            else if (((Alias)namespace.get(aliasname)).getExpression() instanceof GetPropertyExpression)
            {
                currentValue = ((GetPropertyExpression)((Alias) namespace.get(aliasname)).getExpression()).getValue();
            }
            else if (((Alias)namespace.get(aliasname)).getExpression() instanceof ConstExpression)
            {
                currentValue = ((ConstExpression)((Alias) namespace.get(aliasname)).getExpression()).getValue();
            }
            else if (((Alias)namespace.get(aliasname)).getExpression() instanceof CallExpression && ((CallExpression)((Alias) namespace.get(aliasname)).getExpression()).aggregationParams != null)
            {
                currentValue = ((CallExpression)((Alias)namespace.get(aliasname)).getExpression()).calculateValue();
            }
            else if (((Alias) namespace.get(aliasname)).getExpression() instanceof CallExpression && !MainOptions.isAggregateFunction(((CallExpression)((Alias) namespace.get(aliasname)).getExpression()).functionName))
            {
                currentValue = ((CallExpression)((Alias)namespace.get(aliasname)).getExpression()).getValue();
            }
            else if (((Alias)namespace.get(aliasname)).getExpression() instanceof IdentifierExpression && ((IdentifierExpression)((Alias)namespace.get(aliasname)).getExpression()).getIdentifier() instanceof Alias)
            {
                currentValue = getFromNamespace(((IdentifierExpression)((Alias)namespace.get(aliasname)).getExpression()), namespace);
            }
            else
            {
                throw new RuntimeException("In WITH getNamespace, found something that is not a GetPropertyExpression or a ConstExpression, but a "+namespace.get(aliasname).getClass().getName());
            }
        }
        else if (expression instanceof IdentifierExpression && (((IdentifierExpression) expression).getIdentifier() instanceof NodeIdentifier || ((IdentifierExpression) expression).getIdentifier() instanceof RelationIdentifier))
        {
            if(((IdentifierExpression) expression).getIdentifier() instanceof NodeIdentifier)
            {
                currentValue = ((NodeIdentifier)((IdentifierExpression) expression).getIdentifier()).actualNode;
            }
            else
            {
                currentValue = ((RelationIdentifier)((IdentifierExpression) expression).getIdentifier()).actualRelationship;
            }
        }
        else if (expression instanceof GetPropertyExpression)
        {
            String nodename = ((IdentifierExpression)((GetPropertyExpression)expression).getFromExpression()).getIdentifier().getName();
            String propertyname = ((GetPropertyExpression)expression).getPropertyName();
            currentValue = (namespace.get(nodename));
            if(currentValue instanceof AbstractNode)
            {
                currentValue = ((AbstractNode)currentValue).getProperties().get(propertyname);
            }
            else if(currentValue instanceof AbstractRelationship)
            {
                currentValue = ((AbstractRelationship)currentValue).getProperties().get(propertyname);
            }
            else if (currentValue instanceof  Alias)
            {
                Object temp = ((Alias) currentValue).getValue();
                if(temp instanceof AbstractNode)
                {
                    currentValue = ((AbstractNode)temp).getProperties().get(propertyname);
                }
                else if(temp instanceof AbstractRelationship)
                {
                    currentValue = ((AbstractRelationship)temp).getProperties().get(propertyname);
                }
                else
                {
                    throw new RuntimeException("In WITH getNamespace, found something that is not a node or a relationship, but a "+temp.getClass().getName());
                }
            }
            else
            {
                throw new RuntimeException("In WITH getNamespace, found something that is not a node or a relationship, but a "+currentValue.getClass().getName());
            }
        }
        else if (expression instanceof ConstExpression)
        {
            currentValue = ((ConstExpression)expression).getValue();
        }
        else if (expression instanceof BinaryNumberExpression)
            currentValue = ((BinaryNumberExpression)expression).getValue();
        else
        {
            throw new RuntimeException("In WITH getNamespace, found something that is not alias or identifier, but a "+expression.getClass().getName());
        }
        return currentValue;
    }

    boolean mapEqual(Map<String, Object> a, Map<String, Object> b)
    {
        if(a.size() != b.size())
            return false;
        for(String key: a.keySet())
        {
            if(!a.get(key).equals(b.get(key)))
                return false;
        }
        return true;
    }

    boolean notInSet(Map<String, Object> current, List<Map<String, Object>> set)
    {
        if(set.size() == 0)
            return true;
        for(Map<String, Object> item: set)
        {
            if(item.equals(current))
                return false;
        }
        return true;
    }

    public List<Map<String, Object>> fromExpandNamespace(List<Map<String, Object>> expandedNamespace, List<IExpression> orderBy, List<Boolean> isOrderByDesc, IExpression limit, IExpression skip, boolean distinct, List<IRet> RetList, CypherClause clause){
        boolean aggregationFunction = false;
        long repeatTimes = 1;
        for (IRet ret : RetList) {
            if (ret.getExpression() instanceof CallExpression) {
                if (MainOptions.isAggregateFunction(((CallExpression) ret.getExpression()).functionName) && ((CallExpression) ret.getExpression()).aggregationParams == null){
                    aggregationFunction = true;
                    break;
                }
            }
        }

        if(aggregationFunction) {
            Map<Map<String, Object>, Map<String, List>> nonaggregation = new HashMap<>();
            Set<IIdentifier> nonaggregationkey = new HashSet<>();
            Set<IIdentifier> aggregationkey = new HashSet<>();
            for (IRet ret : RetList) {
                if (ret.getExpression() instanceof CallExpression && ((CallExpression) ret.getExpression()).aggregationParams == null) {
                    if (MainOptions.isAggregateFunction(((CallExpression) ret.getExpression()).functionName)) {
                        aggregationkey.add(ret.getIdentifier());
                    }
                    else{
                        nonaggregationkey.add(ret.getIdentifier());
                    }
                }
                else {
                    nonaggregationkey.add(ret.getIdentifier());
                }
            }
            for(int i = 0; i < expandedNamespace.size(); i++)
            {
                Map<String, Object> currentItem = expandedNamespace.get(i);
                Map<String, Object> extractedMap = new HashMap<>();
                for(IIdentifier key: nonaggregationkey)
                {
                    if(currentItem.containsKey(key.getName()))
                    {
                        extractedMap.put(key.getName(), currentItem.get(key.getName()));
                    }
                    else
                    {
                        if(key instanceof RelationIdentifier || key instanceof NodeIdentifier) {
                            if (key instanceof RelationIdentifier) {
                                extractedMap.put(key.getName(), ((RelationIdentifier) key).actualRelationship);
                            } else {
                                extractedMap.put(key.getName(), ((NodeIdentifier) key).actualNode);
                            }
                        }
                        else if (key instanceof Alias)
                        {
                            extractedMap.put(key.getName(), (((Alias) key)));
                        }
                        else
                        {
                            throw new RuntimeException("In WITH, found something that is not a Alias or a Identifier, but a "+key.getClass().getName());
                        }
                    }
                }
                if (nonaggregation.containsKey(extractedMap)) {
                    for (IIdentifier aggregation : aggregationkey) {

                        Object currentValue = getFromNamespace(((CallExpression) (((Alias) aggregation).getExpression())).params.get(0), currentItem);
                        nonaggregation.get(extractedMap).get(aggregation.toString()).add(currentValue);
                    }
                } else {
                    nonaggregation.put(extractedMap, new HashMap<>());
                    for (IIdentifier aggregation : aggregationkey) {
                        nonaggregation.get(extractedMap).put(aggregation.toString(), new ArrayList<>());

                        Object currentValue = getFromNamespace(((CallExpression) (((Alias) aggregation).getExpression())).params.get(0), currentItem);
                        nonaggregation.get(extractedMap).get(aggregation.toString()).add(currentValue);
                    }
                }
            }
            for(Map<String, Object> key : nonaggregation.keySet())
            {
                for(IIdentifier aggregation : aggregationkey)
                {
                    assert(aggregation instanceof Alias && ((Alias)aggregation).getExpression() instanceof CallExpression);
                    CallExpression callExpression = (CallExpression)((Alias) aggregation).getExpression();
                    if(callExpression.aggregationParams == null)
                        callExpression.aggregationParams = new ArrayList<>();
                    callExpression.aggregationParams.add(new ArrayList<>());
                    callExpression.aggregationParams.get(callExpression.aggregationParams.size()-1).addAll(nonaggregation.get(key).get(aggregation.toString()));
                }
            }


            expandedNamespace.clear();
            for(Map<String, Object> key : nonaggregation.keySet())
            {
                expandedNamespace.add(key);
            }

            for (IRet ret : RetList) {
                if (ret.getIdentifier() instanceof Alias && ((Alias)ret.getIdentifier()).getExpression() instanceof CallExpression) {
                    if (MainOptions.isAggregateFunction(((CallExpression) ((Alias)ret.getIdentifier()).getExpression()).functionName) && ((CallExpression)(((Alias)ret.getIdentifier()).getExpression())).functionName != "collect") {
                        List<Object> value = (List)((CallExpression)((Alias)ret.getIdentifier()).getExpression()).calculateValue();
                        int cnt = 0;
                        for (int i = 0; i < expandedNamespace.size(); i++) {
                            expandedNamespace.get(i).put(ret.getIdentifier().getName(), new Alias(ret.getIdentifier().getName(), new ConstExpression(value.get(cnt))));
                            cnt++;
                            if (cnt == value.size())
                                cnt = 0;
                        }
                    }
                    else if (((CallExpression)ret.getExpression()).functionName == "collect")
                    {
                        List<Object> value = (List<Object>) ((CallExpression) ret.getExpression()).calculateValue();
                        for (int i = 0; i < expandedNamespace.size(); i++) {
                            expandedNamespace.get(i).put(ret.getIdentifier().getName(), new Alias(ret.getIdentifier().getName(), new ConstExpression(value)));
                        }
                    }
                }
            }
        }
        else
        {

            for(IRet ret:RetList)
            {
                if(ret.getIdentifier() instanceof Alias && ((Alias)ret.getIdentifier()).getExpression() instanceof CreateListExpression)
                {

                }
                else if (ret.getIdentifier() instanceof Alias && ((Alias)ret.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression)((Alias)ret.getIdentifier()).getExpression()).functionName)){

                }
                else
                {
                    for(int i = 0; i < expandedNamespace.size(); i++)
                    {
                        if(ret.isAll() == true)
                            break;
                        if(ret.getIdentifier() instanceof Alias)
                        {
                            expandedNamespace.get(i).put(ret.getIdentifier().getName(), ret.getIdentifier());
                        }
                        else if (ret.getIdentifier() instanceof NodeIdentifier || ret.getIdentifier() instanceof RelationIdentifier)
                        {
                            if(ret.getIdentifier() instanceof NodeIdentifier)
                            {
                                expandedNamespace.get(i).put(ret.getIdentifier().getName(), ((NodeIdentifier)ret.getIdentifier()).actualNode);
                            }
                            else
                            {
                                expandedNamespace.get(i).put(ret.getIdentifier().getName(), ((RelationIdentifier)ret.getIdentifier()).actualRelationship);
                            }
                        }
                        else
                        {
                            throw new RuntimeException("In WITH, found something that is not a Alias or a Identifier, but a "+ret.getIdentifier().getClass().getName());
                        }
                    }
                }
            }

        }

        if(distinct == true || aggregationFunction == true)
        {










            List<Map<String, Object>> distinctSet = new ArrayList<>();
            List<Map<String, Object>> deduplicated = new ArrayList<>();
            for(int i = 0; i < expandedNamespace.size(); i++) {
                Map<String, Object> currentItem = expandedNamespace.get(i);
                Map<String, Object> currentReturn = new HashMap<>();
                for (IRet ret : RetList) {
                    if (ret.getIdentifier() instanceof Alias) {
                        Object value = getFromNamespace(new IdentifierExpression(ret.getIdentifier()), currentItem);
                        currentReturn.put(ret.getIdentifier().getName(), value);
                    } else if (ret.getExpression() == null){
                        Object value = getFromNamespace(new IdentifierExpression(ret.getIdentifier()), currentItem);
                        currentReturn.put(ret.getIdentifier().getName(), value);
                    }
                    else
                    {
                        Object value = getFromNamespace(ret.getExpression(), currentItem);
                        currentReturn.put(ret.getIdentifier().getName(), value);
                    }
                }
                if (notInSet(currentReturn, distinctSet)) {
                    distinctSet.add(currentReturn);
                    deduplicated.add(currentItem);
                }
            }
            expandedNamespace.clear();
            expandedNamespace.addAll(new ArrayList<>(deduplicated));
        }


        if(orderBy != null && orderBy.size() > 0)
        {


            expandedNamespace.sort((o1, o2) -> {
                Object value1, value2;
                for(int i = 0; i < orderBy.size(); i++)
                {
                    IExpression expression = orderBy.get(i);
                    boolean isOrder = isOrderByDesc.get(i);
                    value1 = getFromNamespace(expression, o1);
                    value2 = getFromNamespace(expression, o2);
                    int result = ((Comparable)value1).compareTo(value2);
                    if(result != 0)
                    {
                        if(isOrder)
                            return -result;
                        else
                            return result;
                    }
                }
                return 0;
            });

            long skipValue = 0, limitValue = expandedNamespace.size();
            if(limit != null)
            {
                Object temp = ((CypherExpression)limit).getValue();
                if (temp instanceof Long)
                    limitValue = ((Long)temp).intValue();
                else
                    limitValue = (long)temp;
            }
            if(skip != null)
            {
                Object temp = ((CypherExpression)skip).getValue();
                if (temp instanceof Long)
                    skipValue = ((Long)temp).intValue();
                else
                    skipValue = (long)temp;
            }
            if(skipValue > expandedNamespace.size()) {
                skipValue = expandedNamespace.size()-1;
                if(clause instanceof Return)
                    ((Return)clause).setSkip(new ConstExpression(skipValue));
                else if (clause instanceof With)
                    ((With)clause).setSkip(new ConstExpression(skipValue));
            }
            if(skipValue + limitValue > expandedNamespace.size()) {
                limitValue = expandedNamespace.size() - skipValue;
                if(clause instanceof Return)
                    ((Return)clause).setLimit(new ConstExpression(limitValue));
                else if (clause instanceof With)
                    ((With)clause).setLimit(new ConstExpression(limitValue));
            }
            if(expandedNamespace.size() - skipValue <= 0)
            {
                limitValue = 1;
                skipValue = 0;
                if(clause instanceof Return) {
                    ((Return) clause).setLimit(new ConstExpression(limitValue));
                    ((Return) clause).setSkip(new ConstExpression(skipValue));
                }
                else if (clause instanceof With) {
                    ((With) clause).setLimit(new ConstExpression(limitValue));
                    ((With) clause).setSkip(new ConstExpression(skipValue));
                }
            }
            List<Map<String, Object>> newExpandedNamespace = new ArrayList<>(expandedNamespace.subList((int) skipValue, (int) (skipValue + limitValue)));
            expandedNamespace.clear();
            expandedNamespace.addAll(newExpandedNamespace);

        }
        for (IRet ret : RetList) {
            if (ret.getIdentifier() instanceof Alias && ((Alias)ret.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression)((Alias)ret.getIdentifier()).getExpression()).functionName)){
                List<Object> elementValue = new ArrayList<>();
                for(int i = 0; i < expandedNamespace.size(); i++)
                {
                    Map<String, Object> currentNamespace = expandedNamespace.get(i);
                    Object currentData = getFromNamespace(new IdentifierExpression((Alias)ret.getIdentifier()), currentNamespace);
                    elementValue.add(currentData);
                }
                ((CallExpression)((Alias)ret.getIdentifier()).getExpression()).setElementValue(elementValue);
            }
        }
        return expandedNamespace;
    }

    public long expandNamespace(Map<String, Object> originalNamespace, List<IExpression> orderBy, List<Boolean> isOrderByDesc, IExpression limit, IExpression skip, boolean distinct, List<IRet> RetList, List<Map<String, Object>> expandedNamespace, CypherClause clause){
        boolean aggregationFunction = false;
        long repeatTimes = 1;
        for (IRet ret : RetList) {
            if (ret.getExpression() instanceof CallExpression) {
                if (MainOptions.isAggregateFunction(((CallExpression) ret.getExpression()).functionName) && ((CallExpression) ret.getExpression()).aggregationParams == null){
                    aggregationFunction = true;
                    break;
                }
            }
        }





        Set<Alias> toExpand = new HashSet<>();
        Set<Alias> toDistribute = new HashSet<>();
        Set<Alias> expanded = new HashSet<>();
        expandedNamespace.add(new HashMap<>());
        for(String key: originalNamespace.keySet())
        {
            Object currentValue = originalNamespace.get(key);
            if(currentValue instanceof Alias && ((Alias)currentValue).getExpression() instanceof CreateListExpression && (((CreateListExpression)((Alias)currentValue).getExpression()).elementValue== null || ((CreateListExpression)((Alias)currentValue).getExpression()).elementValue.size() == 0))
            {
                toExpand.add((Alias)currentValue);
            }
            else if (currentValue instanceof Alias && ((Alias)currentValue).getExpression() instanceof CallExpression && (MainOptions.isAggregateFunction(((CallExpression)((Alias)currentValue).getExpression()).functionName)))
            {
                toDistribute.add((Alias)currentValue);
            }
            else if(currentValue instanceof Alias && ((Alias)currentValue).getExpression() instanceof CreateListExpression && (((CreateListExpression)((Alias)currentValue).getExpression()).elementValue!= null && ((CreateListExpression)((Alias)currentValue).getExpression()).elementValue.size() > 0))
            {

                toExpand.add((Alias)(currentValue));
            }
            else
            {
                expandedNamespace.get(0).put(key, currentValue);
            }
        }

        if(expanded.size() > 0)
        {
            boolean firstAlias = true;
            for(Alias alias: expanded)
            {
                if(firstAlias)
                {
                    firstAlias = false;
                    for(int i = 0; i < (((CreateListExpression)alias.getExpression()).elementValue.size()-1); i++)
                    {
                        expandedNamespace.add(new HashMap<>(expandedNamespace.get(0)));
                    }
                }
                else
                {
                    List<Map<String, Object>> newNamespace  = new ArrayList<>();
                    for(int i = 0; i < expandedNamespace.size(); i++)
                    {
                        newNamespace.add(new HashMap<>(expandedNamespace.get(i)));
                    }

                    for(int j = 0; j < (((CreateListExpression)alias.getExpression()).elementValue.size()) -1; j++)
                    {
                        for(int k = 0; k < newNamespace.size(); k++)
                        {
                            expandedNamespace.add(new HashMap<>(newNamespace.get(k)));
                        }
                    }
                }




                List<Object> value = ((CreateListExpression)alias.getExpression()).getValue();
                int cnt = 0;
                for(int i = 0; i < expandedNamespace.size(); i++)
                {
                    expandedNamespace.get(i).put(alias.getName(), new Alias(alias.getName(), new ConstExpression(value.get(cnt))));
                    cnt++;
                    if(cnt == value.size())
                        cnt = 0;
                }
            }
        }



        for(Alias alias: toExpand)
        {
            List<Object> list = ((CreateListExpression)alias.getExpression()).getValue();
            List<Map<String, Object>> newNamespace  = new ArrayList<>();
            for(int i = 0; i < expandedNamespace.size(); i++)
            {
                newNamespace.add(new HashMap<>(expandedNamespace.get(i)));
            }

            for(int j = 0; j < list.size() -1; j++)
            {
                for(int k = 0; k < newNamespace.size(); k++)
                {
                    expandedNamespace.add(new HashMap<>(newNamespace.get(k)));
                }
            }

            int cnt = 0;
            for(int i = 0; i < expandedNamespace.size(); i++)
            {
                expandedNamespace.get(i).put(alias.getName(), new Alias(alias.getName(), new ConstExpression(list.get(cnt))));
                cnt++;
                if(cnt == list.size())
                    cnt = 0;
            }
        }
        for(Alias alias: toDistribute)
        {
            CallExpression callExpression = (CallExpression)alias.getExpression();
            Object value = callExpression.calculateValue();
            if(value instanceof List)
            {
                List<Object> list = (List<Object>)value;
                int cnt = 0;
                for(int i = 0; i < expandedNamespace.size(); i++)
                {
                    expandedNamespace.get(i).put(alias.getName(), new Alias(alias.getName(), new ConstExpression(list.get(cnt))));
                    cnt++;
                    if(cnt == list.size())
                        cnt = 0;
                }
            }
            else
            {
                for(int i = 0; i < expandedNamespace.size(); i++)
                {
                    expandedNamespace.get(i).put(alias.getName(), new Alias(alias.getName(), new ConstExpression(value)));
                }
            }
        }


        if(aggregationFunction) {
            Map<Map<String, Object>, Map<String, List>> nonaggregation = new HashMap<>();
            Set<IIdentifier> nonaggregationkey = new HashSet<>();
            Set<IExpression> aggregationkey = new HashSet<>();
            for (IRet ret : RetList) {
                if (ret.getExpression() instanceof CallExpression && ((CallExpression) ret.getExpression()).aggregationParams == null) {
                    if (MainOptions.isAggregateFunction(((CallExpression) ret.getExpression()).functionName)) {
                        aggregationkey.add(((CallExpression)(ret.getExpression())));
                    }
                    else{
                        nonaggregationkey.add(ret.getIdentifier());
                    }
                }
                else {
                    nonaggregationkey.add(ret.getIdentifier());
                }
            }
            for(int i = 0; i < expandedNamespace.size(); i++)
            {
                Map<String, Object> currentItem = expandedNamespace.get(i);
                Map<String, Object> extractedMap = new HashMap<>();
                for(IIdentifier key: nonaggregationkey)
                {
                   if(currentItem.containsKey(key.getName()))
                   {
                       extractedMap.put(key.getName(), currentItem.get(key.getName()));
                   }
                   else
                   {
                       if(key instanceof RelationIdentifier || key instanceof NodeIdentifier)
                       {
                            extractedMap.put(key.getName(), key);
                       }
                       else if (key instanceof Alias)
                       {
                           extractedMap.put(key.getName(), (((Alias) key)));
                       }
                       else
                       {
                           throw new RuntimeException("In WITH, found something that is not a Alias or a Identifier, but a "+key.getClass().getName());
                       }
                   }
                }
                if(nonaggregation.containsKey(extractedMap))

                {
                    for(IExpression aggregation : aggregationkey)
                    {

                        Object currentValue = getFromNamespace(((CallExpression)aggregation).params.get(0), currentItem);
                        nonaggregation.get(extractedMap).get(aggregation.toString()).add(currentValue);
                    }
                }
                else
                {
                    nonaggregation.put(extractedMap, new HashMap<>());
                    for(IExpression aggregation : aggregationkey)
                    {
                        nonaggregation.get(extractedMap).put(aggregation.toString(), new ArrayList<>());

                        Object currentValue = getFromNamespace(((CallExpression)aggregation).params.get(0), currentItem);
                        nonaggregation.get(extractedMap).get(aggregation.toString()).add(currentValue);
                    }
                }
            }
            for(Map<String, Object> key : nonaggregation.keySet())
            {
                for(IExpression aggregation : aggregationkey)
                {
                    assert(aggregation instanceof CallExpression);
                    CallExpression callExpression = (CallExpression)aggregation;
                    if(callExpression.aggregationParams == null)
                        callExpression.aggregationParams = new ArrayList<>();
                    callExpression.aggregationParams.add(new ArrayList<>());
                    callExpression.aggregationParams.get(callExpression.aggregationParams.size()-1).addAll(nonaggregation.get(key).get(aggregation.toString()));
                }
            }


            expandedNamespace.clear();
            for(Map<String, Object> key : nonaggregation.keySet())
            {
                expandedNamespace.add(key);
            }

            for (IRet ret : RetList) {
                if (ret.getIdentifier() instanceof Alias && ((Alias)ret.getIdentifier()).getExpression() instanceof CallExpression && !toDistribute.contains( ret.getIdentifier() )) {
                    if (MainOptions.isAggregateFunction(((CallExpression) ret.getExpression()).functionName)) {
                        List<Object> value = (List)((CallExpression) ret.getExpression()).calculateValue();
                        int cnt = 0;
                        for (int i = 0; i < expandedNamespace.size(); i++) {
                            expandedNamespace.get(i).put(ret.getIdentifier().getName(), new Alias(ret.getIdentifier().getName(), new ConstExpression(value.get(cnt))));
                            cnt++;
                            if (cnt == value.size())
                                cnt = 0;
                        }
                    }
                    else if (((CallExpression)ret.getExpression()).functionName == "collect")
                    {
                        List<Object> value = (List<Object>) ((CallExpression) ret.getExpression()).calculateValue();
                        for (int i = 0; i < expandedNamespace.size(); i++) {
                            expandedNamespace.get(i).put(ret.getIdentifier().getName(), new Alias(ret.getIdentifier().getName(), new ConstExpression(value)));
                        }
                    }
                }
            }
        }
        else
        {

            for(IRet ret:RetList)
            {
                if(ret.getIdentifier() instanceof Alias && ((Alias)ret.getIdentifier()).getExpression() instanceof CreateListExpression)
                {

                }
                else if (ret.getIdentifier() instanceof Alias && ((Alias)ret.getIdentifier()).getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression)((Alias)ret.getIdentifier()).getExpression()).functionName)){

                }
                else
                {
                    for(int i = 0; i < expandedNamespace.size(); i++)
                    {
                        if(ret.getIdentifier() instanceof Alias)
                        {
                            expandedNamespace.get(i).put(ret.getIdentifier().getName(), ret.getIdentifier());
                        }
                        else if (ret.getIdentifier() instanceof NodeIdentifier || ret.getIdentifier() instanceof RelationIdentifier)
                        {
                            expandedNamespace.get(i).put(ret.getIdentifier().getName(), ret.getIdentifier());
                        }
                        else
                        {
                            throw new RuntimeException("In WITH, found something that is not a Alias or a Identifier, but a "+ret.getIdentifier().getClass().getName());
                        }
                    }
                }
            }

        }
        if(distinct == true || aggregationFunction == true)
        {










            List<Map<String, Object>> distinctSet = new ArrayList<>();
            List<Map<String, Object>> deduplicated = new ArrayList<>();
            for(int i = 0; i < expandedNamespace.size(); i++) {
                Map<String, Object> currentItem = expandedNamespace.get(i);
                Map<String, Object> currentReturn = new HashMap<>();
                for (IRet ret : RetList) {
                    if (ret.getIdentifier() instanceof Alias) {
                        Object value = getFromNamespace(new IdentifierExpression(ret.getIdentifier()), currentItem);
                        currentReturn.put(ret.getIdentifier().getName(), value);
                    } else {
                        Object value = getFromNamespace(ret.getExpression(), currentItem);
                        currentReturn.put(ret.getIdentifier().getName(), value);
                    }
                }
                if (notInSet(currentReturn, distinctSet)) {
                    distinctSet.add(currentReturn);
                    deduplicated.add(currentItem);
                }
            }
            expandedNamespace.clear();
            expandedNamespace.addAll(new ArrayList<>(deduplicated));
        }


        if(orderBy != null && orderBy.size() > 0)
        {


            expandedNamespace.sort((o1, o2) -> {
                Object value1, value2;
                for(int i = 0; i < orderBy.size(); i++)
                {
                    IExpression expression = orderBy.get(i);
                    boolean isOrder = isOrderByDesc.get(i);
                    value1 = getFromNamespace(expression, o1);
                    value2 = getFromNamespace(expression, o2);
                    int result = ((Comparable)value1).compareTo(value2);
                    if(result != 0)
                    {
                        if(isOrder)
                            return -result;
                        else
                            return result;
                    }
                }
                return 0;
            });

            long skipValue = 0, limitValue = expandedNamespace.size();
            if(limit != null)
            {
                Object temp = ((CypherExpression)limit).getValue();
                if (temp instanceof Long)
                    limitValue = ((Long)temp).intValue();
                else
                    limitValue = (long)temp;
            }
            if(skip != null)
            {
                Object temp = ((CypherExpression)skip).getValue();
                if (temp instanceof Long)
                    skipValue = ((Long)temp).intValue();
                else
                    skipValue = (long)temp;
            }
            if(skipValue > expandedNamespace.size()) {
                skipValue = expandedNamespace.size()-1;
                if(clause instanceof Return)
                    ((Return)clause).setSkip(new ConstExpression(skipValue));
                else if (clause instanceof With)
                    ((With)clause).setSkip(new ConstExpression(skipValue));
            }
            if(skipValue + limitValue > expandedNamespace.size()) {
                limitValue = expandedNamespace.size() - skipValue;
                if(clause instanceof Return)
                    ((Return)clause).setLimit(new ConstExpression(limitValue));
                else if (clause instanceof With)
                    ((With)clause).setLimit(new ConstExpression(limitValue));
            }
            if(expandedNamespace.size() - skipValue <= 0)
            {
                limitValue = 1;
                skipValue = 0;
                if(clause instanceof Return) {
                    ((Return) clause).setLimit(new ConstExpression(limitValue));
                    ((Return) clause).setSkip(new ConstExpression(skipValue));
                }
                else if (clause instanceof With) {
                    ((With) clause).setLimit(new ConstExpression(limitValue));
                    ((With) clause).setSkip(new ConstExpression(skipValue));
                }
            }
            List<Map<String, Object>> newExpandedNamespace = new ArrayList<>(expandedNamespace.subList((int) skipValue, (int) (skipValue + limitValue)));
            expandedNamespace.clear();
            expandedNamespace.addAll(newExpandedNamespace);

        }

        return repeatTimes;

    }

    @Override
    public ICypherClause getNextClause() {
        return nextClause;
    }

    @Override
    public void setPrevClause(ICypherClause prev) {
        this.prevClause = prev;
    }

    @Override
    public ICypherClause getPrevClause() {
        return this.prevClause;
    }

    @Override
    public List<IAliasAnalyzer> getLocalAliases() {
        return symtab.getLocalAliasDefs();
    }

    @Override
    public List<INodeAnalyzer> getLocalNodeIdentifiers() {
        return symtab.getLocalNodePatterns();
    }

    @Override
    public List<IRelationAnalyzer> getLocalRelationIdentifiers() {
        return symtab.getLocalRelationPatterns();
    }

    @Override
    public List<IAliasAnalyzer> getAvailableAliases() {
        return symtab.getAvailableAliasDefs();
    }

    @Override
    public List<INodeAnalyzer> getAvailableNodeIdentifiers() {
        return symtab.getAvailableNodePatterns();
    }

    @Override
    public List<IRelationAnalyzer> getAvailableRelationIdentifiers() {
        return symtab.getAvailableRelationPatterns();
    }

    @Override
    public List<IAliasAnalyzer> getExtendableAliases() {
        if(extendableAliases != null){
            return extendableAliases;
        }
        if(prevClause == null)
            return new ArrayList<>();
        extendableAliases =  prevClause.toAnalyzer().getAvailableAliases();
        return extendableAliases;
    }

    @Override
    public List<INodeAnalyzer> getExtendableNodeIdentifiers() {
        if(prevClause == null)
            return new ArrayList<>();
        return prevClause.toAnalyzer().getAvailableNodeIdentifiers();
    }

    @Override
    public List<IRelationAnalyzer> getExtendableRelationIdentifiers() {
        if(prevClause == null)
            return new ArrayList<>();
        return prevClause.toAnalyzer().getAvailableRelationIdentifiers();
    }

    @Override
    public List<IIdentifierAnalyzer> getAvailableIdentifiers(){
        List<IIdentifierAnalyzer> identifierAnalyzers = new ArrayList<>();
        identifierAnalyzers.addAll(getAvailableNodeIdentifiers());
        identifierAnalyzers.addAll(getAvailableRelationIdentifiers());
        identifierAnalyzers.addAll(getAvailableAliases());
        return identifierAnalyzers;
    }

    @Override
    public List<IIdentifierAnalyzer> getLocalIdentifiers(){
        List<IIdentifierAnalyzer> identifierAnalyzers = new ArrayList<>();
        identifierAnalyzers.addAll(getLocalNodeIdentifiers());
        identifierAnalyzers.addAll(getLocalRelationIdentifiers());
        identifierAnalyzers.addAll(getLocalAliases());
        return identifierAnalyzers;
    }

    @Override
    public List<IIdentifierAnalyzer> getExtendableIdentifiers(){
        List<IIdentifierAnalyzer> identifierAnalyzers = new ArrayList<>();
        identifierAnalyzers.addAll(getExtendableNodeIdentifiers());
        identifierAnalyzers.addAll(getExtendableRelationIdentifiers());
        identifierAnalyzers.addAll(getExtendableAliases());
        return identifierAnalyzers;
    }

    @Override
    public IIdentifierAnalyzer getIdentifierAnalyzer(String name){
        List<IIdentifierAnalyzer> identifierAnalyzers = getAvailableIdentifiers();
        for(IIdentifierAnalyzer identifierAnalyzer: identifierAnalyzers){
            if(identifierAnalyzer.getName().equals(name)){
                return identifierAnalyzer;
            }
        }
        return null;
    }

    @Override
    public IIdentifierAnalyzer getIdentifierAnalyzer(IIdentifier identifier){
        return getIdentifierAnalyzer(identifier.getName());
    }



}
