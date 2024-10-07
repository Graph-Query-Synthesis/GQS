package org.example.gqs.cypher.standard_ast.expr;

import org.example.gqs.cypher.ast.ICypherClause;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;

import java.util.Set;

public abstract class CypherExpression implements IExpression {
    protected IExpression parentExpression;
    protected ICypherClause parentClause;

    @Override
    public IExpression getParentExpression() {
        return parentExpression;
    }
    abstract public Object getValue();

    @Override
    public void setParentExpression(IExpression parentExpression) {
        this.parentExpression = parentExpression;
    }

    @Override
    public ICypherClause getExpressionRootClause() {
        if(parentExpression != null){
            return parentExpression.getExpressionRootClause();
        }
        return parentClause;
    }

    @Override
    public void setParentClause(ICypherClause parentClause){
        this.parentClause = parentClause;
    }
    abstract public Set<IIdentifier> reliedContent();
    abstract public void removeElement(Set<IIdentifier> toRemove);

}
