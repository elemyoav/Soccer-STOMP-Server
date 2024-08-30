package bgu.spl.net.impl.echo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class EchoClient {

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            args = new String[]{"localhost", "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:gorbe\npasscode:240799\n\n^@"};
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }

        //BufferedReader and BufferedWriter automatically using UTF-8 encoding
        try (Socket sock = new Socket(args[0], 7777);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            System.out.println('\u0000');
            System.out.println("sending message to server");
            out.write(args[1] + '\u0000');
            out.flush();

            System.out.println("awaiting response");
            int read;
            while((read = in.read()) > 0) {System.out.print((char)read);}
            
            System.out.println();

            out.write("SUBSCRIBE\ndestination:/topics/a\nid:78\n\n^@\u0000");
            out.flush();
           
            out.write("SEND\ndestination:/topics/a\n\nHello Topic A!\n^@\u0000");
            out.flush();

            while((read = in.read()) > 0) {System.out.print((char)read);}

            System.out.println();

            out.write("SEND\ndestination:/topics/a\n\nHello Topic A!\n^@\u0000");
            out.flush();

            while((read = in.read()) > 0) {System.out.print((char)read);}

            System.out.println();

            out.write("SEND\ndestination:/topics/a\n\nHello Topic A!\n^@\u0000");
            out.flush();

            while((read = in.read()) > 0) {System.out.print((char)read);}

            System.out.println();

            out.write("SEND\ndestination:/topics/a\n\nHello Topic A!\n^@\u0000");
            out.flush();

            while((read = in.read()) > 0) {System.out.print((char)read);}

            System.out.println();
            
            // out.write("DISCONNECT\nreceipt:77\n\n^@\u0000");
            // out.flush();

            while((read = in.read()) >= 0) {System.out.print((char)read);}
            System.out.println();
        }
    }
}
