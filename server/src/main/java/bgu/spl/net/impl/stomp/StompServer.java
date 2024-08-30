package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.MessageEncoderDecoderImpl;
import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        if(args.length == 0){
            Server<String> server = Server.threadPerClient(7777, ()-> new StompMessagingProtocol(), ()-> new MessageEncoderDecoderImpl());
            server.serve();
        }

        int port = Integer.decode(args[0]).intValue();
        if(args[1].equals("tpc")){
            Server<String> server = Server.threadPerClient(port, ()-> new StompMessagingProtocol(), ()-> new MessageEncoderDecoderImpl());
            server.serve();
        }

        if(args[1].equals("reactor") ){
            Server<String> server = Server.reactor(3, port, ()-> new StompMessagingProtocol(), ()-> new MessageEncoderDecoderImpl());
            server.serve();
        }
    }
}
