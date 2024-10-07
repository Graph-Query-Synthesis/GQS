package org.example.gqs.cypher.standard_ast;

import org.example.gqs.MainOptions;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IReturnAnalyzer;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.*;
import java.util.stream.Collectors;

public class Return extends CypherClause implements IReturnAnalyzer {

    private IExpression skip = null, limit = null;
    public List<IExpression> orderBy = new ArrayList<>();
    List<Boolean> isOrderByDesc = new ArrayList<>();
    private boolean distinct = false;
    public List<Map<String, String>> correctReturn = new ArrayList<>();
    public long repeatTimes = 1;
    public boolean usedAggregationFunc = false;
    public List<Map<String, Object>> expandedNamespace;

    public Return(){
        super(false);
    }


    @Override
    public List<IRet> getReturnList() {
        return symtab.getAliasDefinitions();
    }

    @Override
    public void setReturnList(List<IRet> returnList) {
        symtab.setAliasDefinition(returnList);
    }

    public List<Map<String, Object>> getNamespace(List<Map<String, Object>> expandedNamespace) {

        expandedNamespace = fromExpandNamespace(expandedNamespace, orderBy, isOrderByDesc, limit, skip, distinct, getReturnList(), this);
        this.expandedNamespace = expandedNamespace;
        return expandedNamespace;
    }

    public Map<String, Object> getNamespace(Map<String, Object> originalNamespace)
    {
        Map<String, Object> namespace = new HashMap<>();

        this.expandedNamespace = new ArrayList<>();
        for (IRet ret : getReturnList()) {
            if (ret.getIdentifier() instanceof Alias) {
                originalNamespace.put(ret.getIdentifier().getName(), ((Alias) ret.getIdentifier()));
            }
            else if (ret.getIdentifier() instanceof IIdentifier) {
                if(ret.getIdentifier() instanceof NodeIdentifier)
                {
                    originalNamespace.put(ret.getIdentifier().getName(), ((NodeIdentifier) ret.getIdentifier()).actualNode);
                }
                else if(ret.getIdentifier() instanceof RelationIdentifier)
                {
                    originalNamespace.put(ret.getIdentifier().getName(), ((RelationIdentifier) ret.getIdentifier()).actualRelationship);
                }
                else
                {
                    throw new RuntimeException("In RETURN getNamespace: Identifier is not a valid identifier, but a "+ret.getIdentifier().getClass().getName());
                }
            }

            else {
                throw new RuntimeException("In RETURN getNamespace, found something that is not alias or identifier, but a "+ret.getIdentifier().getClass().getName());
            }
        }
        repeatTimes = this.expandNamespace(originalNamespace, orderBy, isOrderByDesc, limit, skip, distinct, getReturnList(), expandedNamespace, this);



        for(String key: originalNamespace.keySet())
        {
            for(int i = 0; i < expandedNamespace.size(); i++)
            {
                Map<String, Object> currentNamespace = expandedNamespace.get(i);
                if(!currentNamespace.containsKey(key))
                {
                    originalNamespace.remove(key);
                }
            }
        }
        for(String key : originalNamespace.keySet())
        {
            Object value = originalNamespace.get(key);
            if (value instanceof Alias && ((Alias)value).getExpression() instanceof CreateListExpression){
                List<Object> elementValue = new ArrayList<>();
                for(int i = 0; i < expandedNamespace.size(); i++)
                {
                    Map<String, Object> currentNamespace = expandedNamespace.get(i);
                    Object currentData = getFromNamespace(new IdentifierExpression((Alias)value), currentNamespace);
                    elementValue.add(currentData);
                }
                ((CreateListExpression)((Alias)value).getExpression()).elementValue = elementValue;
            }
        }

        for (IRet ret : getReturnList()) {
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





        return namespace;
    }

    public void updateProvideAndRequire(){
        provide = new HashSet<>();
        require = new HashSet<>();
        List<IRet> rets = getReturnList();
        for(IRet ret : rets){
            if(ret.isAlias())
            {
                require.add((Alias)ret.getIdentifier());
            }
            if(ret.getExpression() != null)
            {
                require.addAll(((CypherExpression)ret.getExpression()).reliedContent());
            }
        }
        for(IExpression expression : orderBy){
            require.addAll(((CypherExpression)expression).reliedContent());
        }
    }




    public void setCorrectResult(List<IRet> returnList)
    {


        correctReturn.add(new HashMap<>());
        List<Integer> unwindMark = new ArrayList<>();
        for(int i = 0; i < returnList.size(); i++){
            IRet ret = returnList.get(i);

            try{
                IExpression expression = ret.getExpression();
                if(expression instanceof CreateListExpression)
                {

                    unwindMark.add(i);
                }
                else if(expression instanceof GetPropertyExpression){
                    correctReturn.get(0).put(ret.getIdentifier().getName(), ((GetPropertyExpression)expression).getValue().toString());
                }
                else if(expression instanceof ConstExpression){
                    correctReturn.get(0).put(ret.getIdentifier().getName(), ((ConstExpression)expression).getValue().toString());
                }
                else if(expression instanceof CallExpression)
                {
                    usedAggregationFunc = true;
                    correctReturn.get(0).put(ret.getIdentifier().getName(), ((CallExpression)expression).getValue().toString());
                }
                else{
                    throw new Exception("Return statement value parsing error, possibly something haven't seen" + expression.getClass().getName());
                }
            }
            catch(Exception e){
                System.out.println("Return statement value parsing error, possibly something haven't seen" + ret.getExpression().getClass().getName());
                e.printStackTrace();
            }
        }





        Map<String, String> currentCorrectReturn = correctReturn.get(0);
        for(int i = 0; i < repeatTimes - 1; i++)
        {
            correctReturn.add(new HashMap<String, String>(currentCorrectReturn));
        }
        List<List<Object>> toPermutate = new ArrayList<>();
        for(int i = 0; i < unwindMark.size(); i++) {
            IRet ret = returnList.get(unwindMark.get(i));
            IExpression expression = ret.getExpression();
            CreateListExpression subExpression = (CreateListExpression) (((Alias) ret.getIdentifier()).getExpression());
            List<Object> list = subExpression.getValue();
            if (ret.getIdentifier() instanceof Alias && ((Alias) ret.getIdentifier()).getExpression() instanceof CreateListExpression && ((Alias) ret.getIdentifier()).isDistinct) {

                list = new ArrayList<>(new HashSet<>(list));
            }
            toPermutate.add(list);
        }
        List<List<Object>> permutateResult = new ArrayList<>();
        permutateResult = toPermutate(permutateResult, toPermutate, 0, new ArrayList<>());
        long cnt = 0;
        for(int j = 0; j < repeatTimes; j++)
        {
            for(int i = 0; i < unwindMark.size(); i++) {
                IRet ret = returnList.get(unwindMark.get(i));
                correctReturn.get(j).put(ret.getIdentifier().getName(), permutateResult.get((int)cnt).get(i).toString());
            }
            cnt++;
            if(cnt == permutateResult.size())
            {
                cnt = 0;
            }
        }












        if(isDistinct() || usedAggregationFunc)
        {
            Set<Map<String, String>> uniqueSet = new HashSet<>(correctReturn);
            correctReturn = new ArrayList<>(uniqueSet);
        }
    }

    private List<List<Object>> toPermutate(List<List<Object>> permutateResult, List<List<Object>> toPermutate, long i, ArrayList<Object> objects) {
        if(i == toPermutate.size())
        {
            permutateResult.add(new ArrayList<>(objects));
            return permutateResult;
        }
        for(int j = 0; j < toPermutate.get((int)i).size(); j++)
        {
            objects.add(toPermutate.get((int)i).get(j));
            toPermutate(permutateResult, toPermutate, i+1, objects);
            objects.remove(objects.size()-1);
        }
        return permutateResult;
    }

    @Override
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public void setOrderBy(List<IExpression> expression, boolean isDesc) {
        orderBy = expression;
        isOrderByDesc = new ArrayList<>();
        isOrderByDesc.add(isDesc);
    }


    @Override
    public List<IExpression> getOrderByExpressions() {
        return orderBy;
    }

    @Override
    public List<Boolean> isOrderByDesc() {
        return isOrderByDesc;
    }

    public void setOrderBy(List<IExpression> expressions, List<Boolean> isDesc) {
        orderBy = expressions;
        isOrderByDesc = isDesc;
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
    public IReturnAnalyzer toAnalyzer() {
        return this;
    }

    @Override
    public ICypherClause getCopy() {
        Return returnClause = new Return();
        if(symtab != null){
            if(symtab != null){
                returnClause.symtab.setPatterns(symtab.getPatterns().stream().map(p->p.getCopy()).collect(Collectors.toList()));
                returnClause.symtab.setAliasDefinition(symtab.getAliasDefinitions().stream().map(a->a.getCopy()).collect(Collectors.toList()));
            }
        }
        if(skip != null){
            returnClause.skip = skip.getCopy();
        }
        if(limit != null){
            returnClause.limit = limit.getCopy();
        }
        returnClause.orderBy = new ArrayList<>(orderBy.stream().map(e->e.getCopy()).collect(Collectors.toList()));
        returnClause.isOrderByDesc = this.isOrderByDesc;
        returnClause.distinct = this.distinct;
        if(provide != null){
            returnClause.provide = new HashSet<>();
            for(IIdentifier identifier: provide){
                returnClause.provide.add(identifier.getCopy());
            }
        }
        if(require != null){
            returnClause.require = new HashSet<>();
            for(IIdentifier identifier: require){
                returnClause.require.add(identifier.getCopy());
            }
        }
        return returnClause;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("RETURN ");
        if(distinct){
            sb.append("DISTINCT ");
        }
        List<IRet> returnList = getReturnList();
        for(int i = 0; i < returnList.size(); i++){
            returnList.get(i).toTextRepresentation(sb);
            if(i != returnList.size()-1){
                sb.append(", ");
            }
        }
        if(orderBy != null && orderBy.size() != 0){
            sb.append(" ORDER BY ");
            for(int i = 0; i < orderBy.size(); i++){
                orderBy.get(i).toTextRepresentation(sb);
                if(isOrderByDesc.get(i)){
                    sb.append(" DESC");
                }
                if(i != orderBy.size()-1){
                    sb.append(", ");
                }
            }
        }
        if(skip != null){
            sb.append(" SKIP ");
            skip.toTextRepresentation(sb);
        }
        if(limit != null){
            sb.append(" LIMIT ");
            limit.toTextRepresentation(sb);
        }
    }

    @Override
    public List<IPattern> getLocalPatternContainsIdentifier(IIdentifier identifier) {
        return new ArrayList<>();
    }

    public List<GQSResultSet> getActualResult()
    {
        List<GQSResultSet> result = new ArrayList<GQSResultSet>();
        result.add(new GQSResultSet());
        for(int i = 0; i < correctReturn.size(); i++)
        {
            result.get(0).result.add(new HashMap<>());
            for(Map.Entry<String, String> entry : correctReturn.get(i).entrySet())
            {
                result.get(0).result.get(i).put(entry.getKey(), entry.getValue().toString());
            }
        }
        result.get(0).resultRowNum = correctReturn.size();
        return result;
    }

    @Override
    public IReturn getSource() {
        return this;
    }
}
