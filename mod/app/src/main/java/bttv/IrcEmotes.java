package bttv;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bttv.emote.Emote;
import bttv.emote.EmoteUrlUtil;
import bttv.emote.Emotes;
import bttv.emote.WideEmotes;

/**
 * Adds our emotes to the "emotes" tag of raw IRC chat lines,
 * so the chat renders them like regular Twitch emotes with the id "BTTV-<id>".
 */
public class IrcEmotes {
    private static final String TAG = "LBTTVIrcEmotes";
    private static final String PRIVMSG = " PRIVMSG #";
    public static final String ROOMSTATE = " ROOMSTATE #";
    private static final String EMOTES_TAG = "emotes=";
    private static final String ROOM_ID_TAG = "room-id=";

    private static int rewrittenLines = 0;

    public static String rewriteFrame(String frame) {
        // one frame can hold multiple lines
        if (frame.indexOf("\r\n") == frame.length() - 2 || !frame.contains("\r\n")) {
            return rewriteLine(frame);
        }
        String[] lines = frame.split("\r\n", -1);
        StringBuilder out = new StringBuilder(frame.length() + 64);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append("\r\n");
            }
            out.append(rewriteLine(lines[i]));
        }
        return out.toString();
    }

    /**
     * The chat gets a ROOMSTATE line right after it joined a channel,
     * so the channel is known before the first message arrives (e.g. for the emote menu).
     */
    public static void onRoomState(String frame) {
        for (String line : frame.split("\r\n")) {
            if (!line.startsWith("@")) {
                continue;
            }
            int tagsEnd = line.indexOf(' ');
            if (tagsEnd == -1 || line.indexOf(ROOMSTATE, tagsEnd) == -1) {
                continue;
            }
            String roomId = getTag(line.substring(1, tagsEnd), ROOM_ID_TAG);
            if (roomId == null || roomId.isEmpty()) {
                continue;
            }
            int channel = Integer.parseInt(roomId);
            if (channel != Data.currentBroadcasterId) {
                Data.setCurrentBroadcasterId(channel);
            }
        }
    }

    private static String rewriteLine(String line) {
        try {
            if (!line.startsWith("@")) {
                return line;
            }
            int tagsEnd = line.indexOf(' ');
            int command = line.indexOf(PRIVMSG, tagsEnd);
            if (tagsEnd == -1 || command == -1) {
                return line;
            }
            int textStart = line.indexOf(" :", command);
            if (textStart == -1) {
                return line;
            }
            textStart += 2;

            String tags = line.substring(1, tagsEnd);
            String text = line.substring(textStart);
            String lineEnd = "";
            if (text.endsWith("\r\n")) {
                lineEnd = "\r\n";
                text = text.substring(0, text.length() - 2);
            }
            if (text.startsWith("\u0001")) {
                // /me messages, positions are relative to the action
                return line;
            }

            String roomId = getTag(tags, ROOM_ID_TAG);
            if (roomId == null || roomId.isEmpty()) {
                return line;
            }
            int channel = Integer.parseInt(roomId);
            if (channel != Data.currentBroadcasterId) {
                Data.setCurrentBroadcasterId(channel);
            }

            Context ctx = Data.ctx;
            if (ctx == null) {
                return line;
            }

            String emotes = getTag(tags, EMOTES_TAG);
            if (emotes == null) {
                return line;
            }
            Set<Integer> taken = takenPositions(emotes);

            // positions are counted in code points
            Map<String, List<int[]>> found = new LinkedHashMap<>();
            // wide emotes get an invisible marker behind them, see WideEmotes
            StringBuilder newText = new StringBuilder(text.length() + 16);
            List<int[]> insertions = new ArrayList<>(); // [position in the original text, length]
            int position = 0;
            int inserted = 0;
            int i = 0;
            int length = text.length();
            while (i < length) {
                int wordEnd = text.indexOf(' ', i);
                if (wordEnd == -1) {
                    wordEnd = length;
                }
                if (wordEnd > i) {
                    String word = text.substring(i, wordEnd);
                    newText.append(word);
                    int codePoints = word.codePointCount(0, word.length());
                    if (!taken.contains(position)) {
                        Emote emote = Emotes.getEmote(ctx, word, channel);
                        if (emote != null) {
                            String marker = WideEmotes.marker(emote);
                            String id = (marker.isEmpty() ? "BTTV-" : EmoteUrlUtil.WIDE_PREFIX) + emote.id;
                            List<int[]> positions = found.get(id);
                            if (positions == null) {
                                positions = new ArrayList<>();
                                found.put(id, positions);
                            }
                            int start = position + inserted;
                            positions.add(new int[]{start, start + codePoints - 1});

                            if (!marker.isEmpty()) {
                                newText.append(marker);
                                insertions.add(new int[]{position + codePoints, marker.length()});
                                inserted += marker.length();
                            }
                        }
                    }
                    position += codePoints;
                }
                // the space
                if (wordEnd < length) {
                    newText.append(' ');
                }
                position += 1;
                i = wordEnd + 1;
            }

            if (found.isEmpty()) {
                return line;
            }

            StringBuilder newEmotes = new StringBuilder(shiftPositions(emotes, insertions));
            for (Map.Entry<String, List<int[]>> entry : found.entrySet()) {
                if (newEmotes.length() > 0) {
                    newEmotes.append('/');
                }
                newEmotes.append(entry.getKey()).append(':');
                boolean first = true;
                for (int[] range : entry.getValue()) {
                    if (!first) {
                        newEmotes.append(',');
                    }
                    first = false;
                    newEmotes.append(range[0]).append('-').append(range[1]);
                }
            }

            String newTags = setTag(tags, EMOTES_TAG, newEmotes.toString());
            String newLine = "@" + newTags + line.substring(tagsEnd, textStart) + newText + lineEnd;
            if (rewrittenLines++ < 5) {
                Log.d(TAG, "rewriteLine: emotes=" + newEmotes);
            }
            return newLine;
        } catch (Throwable t) {
            Log.e(TAG, "rewriteLine: ", t);
            return line;
        }
    }

    private static String getTag(String tags, String name) {
        int start = tags.startsWith(name) ? 0 : tags.indexOf(";" + name);
        if (start == -1) {
            return null;
        }
        start += (start == 0 ? 0 : 1) + name.length();
        int end = tags.indexOf(';', start);
        return end == -1 ? tags.substring(start) : tags.substring(start, end);
    }

    private static String setTag(String tags, String name, String value) {
        int start = tags.startsWith(name) ? 0 : tags.indexOf(";" + name);
        start += (start == 0 ? 0 : 1) + name.length();
        int end = tags.indexOf(';', start);
        return tags.substring(0, start) + value + (end == -1 ? "" : tags.substring(end));
    }

    // positions of the Twitch emotes move when we insert markers in front of them
    private static String shiftPositions(String emotes, List<int[]> insertions) {
        if (emotes.isEmpty() || insertions.isEmpty()) {
            return emotes;
        }
        StringBuilder out = new StringBuilder(emotes.length() + 8);
        for (String emote : emotes.split("/")) {
            int colon = emote.indexOf(':');
            if (colon == -1) {
                continue;
            }
            if (out.length() > 0) {
                out.append('/');
            }
            out.append(emote, 0, colon + 1);
            boolean first = true;
            for (String range : emote.substring(colon + 1).split(",")) {
                int dash = range.indexOf('-');
                if (dash <= 0) {
                    continue;
                }
                int start = Integer.parseInt(range.substring(0, dash));
                int end = Integer.parseInt(range.substring(dash + 1));
                int shift = 0;
                for (int[] insertion : insertions) {
                    if (insertion[0] <= start) {
                        shift += insertion[1];
                    }
                }
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(start + shift).append('-').append(end + shift);
            }
        }
        return out.toString();
    }

    private static Set<Integer> takenPositions(String emotes) {
        Set<Integer> taken = new HashSet<>();
        if (emotes.isEmpty()) {
            return taken;
        }
        for (String emote : emotes.split("/")) {
            int colon = emote.indexOf(':');
            if (colon == -1) {
                continue;
            }
            for (String range : emote.substring(colon + 1).split(",")) {
                int dash = range.indexOf('-');
                if (dash > 0) {
                    taken.add(Integer.parseInt(range.substring(0, dash)));
                }
            }
        }
        return taken;
    }
}
