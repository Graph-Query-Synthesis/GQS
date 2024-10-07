package org.example.gqs.memGraph;

import org.neo4j.driver.*;

import java.util.ArrayList;
import java.util.List;

public class MemGraphDriverManager {
    private static List<DriverInfo> registeredDrivers = new ArrayList<>();

    private static class DriverInfo{
        public Driver driver = null;
        public String url = "", username =  "", password = "";

        public DriverInfo(){
            this(null, "", "", "");
        }

        public DriverInfo(Driver driver, String url, String username, String password){
            this.driver = driver;
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }

    public static synchronized Driver getDriver(String url, String username, String password){
        for(DriverInfo driverInfo : registeredDrivers){
            if(driverInfo.url.equals(url) && driverInfo.username.equals(username) && driverInfo.password.equals(password)){
                boolean flag = false;
                Session session = driverInfo.driver.session();
                String query = "MATCH (n) RETURN count(n) AS count";
                try {
                    Result result = session.run(query);
                    flag = true;
                }
                catch (Exception e){
                    flag = false;
                    registeredDrivers.remove(driverInfo);
                }
                finally {
                    session.close();
                }
                if(flag)
                    return driverInfo.driver;
            }
        }
        Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
        registeredDrivers.add(new DriverInfo(driver, url, username, password));
        return driver;
    }

    public static synchronized void closeDriver(Driver driver){
        DriverInfo closedDriver = null;
        for(DriverInfo driverInfo : registeredDrivers){
            if(driverInfo.driver == driver){
                closedDriver = driverInfo;
                try{
                    driver.close();
                }
                catch (Exception e) {
                    System.out.println("The driver has been closed already");
                }
            }
        }
        registeredDrivers.remove(closedDriver);
    }
}
