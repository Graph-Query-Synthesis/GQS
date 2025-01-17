package org.example.gqs.cypher.gen.list;

import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.ast.analyzer.IUnwindAnalyzer;
import org.example.gqs.cypher.dsl.BasicListGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.gen.expr.RandomExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.Ret;

public class RandomListGenerator<S extends CypherSchema<?,?>> extends BasicListGenerator<S> {
    private boolean overrideOld;
    public RandomListGenerator(S schema, IIdentifierBuilder identifierBuilder, boolean overrideOld) {
        super(schema, identifierBuilder);
        this.overrideOld = overrideOld;
    }

    @Override
    public IRet generateList(IUnwindAnalyzer unwindAnalyzer, IIdentifierBuilder identifierBuilder, S schema) {
        if (unwindAnalyzer.getListAsAliasRet() != null && !overrideOld) {
            return unwindAnalyzer.getListAsAliasRet();
        }
        IExpression listExpression = new RandomExpressionGenerator<>(unwindAnalyzer, schema).generateListWithBasicType(2, CypherType.NUMBER);
        return Ret.createNewExpressionAlias(identifierBuilder, listExpression);
    }
}
