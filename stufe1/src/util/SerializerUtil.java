package util;

import model.Message;

public class SerializerUtil {
    private static final char DELIMITER = 0x1F; // Unit Separator character

    public static String serialize(Message message) {
        return String.join(String.valueOf(DELIMITER), message.from(), message.to(), message.text());
    }

    public static Message deserialize(String message) {
        String[] msg = message.split(String.valueOf(DELIMITER));
        return new Message(msg[0], msg[1], msg[2]);
    }
}
