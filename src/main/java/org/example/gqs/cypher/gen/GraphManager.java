package org.example.gqs.cypher.gen;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.cypher.schema.IRelationTypeInfo;
import org.example.gqs.cypher.standard_ast.NodeIdentifier;
import org.example.gqs.cypher.standard_ast.RelationIdentifier;
import org.example.gqs.cypher.standard_ast.expr.*;
import org.example.gqs.graphfuzz.ConvertToMatrix;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.max;

public class GraphManager implements Cloneable {
    private List<AbstractNode> nodes = new ArrayList<>();
    private List<AbstractRelationship> relationships = new ArrayList<>();
    private ICypherSchema schema;
    private MainOptions options;
    private static long maxNodeColor = 3;

    private Map<IPropertyInfo, List<Object>> propertyValues = new HashMap<>();

    private long presentID = 0;


    private long maxNodeNumber = 128;

    private Randomly randomly = new Randomly();

    public ConvertToMatrix MatrixRep;

    public GraphManager(ICypherSchema schema, MainOptions options){
        this.schema = schema;
        this.options = options;
        this.maxNodeNumber = options.getMaxNodeNum();
        this.MatrixRep = new ConvertToMatrix((int) this.maxNodeNumber);
    }



    public static IExpression generateEquationForSingleElement(Object element, String name){
        Map<String, Object> properties = null;
        IExpression elementExp = null;
        if(element instanceof AbstractNode){
            AbstractNode node = (AbstractNode) element;
            properties = node.getProperties();
            elementExp = new IdentifierExpression(new NodeIdentifier(name, node));
        }
        else if(element instanceof AbstractRelationship){
            AbstractRelationship relation = (AbstractRelationship) element;
            properties = relation.getProperties();
            elementExp = new IdentifierExpression(new RelationIdentifier(name, relation));
        }
        else{
            throw new RuntimeException();
        }
        List<String> keyList = new ArrayList<>(properties.keySet());
        Randomly randomly = new Randomly();
        String randomKey = keyList.get((int)randomly.getInteger(0, keyList.size()));
        int cnt = 0;
        while(properties.get(randomKey) instanceof Boolean)
        {
            for(String key : properties.keySet())
            {
                if(properties.get(key) instanceof Boolean)
                    cnt++;
                else
                    randomKey = key;
            }
        }
        IExpression roundExp = new ConstExpression(true);

        if(properties.get(randomKey) instanceof Integer || properties.get(randomKey) instanceof Long)
        {
            long randomOp = randomly.getInteger(0, 100);
            if(randomKey.equals("id"))
                randomOp = 1;
            if(randomOp < 30)
            {
                roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(properties.get(randomKey)), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);
            }
            else if(randomOp < 60)
            {
                long upperBound = (long)properties.get(randomKey)+randomly.getInteger(1, 3);
                long lowerBound = (long)properties.get(randomKey)-randomly.getInteger(1, 3);
                roundExp = new BinaryLogicalExpression(new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(lowerBound), BinaryComparisonExpression.BinaryComparisonOperation.HIGHER), new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(upperBound), BinaryComparisonExpression.BinaryComparisonOperation.SMALLER), BinaryLogicalExpression.BinaryLogicalOperation.AND);
            }
            else
            {
                long upperBound = (long)properties.get(randomKey)+randomly.getInteger(1, 3);
                long lowerBound = (long)properties.get(randomKey)-randomly.getInteger(1, 3);
                roundExp = new BinaryLogicalExpression(new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(lowerBound), BinaryComparisonExpression.BinaryComparisonOperation.HIGHER_OR_EQUAL), new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(upperBound), BinaryComparisonExpression.BinaryComparisonOperation.SMALLER_OR_EQUAL), BinaryLogicalExpression.BinaryLogicalOperation.AND);
            }
        }
        else if (properties.get(randomKey) instanceof String)
        {
            long randomOp = randomly.getInteger(0, 100);
            if(MainOptions.mode == "falkordb")
                randomOp = randomly.getInteger(0, 70);
            if(randomOp < 10)
            {
                roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(properties.get(randomKey)), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);
            }
            else if(randomOp < 30)
            {
                try {
                    if(((String) properties.get(randomKey)).length() < 5)
                        throw new IndexOutOfBoundsException();
                    String upperBound = (String) ((String) properties.get(randomKey)).substring(0, randomly.getInteger(((String) properties.get(randomKey)).length()-2, ((String) properties.get(randomKey)).length()));
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(upperBound), StringMatchingExpression.StringMatchingOperation.STARTS_WITH);
                }catch(IndexOutOfBoundsException e)
                {
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(properties.get(randomKey)), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);
                }
            }
            else if (randomOp < 50 && !MainOptions.mode.equals("kuzu"))
            {
                try {
                    if(((String) properties.get(randomKey)).length() < 5)
                        throw new IndexOutOfBoundsException();
                    String lowerBound = (String) ((String) properties.get(randomKey)).substring(randomly.getInteger(0,  ((String) properties.get(randomKey)).length() - 4), ((String) properties.get(randomKey)).length());
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(lowerBound), StringMatchingExpression.StringMatchingOperation.ENDS_WITH);
                } catch (IndexOutOfBoundsException e)
                {
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(properties.get(randomKey)), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);
                }
            }
            else if (randomOp < 70)
            {
                try {
                    long lowerIndex = randomly.getInteger(0, max(1, ((String) properties.get(randomKey)).length() - 4));
                    long upperIndex = randomly.getInteger(lowerIndex+4, ((String) properties.get(randomKey)).length());
                    String lowerBound = (String) ((String) properties.get(randomKey)).substring((int) lowerIndex, (int) upperIndex);
                    roundExp = new StringMatchingExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(lowerBound), StringMatchingExpression.StringMatchingOperation.CONTAINS);
                } catch (IndexOutOfBoundsException e)
                {
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(properties.get(randomKey)), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);
                }
            }
            else if (randomOp < 100)
            {
                try {
                    long randomChangeIndex = randomly.getInteger(((String) properties.get(randomKey)).length() - 2, ((String) properties.get(randomKey)).length());
                    String originalString = (String) properties.get(randomKey);
                    String lowerString = originalString.substring(0, (int) randomChangeIndex) + (char) (originalString.charAt((int)randomChangeIndex) - 1) + originalString.substring((int)randomChangeIndex + 1);

                    String upperString = originalString.substring(0, (int)randomChangeIndex) + (char) (originalString.charAt((int)randomChangeIndex) + 1) + originalString.substring((int)randomChangeIndex + 1);
                    roundExp = new BinaryLogicalExpression(new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(lowerString), BinaryComparisonExpression.BinaryComparisonOperation.HIGHER), new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(upperString), BinaryComparisonExpression.BinaryComparisonOperation.SMALLER), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                }
                catch (IndexOutOfBoundsException e)
                {
                    roundExp = new BinaryComparisonExpression(new GetPropertyExpression(elementExp, randomKey), new ConstExpression(properties.get(randomKey)), BinaryComparisonExpression.BinaryComparisonOperation.EQUAL);
                }
            }
        }
        else
        {
            throw new RuntimeException();
        }
        return roundExp;
    }

    String randomGetProperty(Object source)
    {
        Map<String, Object> properties = null;
        if(source instanceof AbstractNode)
        {
            properties = ((AbstractNode) source).getProperties();
        }
        else if(source instanceof AbstractRelationship)
        {
            properties = ((AbstractRelationship) source).getProperties();
        }
        else if (source instanceof HashMap<?,?>)
        {
            properties = (Map<String, Object>) source;
        }
        else
        {
            throw new RuntimeException();
        }
        List<String> keyList = new ArrayList<>(properties.keySet());
        String randomKey = keyList.get(randomly.getInteger(0, keyList.size()));
        while(properties.get(randomKey) instanceof Boolean)
        {
            randomKey = keyList.get(randomly.getInteger(0, keyList.size()));
        }
        return randomKey;
    }

    public List<CypherQueryAdapter> generateCreateEdgeQueries(AbstractRelationship relation){
        List<CypherQueryAdapter> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        AbstractNode from = relation.getFrom();
        AbstractNode to = relation.getTo();
        String template = "MATCH (startNode:Label1 PROPERTY1), (endNode:Label2 PROPERTY2) WHERE CONDITION CREATE (startNode)-[:RELATIONSHIP_TYPE PROPERTIES]->(endNode)";
        if(from.getLabels().size() > 0)
        {
            template = template.replace("Label1", from.getLabels().get(randomly.getInteger(0, from.getLabels().size())).getName());
        }
        else {
            template = template.replace(":Label1", "");
        }
        if(to.getLabels().size() > 0)
        {
            template = template.replace("Label2", to.getLabels().get(randomly.getInteger(0, to.getLabels().size())).getName());
        }
        else {
            template = template.replace(":Label2", "");
        }
        if(relation.getType() != null)
        {
            template = template.replace("RELATIONSHIP_TYPE", relation.getType().getName());
        }
        else {
            template = template.replace(":RELATIONSHIP_TYPE", "");
        }
        IExpression condition = generateEquationForSingleElement(from, "startNode");
        condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(to, "endNode"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
        StringBuilder conditionString = new StringBuilder();
        condition.toTextRepresentation(conditionString);
        boolean hasCondition = false;
        if(randomly.getInteger(0, 100)>50) {
            hasCondition = true;
            template = template.replace("CONDITION", conditionString.toString());
        }
        else
            template = template.replace("WHERE CONDITION", "");
        String randomKey = randomGetProperty(from);
        String randomKey2 = randomGetProperty(to);
        Map<String, Object> prop1 = new HashMap<>();
        prop1.put(randomKey, from.getProperties().get(randomKey));
        StringBuilder prop1sb = new StringBuilder();
        printProperties(prop1sb, prop1);
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("PROPERTY1", "");
        }
        else {
            template = template.replace("PROPERTY1", prop1sb.toString());
        }
        Map<String, Object> prop2 = new HashMap<>();
        prop2.put(randomKey2, to.getProperties().get(randomKey2));
        StringBuilder prop2sb = new StringBuilder();
        printProperties(prop2sb, prop2);
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("PROPERTY2", "");
        }
        else {
            template = template.replace("PROPERTY2", prop2sb.toString());
        }
        StringBuilder properties = new StringBuilder();
        printProperties(properties, relation.getProperties());
        template = template.replace("PROPERTIES", properties.toString());
        results.add(new CypherQueryAdapter(template));

        return results;
    }

    public List<CypherQueryAdapter> generateDeleteEdgeQueries(AbstractRelationship relation) {
        String template = "MATCH (Node1:LABEL1 PROPERTY1)-[Relation:LABEL2 PROPERTY2]->(Node3:LABEL3 PROPERTY3) WHERE CONDITION DELETE Relation";
        AbstractNode from = relation.getFrom();
        AbstractNode to = relation.getTo();
        if(from.getLabels().size() > 0)
        {
            template = template.replace("LABEL1", from.getLabels().get(randomly.getInteger(0, from.getLabels().size())).getName());
        }
        else {
            template = template.replace(":LABEL1", "");
        }
        if(to.getLabels().size() > 0)
        {
            template = template.replace("LABEL3", to.getLabels().get(randomly.getInteger(0, to.getLabels().size())).getName());
        }
        else {
            template = template.replace(":LABEL3", "");
        }
        if(relation.getType() != null)
        {
            template = template.replace("LABEL2", relation.getType().getName());
        }
        else {
            template = template.replace(":LABEL2", "");
        }
        IExpression condition = generateEquationForSingleElement(from, "Node1");
        condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(to, "Node3"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
        condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(relation, "Relation"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
        StringBuilder conditionString = new StringBuilder();
        condition.toTextRepresentation(conditionString);
        boolean hasCondition = false;
        if(randomly.getInteger(0, 100)>50) {
            hasCondition = true;
            template = template.replace("CONDITION", conditionString.toString());
        }
        else
            template = template.replace("WHERE CONDITION", "");
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("PROPERTY1", "");
        }
        else {
            String randomKey = randomGetProperty(from);
            Map<String, Object> prop1 = new HashMap<>();
            prop1.put(randomKey, from.getProperties().get(randomKey));
            StringBuilder prop1sb = new StringBuilder();
            printProperties(prop1sb, prop1);
            template = template.replace("PROPERTY1", prop1sb.toString());
        }
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("PROPERTY3", "");
        }
        else {
            String randomKey = randomGetProperty(to);
            Map<String, Object> prop1 = new HashMap<>();
            prop1.put(randomKey, to.getProperties().get(randomKey));
            StringBuilder prop1sb = new StringBuilder();
            printProperties(prop1sb, prop1);
            template = template.replace("PROPERTY3", prop1sb.toString());
        }
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("PROPERTY2", "");
        }
        else {
            String randomKey = randomGetProperty(relation);
            Map<String, Object> prop1 = new HashMap<>();
            prop1.put(randomKey, relation.getProperties().get(randomKey));
            StringBuilder prop1sb = new StringBuilder();
            printProperties(prop1sb, prop1);
            template = template.replace("PROPERTY2", prop1sb.toString());
        }
        List<CypherQueryAdapter> results = new ArrayList<>();
        results.add(new CypherQueryAdapter(template));
        return results;
    }
    public List<CypherQueryAdapter> generateDeleteCreateEdgeQueries(AbstractRelationship oldRelation, AbstractRelationship newRelation){
        List<CypherQueryAdapter> results = new ArrayList<>();
        String template = "MATCH (sourceNode:OLDSOURCELABEL OLDSOURCEPROP)-[oldRelationship:OLDRELATIONLABEL OLDRELATIONPROP]->(destinationNodeOld:OLDDESLABEL OLDDESPROP) WHERE CONDITION CREATE (sourceNodeNew:NEWSOURCELABEL NEWSOURCEPROP)-[newRelationship:NEWRELATIONLABEL NEWRELATIONPROP]->(destinationNodeNew:NEWDESLABEL NEWDESPROP) DELETE oldRelationship";
        AbstractNode from = oldRelation.getFrom();
        AbstractNode to = oldRelation.getTo();
        if(from.getLabels().size() > 0)
        {
            template = template.replace("OLDSOURCELABEL", from.getLabels().get(randomly.getInteger(0, from.getLabels().size())).getName());
        }
        else {
            template = template.replace(":OLDSOURCELABEL", "");
        }
        if(to.getLabels().size() > 0)
        {
            template = template.replace("OLDDESLABEL", to.getLabels().get(randomly.getInteger(0, to.getLabels().size())).getName());
        }
        else {
            template = template.replace(":OLDDESLABEL", "");
        }
        if(oldRelation.getType() != null)
        {
            template = template.replace("OLDRELATIONLABEL", oldRelation.getType().getName());
        }
        else {
            template = template.replace(":OLDRELATIONLABEL", "");
        }

        IExpression condition = generateEquationForSingleElement(from, "sourceNode");
        condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(to, "destinationNodeOld"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
        condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(oldRelation, "oldRelationship"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
        StringBuilder conditionString = new StringBuilder();
        condition.toTextRepresentation(conditionString);
        boolean hasCondition = false;
        if(randomly.getInteger(0, 100)>50) {
            hasCondition = true;
            template = template.replace("CONDITION", conditionString.toString());
        }
        else
            template = template.replace("WHERE CONDITION", "");

        String randomKey = randomGetProperty(from);
        Map<String, Object> prop1 = new HashMap<>();
        prop1.put(randomKey, from.getProperties().get(randomKey));
        StringBuilder prop1sb = new StringBuilder();
        printProperties(prop1sb, prop1);
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("OLDSOURCEPROP", "");
        }
        else {
            template = template.replace("OLDSOURCEPROP", prop1sb.toString());
        }
        String randomKey2 = randomGetProperty(to);
        Map<String, Object> prop2 = new HashMap<>();
        prop2.put(randomKey2, to.getProperties().get(randomKey2));
        StringBuilder prop2sb = new StringBuilder();
        printProperties(prop2sb, prop2);
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("OLDDESPROP", "");
        }
        else {
            template = template.replace("OLDDESPROP", prop2sb.toString());
        }
        String randomKey3 = randomGetProperty(oldRelation);
        Map<String, Object> prop3 = new HashMap<>();
        prop3.put(randomKey3, oldRelation.getProperties().get(randomKey3));
        StringBuilder prop3sb = new StringBuilder();
        printProperties(prop3sb, prop3);
        if(hasCondition && randomly.getInteger(0, 100)>50)
        {
            template = template.replace("OLDRELATIONPROP", "");
        }
        else {
            template = template.replace("OLDRELATIONPROP", prop3sb.toString());
        }

        from = newRelation.getFrom();
        to = newRelation.getTo();
        if(from.getLabels().size() > 0)
        {
            template = template.replace("NEWSOURCELABEL", from.getLabels().get(randomly.getInteger(0, from.getLabels().size())).getName());
        }
        else {
            template = template.replace(":NEWSOURCELABEL", "");
        }
        if(to.getLabels().size() > 0)
        {
            template = template.replace("NEWDESLABEL", to.getLabels().get(randomly.getInteger(0, to.getLabels().size())).getName());
        }
        else {
            template = template.replace(":NEWDESLABEL", "");
        }
        if(newRelation.getType() != null)
        {
            template = template.replace("NEWRELATIONLABEL", newRelation.getType().getName());
        }
        else {
            template = template.replace(":NEWRELATIONLABEL", "");
        }

        String randomKey4 = randomGetProperty(from);
        Map<String, Object> prop4 = new HashMap<>();
        prop4.put(randomKey4, from.getProperties().get(randomKey4));
        StringBuilder prop4sb = new StringBuilder();
        printProperties(prop4sb, prop4);
        template = template.replace("NEWSOURCEPROP", prop4sb.toString());
        String randomKey5 = randomGetProperty(to);
        Map<String, Object> prop5 = new HashMap<>();
        prop5.put(randomKey5, to.getProperties().get(randomKey5));
        StringBuilder prop5sb = new StringBuilder();
        printProperties(prop5sb, prop5);
        template = template.replace("NEWDESPROP", prop5sb.toString());
        StringBuilder prop6sb = new StringBuilder();
        printProperties(prop6sb, newRelation.getProperties());
        template = template.replace("NEWRELATIONPROP", prop6sb.toString());
        results.add(new CypherQueryAdapter(template));
        return results;
    }
    public List<CypherQueryAdapter> generateAlterEdgeQueries(AbstractRelationship oldRelation, AbstractRelationship newRelation)
    {
        List<CypherQueryAdapter> results = new ArrayList<>();
        String template = "MATCH (sourceNode:OLDSOURCELABEL OLDSOURCEPROP)-[oldRelationship:OLDRELATIONLABEL OLDRELATIONPROP]->(destinationNodeOld:OLDDESLABEL OLDDESPROP) WHERE CONDITION CHANGE";
        AbstractNode from = oldRelation.getFrom();
        AbstractNode to = oldRelation.getTo();
        oldRelation = oldRelation.getCopy();
        Map<String, Object> oldpros = new HashMap<>(oldRelation.getProperties());
        Map<String, Object> newpros = new HashMap<>(newRelation.getProperties());
        for(String curEntry : oldpros.keySet())
        {
            String forDelProperties = new String(template);
            if(from.getLabels().size() > 0)
            {
                template = template.replace("OLDSOURCELABEL", from.getLabels().get(randomly.getInteger(0, from.getLabels().size())).getName());
            }
            else {
                template = template.replace(":OLDSOURCELABEL", "");
            }
            if(to.getLabels().size() > 0)
            {
                template = template.replace("OLDDESLABEL", to.getLabels().get(randomly.getInteger(0, to.getLabels().size())).getName());
            }
            else {
                template = template.replace(":OLDDESLABEL", "");
            }
            if(oldRelation.getType() != null)
            {
                template = template.replace("OLDRELATIONLABEL", oldRelation.getType().getName());
            }
            else {
                template = template.replace(":OLDRELATIONLABEL", "");
            }

            IExpression condition = generateEquationForSingleElement(from, "sourceNode");
            condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(to, "destinationNodeOld"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
            condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(oldRelation, "oldRelationship"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
            StringBuilder conditionString = new StringBuilder();
            condition.toTextRepresentation(conditionString);
            boolean hasCondition = false;
            if(randomly.getInteger(0, 100)>50) {
                hasCondition = true;
                template = template.replace("CONDITION", conditionString.toString());
            }
            else
                template = template.replace("WHERE CONDITION", "");

            String randomKey = randomGetProperty(from);
            Map<String, Object> prop1 = new HashMap<>();
            prop1.put(randomKey, from.getProperties().get(randomKey));
            StringBuilder prop1sb = new StringBuilder();
            printProperties(prop1sb, prop1);
            if(hasCondition && randomly.getInteger(0, 100)>50)
            {
                template = template.replace("OLDSOURCEPROP", "");
            }
            else {
                template = template.replace("OLDSOURCEPROP", prop1sb.toString());
            }
            String randomKey2 = randomGetProperty(to);
            Map<String, Object> prop2 = new HashMap<>();
            prop2.put(randomKey2, to.getProperties().get(randomKey2));
            StringBuilder prop2sb = new StringBuilder();
            printProperties(prop2sb, prop2);
            if(hasCondition && randomly.getInteger(0, 100)>50)
            {
                template = template.replace("OLDDESPROP", "");
            }
            else {
                template = template.replace("OLDDESPROP", prop2sb.toString());
            }
            String randomKey3 = randomGetProperty(oldRelation);
            Map<String, Object> prop3 = new HashMap<>();
            prop3.put(randomKey3, oldRelation.getProperties().get(randomKey3));
            StringBuilder prop3sb = new StringBuilder();
            printProperties(prop3sb, prop3);
            if(hasCondition && randomly.getInteger(0, 100)>50)
            {
                template = template.replace("OLDRELATIONPROP", "");
            }
            else {
                template = template.replace("OLDRELATIONPROP", prop3sb.toString());
            }
            if(!newpros.containsKey(curEntry))
            {
                template = template.replace("CHANGE", "REMOVE oldRelationship._NEW1_");
                template = template.replace("_NEW1_", curEntry);
                results.add(new CypherQueryAdapter(template));
                template = new String(forDelProperties);
                oldRelation.properties.remove(curEntry);
            }
            else {
                template = template.replace("CHANGE", "SET oldRelationship._NEW1_ = _NEW2_");
                template = template.replace("_NEW1_", curEntry);
                template = template.replace("_NEW2_", getFormattedValue(newpros.get(curEntry)));
                results.add(new CypherQueryAdapter(template));
                template = new String(forDelProperties);
                oldRelation.properties.put(curEntry, newpros.get(curEntry));
            }
        }

        for(String curEntry : newpros.keySet())
        {
            String forDelProperties = new String(template);
            if(!oldpros.containsKey(curEntry))
            {if(from.getLabels().size() > 0)
            {
                template = template.replace("OLDSOURCELABEL", from.getLabels().get(randomly.getInteger(0, from.getLabels().size())).getName());
            }
            else {
                template = template.replace(":OLDSOURCELABEL", "");
            }
                if(to.getLabels().size() > 0)
                {
                    template = template.replace("OLDDESLABEL", to.getLabels().get(randomly.getInteger(0, to.getLabels().size())).getName());
                }
                else {
                    template = template.replace(":OLDDESLABEL", "");
                }
                if(oldRelation.getType() != null)
                {
                    template = template.replace("OLDRELATIONLABEL", oldRelation.getType().getName());
                }
                else {
                    template = template.replace(":OLDRELATIONLABEL", "");
                }

                IExpression condition = generateEquationForSingleElement(from, "sourceNode");
                condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(to, "destinationNodeOld"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                condition = new BinaryLogicalExpression(condition, generateEquationForSingleElement(oldRelation, "oldRelationship"), BinaryLogicalExpression.BinaryLogicalOperation.AND);
                StringBuilder conditionString = new StringBuilder();
                condition.toTextRepresentation(conditionString);
                boolean hasCondition = false;
                if(randomly.getInteger(0, 100)>50) {
                    hasCondition = true;
                    template = template.replace("CONDITION", conditionString.toString());
                }
                else
                    template = template.replace("WHERE CONDITION", "");

                String randomKey = randomGetProperty(from);
                Map<String, Object> prop1 = new HashMap<>();
                prop1.put(randomKey, from.getProperties().get(randomKey));
                StringBuilder prop1sb = new StringBuilder();
                printProperties(prop1sb, prop1);
                if(hasCondition && randomly.getInteger(0, 100)>50)
                {
                    template = template.replace("OLDSOURCEPROP", "");
                }
                else {
                    template = template.replace("OLDSOURCEPROP", prop1sb.toString());
                }
                String randomKey2 = randomGetProperty(to);
                Map<String, Object> prop2 = new HashMap<>();
                prop2.put(randomKey2, to.getProperties().get(randomKey2));
                StringBuilder prop2sb = new StringBuilder();
                printProperties(prop2sb, prop2);
                if(hasCondition && randomly.getInteger(0, 100)>50)
                {
                    template = template.replace("OLDDESPROP", "");
                }
                else {
                    template = template.replace("OLDDESPROP", prop2sb.toString());
                }
                String randomKey3 = randomGetProperty(oldRelation.properties);
                Map<String, Object> prop3 = new HashMap<>();
                prop3.put(randomKey3, oldRelation.getProperties().get(randomKey3));
                StringBuilder prop3sb = new StringBuilder();
                printProperties(prop3sb, prop3);
                if(hasCondition && randomly.getInteger(0, 100)>50)
                {
                    template = template.replace("OLDRELATIONPROP", "");
                }
                else {
                    template = template.replace("OLDRELATIONPROP", prop3sb.toString());
                }
                template = template.replace("CHANGE", "SET oldRelationship._NEW1_ = _NEW2_");
                template = template.replace("_NEW1_", curEntry);
                template = template.replace("_NEW2_", getFormattedValue(newpros.get(curEntry)));
                results.add(new CypherQueryAdapter(template));
                template = new String(forDelProperties);
                oldRelation.properties.put(curEntry, newpros.get(curEntry));
            }
        }


        return results;
    }


    public List<CypherQueryAdapter> generateDeleteNodeQueries(AbstractNode node){
            List<CypherQueryAdapter> results = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            List<String> availableKeys = new ArrayList<>(node.getProperties().keySet());
            String randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
            while(node.getProperties().get(randomKey) instanceof Boolean)
            {
                randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
            }
            String template = "MATCH (n0:LABEL PROPERTIES) WHERE CONDITION DETACH DELETE n0";
            if(node.getLabels().isEmpty()) {
                template = template.replace(":LABEL", "");
            }
            else {
                ILabel randomLabel = node.getLabels().get(randomly.getInteger(0, node.getLabels().size()));
                template = template.replace("LABEL", randomLabel.getName());
            }
            StringBuilder properties = new StringBuilder();
            Map<String, Object> props = new HashMap<>();
            props.put(randomKey, node.getProperties().get(randomKey));
            printProperties(properties, props);
            boolean hasCondition = false;
            if(randomly.getInteger(0, 100)>50) {
                hasCondition = true;
                StringBuilder conditionString = new StringBuilder();
                IExpression condition = generateEquationForSingleElement(node, "n0");
                condition.toTextRepresentation(conditionString);
                template = template.replace("CONDITION", conditionString.toString());
            }
            else
                template = template.replace("WHERE CONDITION", "");
            if(hasCondition && randomly.getInteger(0, 100)>50)
            {
                template = template.replace("PROPERTIES", "");
            }
            else {
                template = template.replace("PROPERTIES", properties.toString());
            }
            sb.append(template);
            results.add(new CypherQueryAdapter(sb.toString()));
            return results;
    }

    public List<CypherQueryAdapter> generateCreateNodeQueries(AbstractNode node){
        List<CypherQueryAdapter> results = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE ");
            sb.append("(n0 ");
            node.getLabelInfos().forEach(
                l->{
                    sb.append(":").append(l.getName());
                }
            );
            printProperties(sb, node.getProperties());
            sb.append(")");
            results.add(new CypherQueryAdapter(sb.toString()));
        return results;
    }

    public List<CypherQueryAdapter> generateAlterNodeQueries(AbstractNode newNode, AbstractNode oldNode){
        List<CypherQueryAdapter> results = new ArrayList<>();
        Map<String, Object> oldProperties = oldNode.getProperties();
        Map<String, Object> availableProps = new HashMap<>(oldNode.getProperties());
        List<String> availableKeys = new ArrayList<>(oldNode.getProperties().keySet());
        for(String curEntry : oldProperties.keySet())
        {
            if(!newNode.getProperties().containsKey(curEntry))
            {
                String randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                while(availableProps.get(randomKey) instanceof Boolean)
                {
                    randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                }
                StringBuilder sb = new StringBuilder();
                String template = "MATCH (n0:LABEL PROPERTIES) REMOVE n0._NEW1_";
                if(oldNode.getLabels().size() == 0) {
                    template = template.replace(":LABEL", "");
                }
                else {
                    ILabel randomLabel = oldNode.getLabels().get(randomly.getInteger(0, oldNode.getLabels().size()));
                    template = template.replace("LABEL", randomLabel.getName());
                }
                StringBuilder properties = new StringBuilder();
                Map<String, Object> props = new HashMap<>();
                props.put(randomKey, availableProps.get(randomKey));
                printProperties(properties, props);
                template = template.replace("PROPERTIES", properties.toString());
                sb.append(template.replace("_NEW1_", curEntry));
                results.add(new CypherQueryAdapter(sb.toString()));
                availableKeys.remove(curEntry);
                availableProps.remove(curEntry);
            }
            else {
                String randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                while(availableProps.get(randomKey) instanceof Boolean)
                {
                    randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                }
                StringBuilder sb = new StringBuilder();
                String template = "MATCH (n0:LABEL PROPERTIES) SET n0._NEW1_ = _NEW2_";
                if(oldNode.getLabels().size() == 0)
                {
                    template = template.replace(":LABEL", "");
                }
                else {
                    ILabel randomLabel = oldNode.getLabels().get(randomly.getInteger(0, oldNode.getLabels().size()));
                    template = template.replace("LABEL", randomLabel.getName());
                }
                StringBuilder properties = new StringBuilder();
                Map<String, Object> props = new HashMap<>();
                props.put(randomKey, availableProps.get(randomKey));
                printProperties(properties, props);
                sb.append(template.replace("PROPERTIES", properties.toString()).replace("_NEW1_", curEntry).replace("_NEW2_", getFormattedValue((Object)newNode.getProperties().get(curEntry))));
                results.add(new CypherQueryAdapter(sb.toString()));
                availableProps.put(curEntry, newNode.getProperties().get(curEntry));
            }
        }

        for(String curEntry : newNode.getProperties().keySet())
        {
            if(!oldProperties.containsKey(curEntry))
            {
                String template = "MATCH (n0:LABEL PROPERTIES) SET n0._NEW1_ = _NEW2_";
                String randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                while(availableProps.get(randomKey) instanceof Boolean)
                {
                    randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                }
                StringBuilder sb = new StringBuilder();
                if(oldNode.getLabels().size() == 0) {
                    template = template.replace(":LABEL", "");
                }
                else {
                    ILabel randomLabel = oldNode.getLabels().get(randomly.getInteger(0, oldNode.getLabels().size()));
                    template = template.replace("LABEL", randomLabel.getName());
                }
                StringBuilder properties = new StringBuilder();
                Map<String, Object> props = new HashMap<>();
                props.put(randomKey, availableProps.get(randomKey));
                printProperties(properties, props);
                template = template.replace("PROPERTIES", properties.toString());
                template = template.replace("_NEW1_", curEntry);
                template = template.replace("_NEW2_", getFormattedValue((Object)newNode.getProperties().get(curEntry)));
                sb.append(template);
                results.add(new CypherQueryAdapter(sb.toString()));
                availableProps.put(curEntry, newNode.getProperties().get(curEntry));
                availableKeys.add(curEntry);
            }
        }


        List<ILabel> oldLabels = oldNode.getLabels();
        List<ILabel> newLabels = newNode.getLabels();

        List<ILabel> availableLabel = new ArrayList<>(oldLabels);


        for(ILabel curLabel : oldLabels)
        {
            if(!newLabels.contains(curLabel))
            {
                StringBuilder sb = new StringBuilder();
                String template = "MATCH (n0:LABEL PROPERTIES) WHERE CONDITION REMOVE n0:CUR";
                ILabel randomLabel;
                if(availableLabel.size()>0) {
                    randomLabel = availableLabel.get(randomly.getInteger(0, availableLabel.size()));
                    template = template.replace("LABEL", randomLabel.getName());
                }
                else {
                    template = template.replace(":LABEL", "");
                }

                StringBuilder properties = new StringBuilder();
                Map<String, Object> props = new HashMap<>();
                String randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                while(availableProps.get(randomKey) instanceof Boolean)
                {
                    randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                }
                props.put(randomKey, availableProps.get(randomKey));
                printProperties(properties, props);
                template = template.replace("PROPERTIES", properties.toString());
                if(randomly.getInteger(0, 100)>50)
                {
                    StringBuilder conditionString = new StringBuilder();
                    IExpression condition = generateEquationForSingleElement(newNode, "n0");
                    condition.toTextRepresentation(conditionString);
                    template = template.replace("CONDITION", conditionString.toString());
                }
                else {
                    template = template.replace("WHERE CONDITION", "");
                }
                sb.append(template.replace("CUR", curLabel.getName()));
                results.add(new CypherQueryAdapter(sb.toString()));
                availableLabel.remove(curLabel);
            }
        }


        for(ILabel curLabel : newLabels)
        {
            if(!oldLabels.contains(curLabel))
            {
                String template = "MATCH (n0:OLD PROPERTY) SET n0:LABEL";
                StringBuilder sb = new StringBuilder();
                if(availableLabel.isEmpty())
                {
                    template = template.replace(":OLD", "");
                }
                else {
                    ILabel randomLabel = availableLabel.get(randomly.getInteger(0, availableLabel.size()));
                    template = template.replace("OLD", randomLabel.getName());
                }
                StringBuilder properties = new StringBuilder();
                Map<String, Object> props = new HashMap<>();
                String randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                while(availableProps.get(randomKey) instanceof Boolean)
                {
                    randomKey = availableKeys.get(randomly.getInteger(0, availableKeys.size()));
                }
                props.put(randomKey, availableProps.get(randomKey));
                printProperties(properties, props);
                template = template.replace("PROPERTY", properties.toString());
                template = template.replace("LABEL", curLabel.getName());
                sb.append(template);
                results.add(new CypherQueryAdapter(sb.toString()));
                availableLabel.add(curLabel);
            }
        }
        return results;
    }

    public List<CypherQueryAdapter> generateCreateGraphQueries(){
        List<CypherQueryAdapter> results = new ArrayList<>();
        long nodeNum = randomly.getInteger(maxNodeNumber / 2, maxNodeNumber);
        long relationshipNum = randomly.getInteger(maxNodeNumber / 2, maxNodeNumber * 2);
        if(relationshipNum > MainOptions.maxRelationNumber)
            relationshipNum = MainOptions.maxRelationNumber;


        Map<Pair<String, String>, List<IRelationTypeInfo>> fromtomap = new HashMap<>();
        if(MainOptions.mode == "kuzu") {
            for (IRelationTypeInfo relationTypeInfo : schema.getRelationshipTypeInfos()) {
                CypherSchema.CypherRelationTypeInfo relationTypeInfo1 = (CypherSchema.CypherRelationTypeInfo) relationTypeInfo;

                for(Pair<String, String> fromto : relationTypeInfo1.fromto){
                    if(!fromtomap.containsKey(fromto)){
                        fromtomap.put(fromto, new ArrayList<>());
                    }
                    fromtomap.get(fromto).add(relationTypeInfo1);
                }
            }
        }


        Set<String> deduplicate = new HashSet<>();
        for(int i = 0; i < nodeNum; i++){
            AbstractNode node = randomColorNode(deduplicate);
            nodes.add(node);
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE ");
            sb.append("(n0 ");
            node.getLabelInfos().forEach(
                l->{
                    sb.append(":").append(l.getName());
                }
            );
            printProperties(sb, node.getProperties());
            sb.append(")");
            results.add(new CypherQueryAdapter(sb.toString()));
        }

        for(int i = 0; i < relationshipNum; i++){
            AbstractNode n0 = nodes.get(randomly.getInteger(0, nodes.size()));
            AbstractNode n1 = nodes.get(randomly.getInteger(0, nodes.size()));

            AbstractRelationship relationship = null;
            if(MainOptions.mode == "kuzu") {
                relationship = randomColorRelationship(deduplicate, n0, n1, fromtomap);
                if(relationship == null)
                    continue;
            }
            else
                relationship = randomColorRelationship(deduplicate);
            n0.addRelationship(relationship);
            n1.addRelationship(relationship);
            relationship.setFrom(n0);
            relationship.setTo(n1);
            relationships.add(relationship);
            StringBuilder sb = new StringBuilder();
            sb.append("MATCH ");
            sb.append("(n0 {id : ").append(""+n0.getId()).append("})");
            sb.append(", ");
            sb.append("(n1 {id : ").append(""+n1.getId()).append("})");
            if(MainOptions.mode == "kuzu")
                sb.append(" CREATE");
            else
                sb.append(" MERGE");
            sb.append("(n0)-[r ").append(":").append(relationship.getType().getName());
            printProperties(sb, relationship.getProperties());
            sb.append("]->(n1)");

            results.add(new CypherQueryAdapter(sb.toString()));
        }


        MatrixRep.initialize(nodes, relationships);

        return results;
    }

    public List<AbstractNode> getNodes(){
        return nodes;
    }

    public List<AbstractRelationship> getRelationships(){
        return relationships;
    }

    private void printProperties(StringBuilder sb, Map<String, Object> properties){
        if(properties.size() != 0){
            sb.append("{");
            boolean first = true;
            for(Map.Entry<String, Object> pair : properties.entrySet()){
                if(!first){
                    sb.append(", ");
                }
                first = false;
                sb.append(pair.getKey());
                sb.append(" : ");
                if(pair.getValue() instanceof String){
                    sb.append("\"").append(pair.getValue()).append("\"");
                }
                else if(pair.getValue() instanceof Number){
                    sb.append(pair.getValue());
                }
                else if(pair.getValue() instanceof Boolean){
                    sb.append(pair.getValue());
                }
            }
            sb.append("}");
        }
    }

    public String getFormattedValue(Object value){
        if(value instanceof String){
            return "\"" + value + "\"";
        }
        else if(value instanceof Number){
            return value.toString();
        }
        else if(value instanceof Boolean){
            return value.toString();
        }
        else{
            throw new RuntimeException();
        }
    }


    private Object generateValue(IPropertyInfo propertyInfo){





        {
            List<Object> values = new ArrayList<>();
            for(int i = 0; i < 20; i++){
                switch (propertyInfo.getType()){
                    case NUMBER:
                        if(MainOptions.mode == "falkordb")
                            values.add(randomly.getLong(Integer.MIN_VALUE, Integer.MAX_VALUE));
                        else
                            values.add(randomly.getLong(Long.MIN_VALUE, Long.MAX_VALUE));
                        break;
                    case BOOLEAN:
                        values.add(randomly.getLong(0, 2) == 0);
                        break;
                    case STRING:
                        values.add(randomly.getString());
                        break;
                }
            }
            propertyValues.put(propertyInfo, values);
            return values.get(randomly.getInteger(0, values.size()));
        }
    }

    private AbstractNode randomColorNode(Set<String> deduplicate){
        List<ILabelInfo> labels = new ArrayList<>(schema.getLabelInfos());
        AbstractNode node = new AbstractNode();



        long labelNum = getRandomLabelNum(labels.size());
        if(MainOptions.mode == "kuzu")
            labelNum = 1;
        if(MainOptions.mode == "thinker")
            labelNum = 1;
        List<ILabelInfo> selectedLabels = new ArrayList<>();
        for(int j = 0; j < labelNum; j++){
            ILabelInfo current = labels.get(randomly.getInteger(0, labels.size()-1));
            if(!selectedLabels.contains(current))
                selectedLabels.add(current);
        }
        node.setLabelInfos(selectedLabels);
        generateProperties(node, deduplicate);

        return node;
    }

    private AbstractRelationship randomColorRelationship(Set<String> deduplicate){
        AbstractRelationship relationship = new AbstractRelationship();
        List<IRelationTypeInfo> relationTypeInfos = new ArrayList<>(schema.getRelationshipTypeInfos());

        IRelationTypeInfo relationTypeInfo = null;
        if(relationTypeInfos.size() != 0){
            relationTypeInfo = relationTypeInfos.get(randomly.getInteger(0, relationTypeInfos.size()));
        }
        relationship.setType(relationTypeInfo);
        generateProperties(relationship, deduplicate);

        return relationship;
    }

    private AbstractRelationship randomColorRelationship(Set<String> deduplicate, AbstractNode from, AbstractNode to, Map<Pair<String, String>, List<IRelationTypeInfo>> fromtomap){
        AbstractRelationship relationship = new AbstractRelationship();
        List<IRelationTypeInfo> relationTypeInfos = null;
        long cnt = 0;
        while(cnt < max(from.getLabels().size(), to.getLabels().size()))
        {
            String fromLabel = from.getLabels().get(randomly.getInteger(0, from.getLabels().size()-1)).getName();
            String toLabel = to.getLabels().get(randomly.getInteger(0, to.getLabels().size()-1)).getName();
            if(fromtomap.containsKey(new Pair<>(fromLabel, toLabel)))
            {
                relationTypeInfos = new ArrayList<>(fromtomap.get(new Pair<>(fromLabel, toLabel)));
                break;
            }
            cnt++;
        }
        if(relationTypeInfos == null) {
            System.out.println("Cannot found properly typed relations, return rull");
            return null;
        }

        IRelationTypeInfo relationTypeInfo = null;
        if(relationTypeInfos.size() != 0){
            relationTypeInfo = relationTypeInfos.get(randomly.getInteger(0, relationTypeInfos.size()));
        }
        relationship.setType(relationTypeInfo);
        generateProperties(relationship, deduplicate);

        return relationship;
    }

    private void generateProperties(AbstractNode abstractNode, Set<String> deduplicate){
        Map<String, Object> result = new HashMap<>();

        abstractNode.setId(presentID);
        for(ILabelInfo labelInfo : abstractNode.getLabelInfos()){
            for(IPropertyInfo propertyInfo : labelInfo.getProperties()){
                if(randomly.getInteger(0, 100) < 95){
                    Object currentValue = generateValue(propertyInfo);
                    while (deduplicate.contains(currentValue.toString())){
                        currentValue = generateValue(propertyInfo);
                    }
                    result.put(propertyInfo.getKey(), currentValue);
                    if(!(currentValue instanceof Boolean))
                        deduplicate.add(currentValue.toString());
                }
            }
        }
        result.put("id", presentID);
        presentID++;
        abstractNode.setProperties(result);
    }

    private void generateProperties(AbstractRelationship abstractRelationship, Set<String> deduplicate){
        Map<String, Object> result = new HashMap<>();

        abstractRelationship.setId(presentID);
        if(abstractRelationship.getType() != null){
            for(IPropertyInfo propertyInfo : abstractRelationship.getType().getProperties()){
                if(randomly.getInteger(0, 100) < 95){
                    Object currentValue = generateValue(propertyInfo);
                    while (deduplicate.contains(currentValue.toString())){
                        currentValue = generateValue(propertyInfo);
                    }
                    result.put(propertyInfo.getKey(), generateValue(propertyInfo));
                    if(!(currentValue instanceof Boolean))
                        deduplicate.add(currentValue.toString());
                }
            }
        }
        result.put("id", presentID);
        presentID++;
        abstractRelationship.setProperties(result);
    }

    public List<SubgraphTreeNodeInstance> randomCluster(){
        List<AbstractNode> closureNodes = new ArrayList<>();
        AbstractNode node = getNodes().get(randomly.getInteger(0, getNodes().size()));

        if(node.getRelationships().isEmpty()){
            Subgraph subgraph = new Subgraph();
            subgraph.addNode(node);
            SubgraphTreeNode treeNode = new SubgraphTreeNode(subgraph);
            SubgraphTreeNodeInstance instance = new SubgraphTreeNodeInstance();
            instance.setTreeNode(treeNode);
            instance.setIds(new ArrayList<>(Arrays.asList(node.getId())));
            instance.setProperties(new ArrayList<>(Arrays.asList(node.getProperties())));
            return new ArrayList<>(Arrays.asList(instance));
        }

        List<SubgraphTreeNodeInstance> instances = new ArrayList<>();

        closureNodes.add(node);

        AbstractRelationship root = node.getRelationships().get(randomly.getInteger(0, node.getRelationships().size()));
        Set<AbstractRelationship> closureRelationships = new HashSet<>();
        Set<AbstractRelationship> availableRelationships = new HashSet<>();
        availableRelationships.add(root);


        long closureSize = randomly.getInteger(3, 7);

        for(int i = 0; i < closureSize; i++){
            if(availableRelationships.isEmpty()){
                break;
            }
            List<AbstractRelationship> availables = availableRelationships.stream().collect(Collectors.toList());
            AbstractRelationship relationship = availables.get(randomly.getInteger(0, availables.size()));
            addToClosure(closureRelationships, availableRelationships, relationship);
            if(randomly.getInteger(0, 100) < 50){
                List<AbstractRelationship> candidates = new ArrayList<>(relationship.getFrom().getRelationships());
                candidates = candidates.stream().filter(r->r != relationship).collect(Collectors.toList());

                if(candidates.size() == 0){
                    instances.add(randomInstance(Arrays.asList(relationship.getFrom(), relationship.getTo()), relationships));
                }
                else{
                    AbstractRelationship other = candidates.get(randomly.getInteger(0, candidates.size()));
                    AbstractNode otherNode = other.getFrom() == relationship.getFrom() ? other.getTo() : other.getFrom();
                    instances.add(randomInstance(Arrays.asList(otherNode, relationship.getFrom(), relationship.getTo()),
                            Arrays.asList(other, relationship)));
                }
            }
            else {
                List<AbstractRelationship> candidates = new ArrayList<>(relationship.getTo().getRelationships());
                candidates = candidates.stream().filter(r->r != relationship).collect(Collectors.toList());

                if(candidates.size() == 0){
                    instances.add(randomInstance(Arrays.asList(relationship.getFrom(), relationship.getTo()), relationships));
                }
                else {
                    AbstractRelationship other = candidates.get(randomly.getInteger(0, candidates.size()));
                    AbstractNode otherNode = other.getFrom() == relationship.getTo() ? other.getTo() : other.getFrom();
                    instances.add(randomInstance(Arrays.asList(relationship.getFrom(), relationship.getTo(), otherNode),
                            Arrays.asList(relationship, other)));
                }
            }
        }

        return instances;
    }

    private SubgraphTreeNodeInstance randomInstance(List<AbstractNode> nodes, List<AbstractRelationship> relationships){
        boolean reverse = randomly.getInteger(0, 100) < 50;
        if(reverse){
            Collections.reverse(nodes);
            Collections.reverse(relationships);
        }

        Subgraph subgraph = new Subgraph();
        SubgraphTreeNode treeNode = new SubgraphTreeNode(subgraph);
        SubgraphTreeNodeInstance instance = new SubgraphTreeNodeInstance();
        instance.setTreeNode(treeNode);
        instance.setIds(new ArrayList<>());
        instance.setProperties(new ArrayList<>());


        for(int i = 0; i < nodes.size(); i++){
            AbstractNode node = nodes.get(i);
            subgraph.addNode(node);
            instance.getIds().add(node.getId());
            instance.getProperties().add(node.getProperties());

            if(i < relationships.size()){
                AbstractRelationship relationship = relationships.get(i);
                subgraph.addRelationship(relationship);
                instance.getIds().add(relationship.getId());
                instance.getProperties().add(relationship.getProperties());
            }
        }

        return instance;
    }

    private void addToClosure(Set<AbstractRelationship> closureRelationships, Set<AbstractRelationship> availableRelationships, AbstractRelationship relationship){
        availableRelationships.remove(relationship);
        closureRelationships.add(relationship);

        List<AbstractRelationship> candidates = new ArrayList<>(relationship.getFrom().getRelationships());
        candidates.addAll(relationship.getTo().getRelationships());
        candidates.forEach(
                r->{
                    if(!closureRelationships.contains(r)){
                        availableRelationships.add(r);
                    }
                }
        );
    }



    private long getRandomLabelNum(long labelsSize){
        long maxNum = 1 << maxNodeColor;
        long randNum = randomly.getInteger(0, maxNum);
        long labelNum = maxNodeColor + 1;
        if(randNum == 0){
            labelNum = 0;
        }
        else {
            while (randNum > 0){
                randNum = randNum >> 1;
                labelNum--;
            }
        }
        return Math.min(options.getLabelNum(), Math.min(labelsSize, labelNum));
    }
    private SubgraphTreeNodeInstance fixedInstance(List<AbstractNode> nodes, List<AbstractRelationship> relationships){
        boolean reverse = randomly.getInteger(0, 100) < 50;
        if(reverse){
            Collections.reverse(nodes);
            Collections.reverse(relationships);
        }

        Subgraph subgraph = new Subgraph();
        SubgraphTreeNode treeNode = new SubgraphTreeNode(subgraph);
        SubgraphTreeNodeInstance instance = new SubgraphTreeNodeInstance();
        instance.setTreeNode(treeNode);
        instance.setIds(new ArrayList<>());
        instance.setProperties(new ArrayList<>());


        for(int i = 0; i < nodes.size(); i++){
            AbstractNode node = nodes.get(i);
            subgraph.addNode(node);
            instance.getIds().add(node.getId());
            instance.getProperties().add(node.getProperties());
        }

        for(int i = 0; i < relationships.size(); i++) {
            AbstractRelationship relationship = relationships.get(i);
            subgraph.addRelationship(relationship);
            instance.getIds().add(relationship.getId());
            instance.getProperties().add(relationship.getProperties());
        }

        return instance;
    }

    public List<SubgraphTreeNodeInstance> matrixCluster()
    {
        List<List<AbstractNode>> closureNodes = new ArrayList<>();
        List<List<AbstractRelationship>> clousureRelationships = new ArrayList<>();

        MatrixRep.fetchPath(closureNodes, clousureRelationships);

        List<SubgraphTreeNodeInstance> instances = new ArrayList<>();

        for(int i = 0; i < closureNodes.size(); i++)
        {
            instances.add(fixedInstance(closureNodes.get(i), clousureRelationships.get(i)));
        }

        return instances;




































































    }


    @Override
    public GraphManager clone() {
        try {
            GraphManager clone = (GraphManager) super.clone();


















            clone.nodes = new ArrayList<>(nodes);
            clone.relationships = new ArrayList<>(relationships);
            clone.schema = schema;
            clone.options = options;
            clone.propertyValues = new HashMap<>(propertyValues);
            clone.presentID = presentID;
            clone.maxNodeNumber = maxNodeNumber;
            clone.randomly = randomly;
            clone.MatrixRep = MatrixRep.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
