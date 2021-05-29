package tv.twitch.chat;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public enum ChatMessageTokenType {
    Text(0),
    Emoticon(1),
    Mention(2),
    Url(3),
    Bits(4);

    private static Map<Integer, ChatMessageTokenType> s_Map = new HashMap<>();
    private int m_Value;

    static {
        Iterator<ChatMessageTokenType> it = EnumSet.allOf(ChatMessageTokenType.class).iterator();
        while (it.hasNext()) {
            ChatMessageTokenType chatMessageTokenType = (ChatMessageTokenType) it.next();
            s_Map.put(chatMessageTokenType.getValue(), chatMessageTokenType);
        }
    }

    public static ChatMessageTokenType lookupValue(int i) {
        return s_Map.get(i);
    }

    private ChatMessageTokenType(int i) {
        this.m_Value = i;
    }

    public int getValue() {
        return this.m_Value;
    }
}
