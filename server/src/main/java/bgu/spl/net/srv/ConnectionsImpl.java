package bgu.spl.net.srv;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ConnectionsImpl<T> implements Connections<T> {

    // Fields
    public Map<Integer, ConnectionHandler<T>> activeConecctions;
    private Map<T, LinkedList<Integer>> Channels;
    private Map<String,String> users;
    private volatile int msgId;
    Set<String> logged_in_users;
    Map<Integer, String> id_to_user;
    Map<String, Integer> user_to_id;

    // Constructor
    public ConnectionsImpl(){
        activeConecctions = new HashMap<>();
        Channels = new HashMap<>();
        users = new HashMap<>();
        msgId = 0;
        logged_in_users = new HashSet<>();
        id_to_user = new HashMap<>();
        user_to_id = new HashMap<>();
    }

    public boolean send(int connectionId, T msg){
        if (!activeConecctions.containsKey(connectionId)) return false;
        ConnectionHandler<T> handler = activeConecctions.get(connectionId);
        handler.send(msg);
        return true;
    }

    public void send(String channel, T msg){
        for (int id : Channels.get(channel)){
        // if the client disconnected from the channel
            if(!send(id, msg)) 
                Channels.get(channel).remove(id);
        }
    }

    public void disconnect(int connectionId){
        activeConecctions.remove(connectionId);
        
        String user = id_to_user.get(connectionId);
        id_to_user.remove(connectionId);
        user_to_id.remove(user);

        for(T channel: Channels.keySet()){
            if (Channels.get(channel).contains(connectionId)) Channels.get(channel).remove((Integer)connectionId);
        }
    }

    public void connect(int connectionId, ConnectionHandler<T> handler){
        activeConecctions.put(connectionId, handler);
    }

    public boolean login(int id, String userName, String passcode){
        if(!users.containsKey(userName)){
            users.put(userName, passcode);
            id_to_user.put(id, userName);
            user_to_id.put(userName, id);
            return true;
        }

        if(users.get(userName).equals(passcode)){
            id_to_user.put(id, userName);
            user_to_id.put(userName, id);
            return true;
        }

        return false;
    }

    public boolean subtoChannel(int connectionId, T channel){
        if (Channels.containsKey(channel) && Channels.get(channel).contains(connectionId))
            return false;
        if (!Channels.containsKey(channel)){
            Channels.put(channel, new LinkedList<>());
        }
        Channels.get(channel).addFirst(connectionId);
        return true;
    }

    public boolean unsubtoChannel(Integer connectionId, T channel){
        if (!Channels.containsKey(channel) || !Channels.get(channel).contains(connectionId))
            return false;
        Channels.get(channel).remove(connectionId);
        return true;
    }

    public synchronized int getAndIncrement(){
        return msgId++;
    }

    public boolean isLoggedIn(String user){
        return user_to_id.keySet().contains(user);
    }
}
