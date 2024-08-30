package bgu.spl.net.srv;

import java.io.IOException;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    void disconnect(int connectionId);

    boolean login(int id, String userName, String passcode);

    boolean subtoChannel(int connectionId, T channel);

    boolean unsubtoChannel(Integer connectionId, T channel);

    int getAndIncrement();
    
    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean isLoggedIn(String user);
}
