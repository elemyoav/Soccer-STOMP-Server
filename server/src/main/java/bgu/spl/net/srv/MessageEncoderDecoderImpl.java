package bgu.spl.net.srv;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class MessageEncoderDecoderImpl implements MessageEncoderDecoder<String> {
    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
    
    public String decodeNextByte(byte nextByte){
        if ((char)nextByte == '\u0000')
        {
            return popString();
        }
        pushByte(nextByte);
        return null;
    }

    private void pushByte(byte nextByte){
        if (len >= bytes.length){
            bytes = Arrays.copyOf(bytes, 2 * len);
        }
        bytes[len] = nextByte;
        len++;
    }

    private String popString(){
        String result = new String(bytes, 0 , len, StandardCharsets.UTF_8);
        len = 0;
        return result + '\u0000';
    }

    public byte[] encode(String message){
        return message.getBytes();
    }
}
