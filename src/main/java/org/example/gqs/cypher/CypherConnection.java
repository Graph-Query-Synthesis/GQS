package org.example.gqs.cypher;

import org.example.gqs.GDSmithDBConnection;
import org.example.gqs.common.query.GQSResultSet;

import java.util.List;

public abstract class CypherConnection implements GDSmithDBConnection {

    public void executeStatement(String arg) throws Exception{
        System.out.println("execute statement: "+arg);
    }

    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        System.out.println("execute statement: "+arg);
        return null;
    }

    public List<Long> executeStatementAndGetTime(String arg) throws Exception{
        return null;
    }

    public void reproduce(List<String> queries){
    }
}
