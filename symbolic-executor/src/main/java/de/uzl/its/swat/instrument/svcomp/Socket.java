package de.uzl.its.swat.instrument.svcomp;

import java.io.InputStream;

public class Socket {
    public Socket() {
        // create a new socket
        System.out.println("Creating mock socket...");
    }
    public Socket(String host, int port) {
        // create a new socket
        System.out.println("Creating mock socket to " + host + ":" + port + "...");
    }
    public InputStream getInputStream() {
        // return the input stream
        System.out.println("Getting input stream from mock socket...");
        return new InputStream() {
            @Override
            public int read() {
                // read from the input stream
                System.out.println("Reading from mock input stream...");
                return 0;
            }
        };
    }
    public void close() {
        // close the socket
        System.out.println("Closing mock socket...");
    }
    
}
