package org.example.gqs;

import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.common.query.Query;
import org.example.gqs.common.schema.AbstractSchema;
import org.example.gqs.common.schema.AbstractTable;
import org.neo4j.driver.exceptions.ClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class GlobalState<O extends DBMSSpecificOptions<?>, S extends AbstractSchema<?, ?>, C extends GDSmithDBConnection> {

    protected C databaseConnection;
    private Randomly r;
    private MainOptions options;
    public O dbmsSpecificOptions;
    private S schema;
    private Main.SLog logger;
    private StateToReproduce state;
    private Main.QueryManager<C> manager;
    private String databaseName;

    public void setConnection(C con) {
        this.databaseConnection = con;
    }

    public C getConnection() {
        return databaseConnection;
    }

    @SuppressWarnings("unchecked")
    public void setDbmsSpecificOptions(Object dbmsSpecificOptions) {
        this.dbmsSpecificOptions = (O) dbmsSpecificOptions;
    }

    public O getDbmsSpecificOptions() {
        return dbmsSpecificOptions;
    }

    public void setRandomly(Randomly r) {
        this.r = r;
    }

    public Randomly getRandomly() {
        return r;
    }

    public MainOptions getOptions() {
        return options;
    }

    public void setMainOptions(MainOptions options) {
        this.options = options;
    }

    public void setStateLogger(Main.SLog logger) {
        this.logger = logger;
    }

    public Main.SLog getLogger() {
        return logger;
    }

    public void setState(StateToReproduce state) {
        this.state = state;
    }

    public StateToReproduce getState() {
        return state;
    }

    public Main.QueryManager<C> getManager() {
        return manager;
    }

    public void setManager(Main.QueryManager<C> manager) {
        this.manager = manager;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    private ExecutionTimer executePrologue(Query<?> q) throws Exception {
        boolean logExecutionTime = getOptions().logExecutionTime();
        ExecutionTimer timer = null;
        if (logExecutionTime) {
            timer = new ExecutionTimer().start();
        }
        if (getOptions().printAllStatements()) {
            System.out.println(q.getLogString());
        }
        if (getOptions().logEachSelect()) {
            if (logExecutionTime) {
                getLogger().writeCurrentNoLineBreak(q.getLogString());
            } else {
                getLogger().writeCurrent(q.getLogString());
            }
        }
        return timer;
    }

    protected abstract void executeEpilogue(Query<?> q, boolean success, ExecutionTimer timer) throws Exception;

    public boolean executeStatement(Query<C> q, String... fills) throws Exception {
        ExecutionTimer timer = executePrologue(q);
        String query = q.getQueryString();
        boolean success;
        success = manager.execute(q, fills);
        executeEpilogue(q, success, timer);
        return success;
    }

    public List<GQSResultSet> executeStatementAndGet(Query<C> q, String... fills) throws Exception {
        ExecutionTimer timer = executePrologue(q);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<GQSResultSet> results = new ArrayList<>();
        Future<String> future = executor.submit(new Callable<String>() {
            public String call() throws Exception {
                results.addAll(manager.executeAndGet(q, fills));
                return "done";
            }
        });


        try{
            future.get(MainOptions.timeout, java.util.concurrent.TimeUnit.SECONDS);
        } catch (TimeoutException e){
            future.cancel(true);
            System.out.println("Illegal Timeout Status!!!");
            throw new TimeoutException();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new ClientException(e.getMessage());
        }
        catch (ClientException e)
        {
            throw new ClientException(e.getMessage());
        }
        finally {
            executor.shutdownNow();
        }
        boolean success = results != null;
        try {
            executeEpilogue(q, success, timer);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return results;
    }

    public List<Long> executeStatementAndGetTime(Query<C> q, String... fills) throws Exception {
        ExecutionTimer timer = executePrologue(q);
        List<Long> results = manager.executeAndGetTime(q, fills);
        boolean success = results != null;
        try {
            executeEpilogue(q, success, timer);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return results;
    }

    public S getSchema() {
        if (schema == null) {
            updateSchema();
        }
        return schema;
    }

    protected void setSchema(S schema) {
        this.schema = schema;
    }

    public void updateSchema() {
        setSchema(readSchema());
        for (AbstractTable<?, ?, ?> table : schema.getDatabaseTables()) {
            table.recomputeCount();
        }
    }

    protected abstract S readSchema();

}
