package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class StompMessagingProtocol implements bgu.spl.net.api.StompMessagingProtocol<String>{

    private int id;
    private Connections<String> connections;
    private boolean shouldTerminate;
    private Map<String, String> subIdToChannel;
    private Map<String, String> channelTosubId;
    private ConnectionHandler<String> handler;
    private int index;


    public void start(int connectionId, Connections<String> connections)
    {
        id = connectionId;
        this.connections = connections;
        shouldTerminate = false;
        subIdToChannel = new HashMap<>();
        channelTosubId = new HashMap<>();
    }

    public void setHandler(ConnectionHandler<String> handler){
        this.handler = handler;
    }
    
    public String process(String message)
    {
        
        String command = new String();

        while(message.charAt(index) != '\n')
        {
            command += message.charAt(index);
            index++;
        }

        if(command.equals("CONNECT"))
        {
            Connect(message);
        }

        else if(command.equals("SEND"))
        {

            Send(message);
        }

        else if(command.equals("SUBSCRIBE"))
        {
            Subscribe(message);
        }

        else if(command.equals("UNSUBSCRIBE"))
        {
            Unsubscribe(message);
        }

        else if(command.equals("DISCONNECT"))
        {
            Disconnect(message);
        }
        else
        {
            handle_error(message,"malformed frame received", "Command does not exist", new HashMap<String,String>());
        }
        index = 0;
        return null;
    }
	
	/**
     * @return true if the connection should be terminated
     */
    public boolean shouldTerminate(){
        return shouldTerminate;
    }

    private void Connect(String message){

        //System.out.println("Connecting");

        Map<String, String> headers = getHeaders(message);
        String response = "";

        connections.connect(id, handler);
        
        if(connections.isLoggedIn(headers.get("login"))){
            handle_error(message,"user already logged in", "", headers);
        }
        else if(connections.login(id, headers.get("login"), headers.get("passcode"))){
            response = "CONNECTED\nversion:1.2\n\n\u0000";
            connections.send(id, response);

            if(headers.containsKey("receipt")) handleReceipt(headers.get("receipt"));
        }
        else {
            handle_error(message, "Wrong password", "", headers);
        }
    }

    private void Send(String message){
        //System.out.println("Sending");
        
        Map<String, String> headers = getHeaders(message);

        Set<String> expected_header = new HashSet<>();
        expected_header.add("destination");
        if(!headers.keySet().equals(expected_header)){
            handle_error(message,"malformed frame received", "did not contain the expected headers", headers);
            return;
        }
    
        String ClientMsg = getMessage(message);

        String channel = headers.get("destination");
        if (!channelTosubId.containsKey(channel)){
            handle_error(message, "not subscribed to channel", "", headers);
        }
        else {
            if(headers.containsKey("receipt")) handleReceipt(headers.get("receipt"));
            connections.send(headers.get("destination"), "MESSAGE\nsubscription:"
            +channelTosubId.get(channel)+"\nmessage-id:"+connections.getAndIncrement()+
            "\ndestination:"+channel+"\n\n"+ClientMsg+'\u0000');
        }
        
    }

    public void Subscribe(String message){
        //System.out.println("Subing");
        Map<String, String> headers = getHeaders(message);

        Set<String> expected_header = new HashSet<>();
        expected_header.add("destination");
        expected_header.add("id");

        expected_header.add("receipt");

        if(!headers.keySet().equals(expected_header)){
            handle_error(message,"malformed frame received", "did not contain the expected headers", headers);
            return;
        }

        String subId = headers.get("id");
        String channel = headers.get("destination");
        subIdToChannel.put(subId, channel);
        channelTosubId.put(channel, subId);
        if (!connections.subtoChannel(id, channel)){
            handle_error(message, "Already subscribed to channel", "", headers);
        }
        if(headers.containsKey("receipt")) handleReceipt(headers.get("receipt"));
    }

    public void Unsubscribe(String message){
        Map<String, String> headers = getHeaders(message);

        Set<String> expected_header = new HashSet<>();
        expected_header.add("id");

        expected_header.add("receipt");


        if(!headers.keySet().equals(expected_header)){
            handle_error(message,"malformed frame received", "did not contain the expected headers", headers);
            return;
        }

        String subId = headers.get("id");
        String channel = subIdToChannel.get(subId);
        subIdToChannel.remove(subId, channel);
        channelTosubId.remove(channel, subId);
        if(!connections.unsubtoChannel(id, channel)){
            handle_error(message, "Not subscribed to channel", "", headers);
        }
        
        if(headers.containsKey("receipt")) handleReceipt(headers.get("receipt"));
    }
    
    public void Disconnect(String message){

        Map<String, String> headers = getHeaders(message);

        Set<String> expected_header = new HashSet<>();
        expected_header.add("receipt");
        if(!headers.keySet().equals(expected_header)){
            handle_error(message,"malformed frame received", "did not contain the expected headers", headers);
            return;
        }
        
        
        shouldTerminate = true;
        handleReceipt(headers.get("receipt"));
        connections.disconnect(id);
        
    }
    
    
    
    private Map<String,String> getHeaders(String message )
    {

        boolean key = true;
        String k = "";
        String v = "";
        Map<String, String> headers = new HashMap<>();
        index++;

        while(!(message.charAt(index) == '\n' && key))
        {
            if(message.charAt(index) == ':'){
                key = false;
                index++;
                continue;
            }

            if(message.charAt(index) == '\n'){
                key = true;
                headers.put(k, v);
                k = "";
                v = "";
                index++;
                continue;
            }

            if(key) k += message.charAt(index);
            else v += message.charAt(index);

            index++;
        }

        return headers;
    }

    private String getMessage(String msg)
    {
        index++;
        String ret = "";
        while(msg.charAt(index) != '\u0000')
        {
            ret += msg.charAt(index++);

        }
        return ret;
    }

    private void handleReceipt(String receipt_id){
        connections.send(id, "RECEIPT\nreceipt-id:"+receipt_id+"\n\n\u0000");
    }
    
    private void handle_error(String message,String messageToUser, String error, Map<String, String> headers){
        String packet ="ERROR\n";
        if(headers.containsKey("receipt"))
            packet += "receipt-id:"+headers.get("receipt")+"\n";
        packet += "message:" + messageToUser + "\n\nThe message:\n-----\n";
        packet += message.replace('\u0000', '\n');
        packet +="-----\n";
        packet+=error + "\n\u0000";

        connections.send(id, packet);
        shouldTerminate = true;
        connections.disconnect(id);
    }
}
