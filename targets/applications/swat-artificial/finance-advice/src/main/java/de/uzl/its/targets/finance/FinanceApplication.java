package de.uzl.its.targets.finance;

import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;

@SpringBootApplication
public class FinanceApplication {
    public static void main (String[] args) throws SQLException {
        String databasePort = System.getProperty("database.port", "9090");
        Server h2Server = Server.createTcpServer("-tcp", "-tcpPort", databasePort, "-ifNotExists").start();
        if (h2Server.isRunning(true)) {
            System.out.println("H2 server is running on port " + h2Server.getPort());
        } else {
            throw new RuntimeException("Could not start H2 server.");
        }
        SpringApplication.run(FinanceApplication.class, args);
    }
}
