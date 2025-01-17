package org.example.gqs;

public interface GDSmithDBConnection extends AutoCloseable {

    String getDatabaseVersion() throws Exception;
}
