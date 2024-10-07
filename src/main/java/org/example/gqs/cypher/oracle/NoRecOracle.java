package org.example.gqs.cypher.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.gen.condition.RandomConditionGenerator;
import org.example.gqs.cypher.gen.pattern.RandomPatternGenerator;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.cypher.standard_ast.Alias;
import org.example.gqs.cypher.standard_ast.Match;
import org.example.gqs.cypher.standard_ast.Ret;
import org.example.gqs.cypher.standard_ast.With;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoRecOracle <G extends CypherGlobalState<?,S>, S extends CypherSchema<G, ?>> implements TestOracle {
    private final G globalState;
    private RandomQueryGenerator<S, G> randomQueryGenerator;

    public NoRecOracle(G globalState){
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<>();
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = randomQueryGenerator.generateQuery(globalState);
        IClauseSequence equivalent = null;

        ICypherClause clauseBeforeReturn = sequence.getClauseList().get(sequence.getClauseList().size() - 2);

        IExpression condition = null;

        if (!(clauseBeforeReturn instanceof IMatch) || ((IMatch) clauseBeforeReturn).getCondition() == null) {
            Match match = new Match();
            sequence.addClauseAt(match, sequence.getClauseList().size() - 1);

            new RandomPatternGenerator<>(globalState.getSchema(), sequence.getIdentifierBuilder(), false).fillMatchPattern(match.toAnalyzer());
            while (match.getCondition() == null) {
                new RandomConditionGenerator<>(globalState.getSchema(), false).fillMatchCondtion(match.toAnalyzer());
            }

            condition = match.getCondition();
        } else {
            condition = ((IMatch) clauseBeforeReturn).getCondition();
        }

        IReturn lastClause = (IReturn) sequence.getClauseList().get(sequence.getClauseList().size() - 1);
        List<IRet> returnList = new ArrayList<>();
        returnList.add(Ret.createNewExpressionReturnVal(new CallExpression(
                CypherSchema.CypherBuiltInFunctions.COUNT, Arrays.asList(new Star())
        )));
        lastClause.setReturnList(returnList);
        lastClause.setDistinct(false);
        lastClause.setLimit(null);
        lastClause.setOrderBy(new ArrayList<>(), false);
        lastClause.setSkip(null);


        equivalent = sequence.getCopy();
        With with = new With();
        Ret ret = Ret.createNewExpressionAlias(equivalent.getIdentifierBuilder(), condition.getCopy());
        with.setReturnList(new ArrayList<>(Arrays.asList(ret)));
        with.setCondition(new BinaryComparisonExpression(new IdentifierExpression(Alias.createIdentifierRef(ret.getIdentifier())), new ConstExpression(true),
                BinaryComparisonExpression.BinaryComparisonOperation.EQUAL));
        equivalent.getClauseList().add(equivalent.getClauseList().size() - 1, with);

        List<IRet> mutatedReturnList = new ArrayList<>();
        mutatedReturnList.add(Ret.createNewExpressionReturnVal(new CallExpression(
                CypherSchema.CypherBuiltInFunctions.COUNT, Arrays.asList(new Star())
        )));
        IReturn lastClause2 = (IReturn) equivalent.getClauseList().get(equivalent.getClauseList().size() - 1);
        lastClause2.setReturnList(mutatedReturnList);


        if (sequence.getClauseList().size() <= 8) {
            StringBuilder sb = new StringBuilder();
            sequence.toTextRepresentation(sb);
            System.out.println(sb.toString());
            sb.delete(0, sb.length());
            equivalent.toTextRepresentation(sb);
            System.out.println(sb.toString());

            System.out.println(sb);
            GQSResultSet r = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString())).get(0);

            boolean isBugDetected = false;
            long resultLength = r.getRowNum();

            List<CypherSchema.CypherLabelInfo> labels = globalState.getSchema().getLabels();
            List<CypherSchema.CypherRelationTypeInfo> relations = globalState.getSchema().getRelationTypes();
            if (resultLength > 0) {
                randomQueryGenerator.addExecutionRecord(sequence, isBugDetected, resultLength);

                List<String> coveredProperty = new ArrayList<>();
                Pattern allProps = Pattern.compile("(\\.)(k\\d+)(\\))");
                Matcher matcher = allProps.matcher(sb);
                while (matcher.find()) {
                    if (!coveredProperty.contains(matcher.group(2))) {
                        coveredProperty.add(matcher.group(2));
                    }
                }

                for (String name : coveredProperty) {
                    found:
                    {
                        for (CypherSchema.CypherLabelInfo label : labels) {
                            List<IPropertyInfo> props = label.getProperties();
                            for (IPropertyInfo prop : props) {
                                if (Objects.equals(prop.getKey(), name)) {
                                    ((CypherSchema.CypherPropertyInfo) prop).addFreq();
                                    break found;
                                }
                            }
                        }
                        for (CypherSchema.CypherRelationTypeInfo relation : relations) {
                            List<IPropertyInfo> props = relation.getProperties();
                            for (IPropertyInfo prop : props) {
                                if (Objects.equals(prop.getKey(), name)) {
                                    ((CypherSchema.CypherPropertyInfo) prop).addFreq();
                                    break found;
                                }
                            }
                        }
                    }
                }
            }

            for (CypherSchema.CypherLabelInfo label : labels) {
                List<IPropertyInfo> props = label.getProperties();
                for (IPropertyInfo prop : props) {
                    System.out.println(label.getName() + ":" + prop.getKey() + ":" + ((CypherSchema.CypherPropertyInfo) prop).getFreq());
                }
            }
            for (CypherSchema.CypherRelationTypeInfo relation : relations) {
                List<IPropertyInfo> props = relation.getProperties();
                for (IPropertyInfo prop : props) {
                    System.out.println(relation.getName() + ":" + prop.getKey() + ":" + ((CypherSchema.CypherPropertyInfo) prop).getFreq());
                }
            }
        }
    }
}
