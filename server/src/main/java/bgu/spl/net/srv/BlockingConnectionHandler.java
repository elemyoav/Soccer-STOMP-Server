package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final MessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private int id;
    private Connections<T> connections;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol,int id,Connections<T> connections) {
        this.sock = sock;
        this.id = id;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
        if(protocol instanceof StompMessagingProtocol){
            StompMessagingProtocol s = (StompMessagingProtocol)protocol;
            s.start(id, (Connections<String>)connections);
            s.setHandler((ConnectionHandler<String>)this);
        }
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    T response = protocol.process(nextMessage);
                    if (response != null) {
                        send(response);
                    }
                }
            }
            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public synchronized void send(T msg) {
        try{
            out.write(encdec.encode(msg));
            out.flush();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
