package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.schema.IFunctionInfo;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.neo4j.schema.Neo4jSchema;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class CallExpression extends CypherExpression {
    public String functionName;
    private String functionSignature;
    public List<IExpression> params;
    public List<List<Object>> aggregationParams;
    private List<Object> elementValue;
    public Object showValue;

    public CallExpression(IFunctionInfo functionInfo, List<IExpression> params){
        this.functionName = functionInfo.getName();
        this.functionSignature = functionInfo.getSignature();
        this.params = new ArrayList<>();
        for(IExpression e : params)
        {
            this.params.add(e.getCopy());
        }
        params.forEach(e->e.setParentExpression(this));
    }

    public <R> CallExpression(String functionName, String functionSignature, List<IExpression> params, List<List<Object>> aggregationParams, List<Object> elementValue, Object showValue) {
        this.functionName = functionName;
        this.functionSignature = functionSignature;
        this.params = new ArrayList<>();
        for (IExpression e : params) {
            this.params.add(e.getCopy());
        }
        if (aggregationParams != null) {
            this.aggregationParams = new ArrayList<>();
            for (List<Object> l : aggregationParams) {
                List<Object> current = new ArrayList<>();
                for (Object o : l) {
                    current.add(o);
                }
                this.aggregationParams.add(current);
            }
        }
        if (elementValue != null) {
            this.elementValue = new ArrayList<>();
            for (Object o : elementValue) {
                this.elementValue.add(o);
            }
        }
        this.showValue = showValue;
        params.forEach(e -> e.setParentExpression(this));
    }

    public void setElementValue(List<Object> elementValue1) {
        if (showValue != null && !elementValue1.contains(showValue))
            showValue = elementValue1.get((int) Randomly.getNotCachedInteger(0, elementValue1.size() - 1));
        this.elementValue = new ArrayList<>();
        this.elementValue.addAll(elementValue1);
    }
    public List<Object> getElementValue() {
        return elementValue;
    }

    Object getValueFromIExpression(IExpression e)
    {
        try{
            if(e instanceof ConstExpression)
            {
                if(((ConstExpression)e).getType() == CypherType.NUMBER)
                    return Long.parseLong(((ConstExpression) e).getValue().toString());
                else if(((ConstExpression)e).getType() == CypherType.STRING)
                    return ((ConstExpression) e).getValue().toString();
                else if (((ConstExpression)e).getType() == CypherType.BOOLEAN)
                    return ((ConstExpression) e).getValue();
            }
            else if(e instanceof GetPropertyExpression)
            {
                if(((GetPropertyExpression) e).getValue() instanceof Long)
                    return Long.parseLong(((GetPropertyExpression) e).getValue().toString());
                else if(((GetPropertyExpression) e).getValue() instanceof String)
                    return ((GetPropertyExpression) e).getValue();
                else if (((GetPropertyExpression) e).getValue() instanceof Boolean)
                    return ((GetPropertyExpression) e).getValue();
                else
                    throw new Exception("GetPropertyExpression value is neither a Long nor a String, but a " + ((GetPropertyExpression) e).getValue().getClass().getName());
            }
            else if (e instanceof BinaryNumberExpression)
                return ((BinaryNumberExpression) e).getValue();
            else if (e instanceof IdentifierExpression && ((IdentifierExpression) e).getIdentifier() instanceof Alias) {
                return ((Alias) (((IdentifierExpression) e).getIdentifier())).getValue();
            }
            else
            {
                throw new Exception("Sub-expression in a CallExpression is neither a ConstExpression nor a GetPropertyExpression, but a " + e.getClass().getName());
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            return 0;
        }
        return 0;
    }

    public CallExpression(String functionName, String functionSignature, List<IExpression> params){
        this.functionName = functionName;
        this.functionSignature = functionSignature;
        this.params = params;
        params.forEach(e->e.setParentExpression(this));
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        if(MainOptions.mode == "kuzu")
            functionName = functionName.toLowerCase();
        if(MainOptions.mode == "kuzu" && (functionName.equals("toupper")))
            functionName = "upper";
        if(MainOptions.mode == "kuzu" && (functionName.equals("tolower")))
            functionName = "lower";
        if(MainOptions.mode == "kuzu" && (functionName.equals("endswith")))
            functionName = "ends_with";
        if(MainOptions.mode == "kuzu" && (functionName.equals("startswith")))
            functionName = "starts_with";
        if(MainOptions.isFloatFunction(functionName))
        {
            if(MainOptions.mode == "falkordb")
                sb.append("toInteger(abs("+functionName+"(");
            else if (MainOptions.mode == "kuzu")
                sb.append("CAST(floor(abs("+functionName+"(");
            else
                sb.append("toInteger("+functionName+"(");
        }
        else
            sb.append(functionName).append("(");
        if(params.size()>0) {
            params.forEach(e -> {
                e.toTextRepresentation(sb);
                sb.append(", ");
            });
            sb.delete(sb.length() - 2, sb.length());
        }
        if(MainOptions.isFloatFunction(functionName))
        {
            if(MainOptions.mode == "falkordb")
                sb.append(")))");
            else if (MainOptions.mode == "kuzu")
                sb.append("))), \"INT64\")");
            else
                sb.append("))");
        }
        else
            sb.append(")");
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        toTextRepresentation(sb);
        return sb.toString();
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema schema, List<IIdentifierAnalyzer> identifiers) {
        IFunctionInfo functionInfo = schema.getFunctions().stream().filter(f->f.getSignature().equals(functionSignature)).findAny().orElse(null);
        if(functionInfo!=null){
            return functionInfo.calculateReturnType(params);
        }
        return new CypherTypeDescriptor(CypherType.UNKNOWN);
    }

    @Override
    public IExpression getCopy() {
        if(aggregationParams == null && elementValue == null && showValue == null && params!=null){
            return new CallExpression(this.functionName, this.functionSignature, params);
        }
        return new CallExpression(this.functionName, this.functionSignature,
                this.params.stream().map(p->p.getCopy()).collect(Collectors.toList()), new ArrayList<>(this.aggregationParams), new ArrayList<>(this.elementValue), this.showValue);
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof CallExpression)){
            return false;
        }
        if(((CallExpression) o).params == null){
            ((CallExpression) o).params = new ArrayList<>();
        }
        if(params == null){
            params = new ArrayList<>();
        }
        if(params.size() != ((CallExpression) o).params.size()){
            return false;
        }
        return ((CallExpression) o).params.containsAll(params);
    }

    @Override
    public void replaceChild(IExpression originalExpression, IExpression newExpression) {
        for(int i = 0; i < params.size(); i++){
            if(originalExpression == params.get(i)){
                params.set(i, newExpression);
                newExpression.setParentExpression(this);
                return;
            }
        }

        throw new RuntimeException();
    }

    @Override
    public Object getValue(Map<String, Object> varToProperties) {
        return getValue();
    }

    public Object calculateValue() {
        if (elementValue != null && elementValue.size() > 0) {
            return elementValue;
        }
        List<Object> result = new ArrayList<>();
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.MAX_NUMBER.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("MAX_NUMBER function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("MAX_NUMBER function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                long currentMax = (long) current.get(0);
                for (int j = 0; j < current.size(); j++) {
                    if ((long) current.get(j) > currentMax) {
                        currentMax = (long) current.get(j);
                    }
                }
                result.add(currentMax);
            }

        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.MAX_STRING.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("MAX_STRING function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("MAX_STRING function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                String currentMax = (String) current.get(0);
                for (int j = 0; j < current.size(); j++) {
                    if (((String) current.get(j)).compareTo(currentMax) > 0) {
                        currentMax = (String) current.get(j);
                    }
                }
                result.add(currentMax);
            }
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.MIN_STRING.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("MIN_STRING function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("MIN_STRING function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                String currentMax = (String) current.get(0);
                for (int j = 0; j < current.size(); j++) {
                    if (((String) current.get(j)).compareTo(currentMax) < 0) {
                        currentMax = (String) current.get(j);
                    }
                }
                result.add(currentMax);
            }
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.MIN_NUMBER.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("MIN_NUMBER function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("MIN_NUMBER function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                long currentMin = (long) current.get(0);
                for (int j = 0; j < current.size(); j++) {
                    if ((long) current.get(j) < currentMin) {
                        currentMin = (long) current.get(j);
                    }
                }
                result.add(currentMin);
            }

        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.AVG.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("AVG function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("AVG function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                long currentSum = 0;
                for (int j = 0; j < current.size(); j++) {
                    currentSum = Math.addExact(currentSum, (long) current.get(j));
                }
                if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                    result.add((long) (abs(currentSum / current.size())));
                else
                    result.add((long) (currentSum / current.size()));
            }
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SUM.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("SUM function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("SUM function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                long currentSum = 0;
                for (int j = 0; j < current.size(); j++) {
                    currentSum = Math.addExact(currentSum, (long) current.get(j));
                }
                result.add(currentSum);
            }
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.COLLECT.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("COLLECT function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("AVG function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                long currentSum = 0;
                for (int j = 0; j < current.size(); j++) {
                    result.add(current.get(j));
                }
            }
            return result;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.COUNT.getSignature())) {
            if (params.size() != 1)
                throw new RuntimeException("COLLECT function should have only one parameter");
            if (aggregationParams == null || aggregationParams.size() == 0) {
                throw new RuntimeException("AVG function should have aggregationParams");
            }
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                result.add((long) current.size());
            }
        }

        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ST_DEV.getSignature())) {
            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                double tmpavg = 0.0;
                for (Object e : current) {
                    tmpavg += Double.valueOf((Long) e);
                }
                tmpavg /= current.size();
                double tmpsum = 0.0;
                for (Object e : current) {
                    tmpsum += Math.pow((Long) e - tmpavg, 2);
                }
                tmpsum /= (current.size() - 1);
                if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                    result.add((long) (abs(Math.sqrt(tmpsum))));
                else
                    result.add((long) Math.sqrt(tmpsum));
            }
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ST_DEV_P.getSignature())) {

            for (int i = 0; i < aggregationParams.size(); i++) {
                List<Object> current = aggregationParams.get(i);
                double tmpavg = 0.0;
                for (Object e : current) {
                    tmpavg += Double.valueOf((Long) e);
                }
                tmpavg /= current.size();
                double tmpsum = 0.0;
                for (Object e : current) {
                    tmpsum += Math.pow((Long) e - tmpavg, 2);
                }
                tmpsum /= (current.size());
                if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                    result.add((long) (abs(Math.sqrt(tmpsum))));
                else
                    result.add((long) Math.sqrt(tmpsum));
            }
        }
        return result;
    }
    public Object getValue() {
        StringBuilder res = new StringBuilder();
        Object temp = 0;
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.COLLECT.getSignature())) {
            return calculateValue();
        }
        if (MainOptions.isAggregateFunction(functionName)) {
            if (showValue != null)
                return showValue;
            else {
                if (elementValue != null) {
                    showValue = elementValue.get((int) Randomly.getNotCachedInteger(0, elementValue.size() - 1));
                } else {
                    List<Object> value = (List<Object>) calculateValue();
                    this.elementValue = value;
                    showValue = value.get((int) Randomly.getNotCachedInteger(0, value.size() - 1));
                }
                return showValue;
            }
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.DEGREE.getSignature())) {
            assert (params.size() == 1);
            IdentifierExpression nodeexp = (IdentifierExpression) params.get(0);
            if (nodeexp.getIdentifier() instanceof NodeAnalyzer)
                return ((NodeIdentifier) ((NodeAnalyzer) nodeexp.getIdentifier()).getSource()).actualNode.getRelationships().size();
            else
                return ((NodeIdentifier) nodeexp.getIdentifier()).getActualNode().getRelationships().size();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.OUT_DEGREE.getSignature())) {
            assert (params.size() == 1);
            IdentifierExpression nodeexp = (IdentifierExpression) params.get(0);
            long cnt = 0;
            AbstractNode currentNode;
            if (nodeexp.getIdentifier() instanceof NodeAnalyzer)
                currentNode = ((NodeIdentifier) ((NodeAnalyzer) nodeexp.getIdentifier()).getSource()).actualNode;
            else
                currentNode = ((NodeIdentifier) nodeexp.getIdentifier()).getActualNode();
            for (AbstractRelationship relationship : currentNode.getRelationships()) {
                if (relationship.getFrom().equals(((NodeIdentifier) nodeexp.getIdentifier()).getActualNode())) {
                    cnt++;
                }
            }
            return cnt;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.IN_DEGREE.getSignature())) {
            assert (params.size() == 1);
            IdentifierExpression nodeexp = (IdentifierExpression) params.get(0);
            long cnt = 0;
            AbstractNode currentNode;
            if (nodeexp.getIdentifier() instanceof NodeAnalyzer)
                currentNode = ((NodeIdentifier) ((NodeAnalyzer) nodeexp.getIdentifier()).getSource()).actualNode;
            else
                currentNode = ((NodeIdentifier) nodeexp.getIdentifier()).getActualNode();
            for (AbstractRelationship relationship : currentNode.getRelationships()) {
                if (relationship.getTo().equals(((NodeIdentifier) nodeexp.getIdentifier()).getActualNode())) {
                    cnt++;
                }
            }
            return cnt;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.END_NODE.getSignature())) {
            assert (params.size() == 1);
            IdentifierExpression relationexp = (IdentifierExpression) params.get(0);
            if (relationexp.getIdentifier() instanceof RelationAnalyzer)
                return ((RelationIdentifier) (((RelationAnalyzer) relationexp.getIdentifier()).getSource())).actualRelationship.getTo();
            else
                return ((RelationIdentifier) relationexp.getIdentifier()).actualRelationship.getTo();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.HEAD.getSignature())) {
            assert (params.size() == 1);
            CreateListExpression list = (CreateListExpression) params.get(0);
            return list.getValue().get(0);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.LAST.getSignature())) {
            assert (params.size() == 1);
            CreateListExpression list = (CreateListExpression) params.get(0);
            return list.getValue().get((int) (list.getListSize() - 1));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.START_NODE.getSignature())) {
            assert (params.size() == 1);
            IdentifierExpression relationexp = (IdentifierExpression) params.get(0);
            return ((AbstractRelationship) relationexp.getIdentifier()).getFrom();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOBOOLEAN_INTEGER.getSignature())) {
            assert (params.size() == 1);
            long inside = (long) getValueFromIExpression(params.get(0));
            if (inside == 0)
                return false;
            else
                return true;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOBOOLEAN_STRING.getSignature())) {
            assert (params.size() == 1);
            String content = (String) getValueFromIExpression(params.get(0));
            if (content == "t" || content == "true")
                return true;
            else if (content == "f" || content == "false")
                return false;
            else
                return null;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOINTEGER_BOOLEAN.getSignature())) {
            assert (params.size() == 1);
            Boolean inside = (Boolean) getValueFromIExpression(params.get(0));
            if (inside)
                return 1L;
            else
                return 0L;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOINTEGER_INTEGER.getSignature())) {
            assert (params.size() == 1);
            return (Long) getValueFromIExpression(params.get(0));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOINTEGER_STRING.getSignature())) {
            assert (params.size() == 1);
            String inside = (String) getValueFromIExpression(params.get(0));
            try {
                return Long.parseLong(inside);
            } catch (Exception ex) {
                return null;
            }
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOSTRING_BOOLEAN.getSignature())) {
            assert (params.size() == 1);
            Boolean inside = (Boolean) getValueFromIExpression(params.get(0));
            if (inside)
                return "true";
            else
                return "false";
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOSTRING_INTEGER.getSignature())) {
            assert (params.size() == 1);
            return getValueFromIExpression(params.get(0)).toString();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOSTRING_STRING.getSignature())) {
            assert (params.size() == 1);
            return getValueFromIExpression(params.get(0)).toString();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TYPE.getSignature())) {
            assert (params.size() == 1);
            return ((RelationIdentifier) (((IdentifierExpression) params.get(0)).getIdentifier())).actualRelationship.getType().getName();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.LABELS.getSignature())) {
            assert (params.size() == 1);
            return ((NodeIdentifier) (((IdentifierExpression) params.get(0)).getIdentifier())).actualNode.getLabels().stream().map(l -> l.getName()).collect(Collectors.toList());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.RANGE.getSignature())) {
            assert (params.size() == 3);
            long start = (long) getValueFromIExpression(params.get(0));
            long end = (long) getValueFromIExpression(params.get(1));
            long step = (long) getValueFromIExpression(params.get(2));
            List<Long> result = new ArrayList<>();
            if (start > end && step > 0)
                return result;
            if (start < end && step < 0)
                return result;
            for (int i = (int) start; i < end; i += step) {
                result.add((long) i);
            }
            return result;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ABS.getSignature())) {
            assert (params.size() == 1);
            return ((long) (abs(((Long) getValueFromIExpression(params.get(0))).doubleValue())));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ACOS.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(((Long) getValueFromIExpression(params.get(0))).doubleValue());
            else
                return (long) Math.acos(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ASIN.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.asin(((Long) getValueFromIExpression(params.get(0))).doubleValue()));
            return (long) Math.asin(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ATAN.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.atan(((Long) getValueFromIExpression(params.get(0))).doubleValue()));
            return (long) Math.atan(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ATAN2.getSignature())) {
            assert (params.size() == 2);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.atan2(((Long) getValueFromIExpression(params.get(0))).doubleValue(), ((Long) getValueFromIExpression(params.get(1))).doubleValue()));
            return (long) Math.atan2(((Long) getValueFromIExpression(params.get(0))).doubleValue(), ((Long) getValueFromIExpression(params.get(1))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.CEIL.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.ceil((long) getValueFromIExpression(params.get(0))));
            return (long) Math.ceil(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.COS.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.cos((long) getValueFromIExpression(params.get(0))));
            return (long) Math.cos(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.E.getSignature())) {
            assert (params.size() == 0);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.E);
            return (long) Math.E;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.EXP.getSignature())) {
            assert (params.size() == 1);
            if ((long) getValueFromIExpression(params.get(0)) > 40)
                throw new ArithmeticException("Overflow");
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.exp((long) getValueFromIExpression(params.get(0))));
            return (long) Math.exp(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.FLOOR.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.floor((long) getValueFromIExpression(params.get(0))));
            return (long) Math.floor(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.LOG.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb")
                return (long) abs(Math.log((long) getValueFromIExpression(params.get(0))));
            if (MainOptions.mode == "kuzu")
                return (long) abs(Math.log10(((long) getValueFromIExpression(params.get(0)))));
            return (long) Math.log(((Long) getValueFromIExpression(params.get(0))).doubleValue());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.LOG10.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.log10((long) getValueFromIExpression(params.get(0))));
            return (long) Math.log10((long) getValueFromIExpression(params.get(0)));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.PI.getSignature())) {
            assert (params.size() == 0);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.PI);
            return (long) Math.PI;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ROUND.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.round(((Long) getValueFromIExpression(params.get(0))).doubleValue()));
            return ((long) getValueFromIExpression(params.get(0)));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SIGN.getSignature())) {
            assert (params.size() == 1);
            long current = (long) getValueFromIExpression(params.get(0));
            if (current > 0)
                return 1L;
            else if (current < 0)
                return -1L;
            else
                return 0L;
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SIN.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.sin((long) getValueFromIExpression(params.get(0))));
            return (long) Math.sin((long) getValueFromIExpression(params.get(0)));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SQRT.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.sqrt((long) getValueFromIExpression(params.get(0))));
            return (long) Math.sqrt((long) getValueFromIExpression(params.get(0)));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TAN.getSignature())) {
            assert (params.size() == 1);
            if (MainOptions.mode == "falkordb" || MainOptions.mode == "kuzu")
                return (long) abs(Math.tan((long) getValueFromIExpression(params.get(0))));
            return (long) Math.tan((long) getValueFromIExpression(params.get(0)));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.CONTAINS.getSignature())) {
            assert (params.size() == 2);
            String content = (String) getValueFromIExpression(params.get(0));
            String sub = (String) getValueFromIExpression(params.get(1));
            return content.contains(sub);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.ENDSWITH.getSignature())) {
            assert (params.size() == 2);
            String content = (String) getValueFromIExpression(params.get(0));
            String sub = (String) getValueFromIExpression(params.get(1));
            return content.endsWith(sub);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.LEFT.getSignature())) {
            assert (params.size() == 2);
            String content = (String) getValueFromIExpression(params.get(0));
            long len = (long) getValueFromIExpression(params.get(1));
            if (len > content.length())
                return content;
            else
                return content.substring(0, (int) len);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.LTRIM.getSignature())) {
            assert (params.size() == 1);
            long i = 0;
            String str = (String) getValueFromIExpression(params.get(0));
            while (i < str.length() && Character.isWhitespace(str.charAt((int) i))) {
                i++;
            }
            return str.substring((int) i);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.REPLACE.getSignature())) {
            assert (params.size() == 3);
            String content = (String) getValueFromIExpression(params.get(0));
            String sub = (String) getValueFromIExpression(params.get(1));
            String replace = (String) getValueFromIExpression(params.get(2));
            return content.replace(sub, replace);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.RIGHT.getSignature())) {
            assert (params.size() == 2);
            String content = (String) getValueFromIExpression(params.get(0));
            long len = (long) getValueFromIExpression(params.get(1));
            if (len > content.length())
                return content;
            else
                return content.substring((int) (content.length() - len));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.RTRIM.getSignature())) {
            assert (params.size() == 1);
            long i = ((String) getValueFromIExpression(params.get(0))).length() - 1;
            String str = (String) getValueFromIExpression(params.get(0));
            while (i >= 0 && Character.isWhitespace(str.charAt((int) i))) {
                i--;
            }
            return str.substring(0, (int) (i + 1));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.STARTSWITH.getSignature())) {
            assert (params.size() == 2);
            String content = (String) getValueFromIExpression(params.get(0));
            String sub = (String) getValueFromIExpression(params.get(1));
            return content.startsWith(sub);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SUBSTRING.getSignature())) {
            assert (params.size() == 3);
            String content = (String) getValueFromIExpression(params.get(0));
            long start = (long) getValueFromIExpression(params.get(1));
            if (MainOptions.mode == "kuzu")
                start -= 1;
            long end = (long) getValueFromIExpression(params.get(2));
            if (start > content.length())
                return "";
            else if (end > content.length())
                return content.substring((int) start);
            else if (start + end > content.length())
                return content.substring((int) start);
            else
                return content.substring((int) start, (int) end + (int) start);
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOLOWER.getSignature())) {
            assert (params.size() == 1);
            return ((String) getValueFromIExpression(params.get(0))).toLowerCase();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TOUPPER.getSignature())) {
            assert (params.size() == 1);
            return ((String) getValueFromIExpression(params.get(0))).toUpperCase();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TRIM.getSignature())) {
            assert (params.size() == 1);
            return ((String) getValueFromIExpression(params.get(0))).trim();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.TAIL.getSignature())) {
            assert (params.size() == 1);
            List<Object> content = (List) ((CypherExpression) (params.get(0))).getValue();
            return content.subList(1, content.size());
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SPLIT.getSignature())) {
            assert (params.size() == 2);
            String content = (String) getValueFromIExpression(params.get(0));
            String sub = (String) getValueFromIExpression(params.get(1));
            return Arrays.asList(content.split(sub));
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.REVERSE.getSignature())) {
            assert (params.size() == 1);
            String content = (String) ((CypherExpression) (params.get(0))).getValue();
            StringBuilder tmp = new StringBuilder();
            for (int i = content.length() - 1; i >= 0; i--) {
                tmp.append(content.charAt(i));
            }
            return tmp.toString();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SIZE_LIST.getSignature())) {
            assert (params.size() == 1);
            List<Object> list = (List) ((CypherExpression) params.get(0)).getValue();
            assert (list.size() == 1);
            return (long) ((List) (list)).size();
        }
        if (Objects.equals(this.functionSignature, Neo4jSchema.Neo4jBuiltInFunctions.SIZE_STRING.getSignature())) {
            assert (params.size() == 1);
            return (long) ((String) getValueFromIExpression(params.get(0))).length();
        }


        try {
            throw new Exception("Unsupported function name: " + this.functionSignature + " in CallExpression" + this.toString());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return res.toString();
    }

    public Set<IIdentifier> reliedContent()
    {
        Set<IIdentifier> result = new HashSet<>();
        for(IExpression e : params)
        {
            if(e instanceof GetPropertyExpression)
            {
                result.addAll(((GetPropertyExpression)e).reliedContent());
            }
            else if(e instanceof IdentifierExpression)
            {
                result.addAll(((IdentifierExpression)e).reliedContent());
            }
            else if(e instanceof ConstExpression)
            {}
            else if (e instanceof BinaryNumberExpression)
            {
                result.addAll(((BinaryNumberExpression)e).reliedContent());
            }
            else
            {
                throw new RuntimeException("Unknown type of expression in CallExpression");
            }
        }
        return result;
    }

    @Override
    public void removeElement(Set<IIdentifier> toRemove) {
        boolean remove = false;
        for(IExpression e : params)
        {
            CypherExpression temp = (CypherExpression)e;
            Set<IIdentifier> tempReliedContent = temp.reliedContent();
            for(IIdentifier identifier : toRemove){
                if(tempReliedContent.contains(identifier)){
                    remove = true;
                    break;
                }
            }
        }
        if(remove)
        {
            Object value = getValue();
            this.params = new ArrayList<>();
            this.params.add(new ConstExpression(value));
        }
    }
}
