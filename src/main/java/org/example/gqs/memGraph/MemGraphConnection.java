package org.example.gqs.memGraph;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.exceptions.MustRestartDatabaseException;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

import java.util.Arrays;
import java.util.List;

public class MemGraphConnection extends CypherConnection {

    private Driver driver;
    private MemGraphOptions options;

    public MemGraphConnection(Driver driver, MemGraphOptions options){
        this.driver = driver;
        this.options = options;
    }


    @Override
    public String getDatabaseVersion() throws Exception {
        return "memgraph";
    }

    @Override
    public void close() throws Exception {
        MemGraphDriverManager.closeDriver(driver);
    }

    @Override
    public void executeStatement(String arg) throws Exception{
        try ( Session session = driver.session() ) {
            String greeting = session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {
                    tx.run(arg);
                    return "";
                }
            });
        } catch (ServiceUnavailableException e){
            e.printStackTrace();
            Process process = Runtime.getRuntime().exec(options.restartCommand);
            process.waitFor();
            Thread.sleep(10000);
            throw new MustRestartDatabaseException(e);
        }
    }

    public void nonTxnExecuteStatement(String arg) throws Exception{
        try ( Session session = driver.session() ) {
            session.run(arg);
        } catch (ServiceUnavailableException e){
            e.printStackTrace();
            Process process = Runtime.getRuntime().exec(options.restartCommand);
            process.waitFor();
            Thread.sleep(10000);
            throw new MustRestartDatabaseException(e);
        }
    }


    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        try ( Session session = driver.session() )
        {
            return Arrays.asList(new GQSResultSet(session.run(arg)));
        } catch (ServiceUnavailableException e){
            e.printStackTrace();
            Process process = Runtime.getRuntime().exec(options.restartCommand);
            process.waitFor();
            Thread.sleep(10000);
            throw new MustRestartDatabaseException(e);
        }
    }
}
