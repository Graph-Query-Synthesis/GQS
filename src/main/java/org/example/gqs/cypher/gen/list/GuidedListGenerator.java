package org.example.gqs.cypher.gen.list;

import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.ast.analyzer.IUnwindAnalyzer;
import org.example.gqs.cypher.dsl.BasicListGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.gen.expr.NonEmptyExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.Ret;

import java.util.Map;

public class GuidedListGenerator<S extends CypherSchema<?,?>> extends BasicListGenerator<S> {
    private boolean overrideOld;
    private Map<String, Object> varToVal;
    public GuidedListGenerator(S schema, IIdentifierBuilder identifierBuilder, boolean overrideOld, Map<String, Object> varToVal) {
        super(schema, identifierBuilder);
        this.overrideOld = overrideOld;
        this.varToVal = varToVal;
    }

    @Override
    public IRet generateList(IUnwindAnalyzer unwindAnalyzer, IIdentifierBuilder identifierBuilder, S schema) {
        if (unwindAnalyzer.getListAsAliasRet() != null && !overrideOld) {
            return unwindAnalyzer.getListAsAliasRet();
        }
        IExpression listExpression = new NonEmptyExpressionGenerator<>(unwindAnalyzer, schema, varToVal).generateListWithBasicType(2, CypherType.ANY);
        return Ret.createNewExpressionAlias(identifierBuilder, listExpression);
    }
}
