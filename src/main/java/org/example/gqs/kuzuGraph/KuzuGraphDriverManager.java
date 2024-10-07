package org.example.gqs.kuzuGraph;

import com.kuzudb.KuzuConnection;

import java.util.ArrayList;
import java.util.List;

public class KuzuGraphDriverManager {
    private static List<DriverInfo> registeredDrivers = new ArrayList<>();

    private static class DriverInfo{
        public KuzuConnection driver = null;
        public String url = "", username =  "", password = "";

        public DriverInfo(){
            this(null, "", "", "");
        }

        public DriverInfo(KuzuConnection driver, String url, String username, String password){
            this.driver = driver;
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }

}
