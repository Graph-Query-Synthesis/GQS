package org.example.gqs.cypher.gen.list;

import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.ast.analyzer.IUnwindAnalyzer;
import org.example.gqs.cypher.dsl.BasicListGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.gen.EnumerationSeq;
import org.example.gqs.cypher.gen.expr.EnumerationExpressionGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.Ret;

public class EnumerationListGenerator<S extends CypherSchema<?,?>> extends BasicListGenerator<S> {
    private EnumerationSeq enumerationSeq;


    public EnumerationListGenerator(S schema, IIdentifierBuilder identifierBuilder, EnumerationSeq enumerationSeq) {
        super(schema, identifierBuilder);
        this.enumerationSeq = enumerationSeq;
    }

    @Override
    public IRet generateList(IUnwindAnalyzer unwindAnalyzer, IIdentifierBuilder identifierBuilder, S schema) {
        IExpression listExpression = new EnumerationExpressionGenerator<>(unwindAnalyzer, enumerationSeq, schema).generateListWithBasicType(2, CypherType.NUMBER);
        return Ret.createNewExpressionAlias(identifierBuilder, listExpression);
    }
}
