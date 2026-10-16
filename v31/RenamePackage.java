import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Gives the app another package name (and optionally another name), so it can be installed next to the official app.
 * Works on the compiled files, as the resources are not decoded for this Twitch version.
 *
 *   java RenamePackage.java manifest AndroidManifest.xml old.package new.package [label]
 *   java RenamePackage.java arsc resources.arsc old.package new.package
 *   java RenamePackage.java apk in.apk out.apk old.package new.package
 *   java RenamePackage.java icon ic_launcher.xml 7f060637
 */
public class RenamePackage {
    private static final int CHUNK_STRING_POOL = 0x0001;
    private static final int CHUNK_XML_START_ELEMENT = 0x0102;
    private static final int CHUNK_TABLE_PACKAGE = 0x0200;
    private static final int FLAG_UTF8 = 0x100;
    private static final int TYPE_REFERENCE = 0x01;
    private static final int TYPE_STRING = 0x03;

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "manifest": {
                byte[] in = Files.readAllBytes(Paths.get(args[1]));
                Files.write(Paths.get(args[1]), manifest(in, args[2], args[3], args.length > 4 ? args[4] : null));
                break;
            }
            case "arsc": {
                byte[] in = Files.readAllBytes(Paths.get(args[1]));
                Files.write(Paths.get(args[1]), arsc(in, args[2], args[3]));
                break;
            }
            case "apk":
                apk(args[1], args[2], args[3], args[4]);
                break;
            case "icon": {
                byte[] in = Files.readAllBytes(Paths.get(args[1]));
                Files.write(Paths.get(args[1]), iconBackground(in, (int) Long.parseLong(args[2], 16)));
                break;
            }
            default:
                throw new IllegalArgumentException(args[0]);
        }
    }

    /** the package attribute, authorities and permissions, not the names of classes */
    private static String rename(String value, String oldPackage, String newPackage) {
        if (value.equals(oldPackage)) {
            return newPackage;
        }
        if (value.endsWith("." + oldPackage)) {
            return value.substring(0, value.length() - oldPackage.length()) + newPackage;
        }
        if (value.startsWith(oldPackage + ".") && value.indexOf('.', oldPackage.length() + 1) == -1) {
            // classes have at least one more package below the package of the app
            return newPackage + value.substring(oldPackage.length());
        }
        return value;
    }

    static byte[] manifest(byte[] in, String oldPackage, String newPackage, String label) {
        ByteBuffer buffer = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
        int fileHeaderSize = buffer.getShort(2) & 0xFFFF;
        int poolStart = fileHeaderSize;
        if ((buffer.getShort(poolStart) & 0xFFFF) != CHUNK_STRING_POOL) {
            throw new IllegalStateException("string pool expected");
        }
        int poolSize = buffer.getInt(poolStart + 4);
        boolean utf8 = (buffer.getInt(poolStart + 16) & FLAG_UTF8) != 0;
        if (buffer.getInt(poolStart + 12) != 0) {
            throw new IllegalStateException("styles are not supported");
        }

        List<String> strings = readStrings(buffer, poolStart, utf8);
        int renamed = 0;
        for (int i = 0; i < strings.size(); i++) {
            String value = rename(strings.get(i), oldPackage, newPackage);
            if (!value.equals(strings.get(i))) {
                strings.set(i, value);
                renamed++;
            }
        }
        int labelIndex = -1;
        if (label != null) {
            // at the end, the first strings belong to the resource map
            labelIndex = strings.size();
            strings.add(label);
        }

        byte[] rest = new byte[in.length - poolStart - poolSize];
        System.arraycopy(in, poolStart + poolSize, rest, 0, rest.length);
        int labels = label == null ? 0 : setLabel(rest, strings, labelIndex);

        byte[] pool = writeStrings(strings, utf8, buffer.getInt(poolStart + 16));
        ByteBuffer out = ByteBuffer.allocate(fileHeaderSize + pool.length + rest.length).order(ByteOrder.LITTLE_ENDIAN);
        out.put(in, 0, fileHeaderSize);
        out.put(pool);
        out.put(rest);
        out.putInt(4, out.capacity());
        System.out.println("manifest: " + renamed + " strings renamed, " + labels + " labels set");
        return out.array();
    }

    /** the label of the application is a reference to a string resource, replace it (and the same reference on activities) */
    private static int setLabel(byte[] nodes, List<String> strings, int labelIndex) {
        ByteBuffer buffer = ByteBuffer.wrap(nodes).order(ByteOrder.LITTLE_ENDIAN);
        int applicationLabel = 0;
        int changed = 0;
        // two passes, the first one finds the reference
        for (int pass = 0; pass < 2; pass++) {
            int position = 0;
            while (position < nodes.length) {
                int type = buffer.getShort(position) & 0xFFFF;
                int headerSize = buffer.getShort(position + 2) & 0xFFFF;
                int size = buffer.getInt(position + 4);
                if (type == CHUNK_XML_START_ELEMENT) {
                    int body = position + headerSize;
                    String element = strings.get(buffer.getInt(body + 4));
                    int attributeStart = buffer.getShort(body + 8) & 0xFFFF;
                    int attributeSize = buffer.getShort(body + 10) & 0xFFFF;
                    int attributeCount = buffer.getShort(body + 12) & 0xFFFF;
                    for (int i = 0; i < attributeCount; i++) {
                        int attribute = body + attributeStart + i * attributeSize;
                        if (!"label".equals(strings.get(buffer.getInt(attribute + 4)))) {
                            continue;
                        }
                        int dataType = buffer.get(attribute + 15) & 0xFF;
                        int data = buffer.getInt(attribute + 16);
                        if (dataType != TYPE_REFERENCE) {
                            continue;
                        }
                        if (pass == 0 && element.equals("application")) {
                            applicationLabel = data;
                        } else if (pass == 1 && data == applicationLabel) {
                            buffer.putInt(attribute + 8, labelIndex);
                            buffer.put(attribute + 15, (byte) TYPE_STRING);
                            buffer.putInt(attribute + 16, labelIndex);
                            changed++;
                        }
                    }
                }
                position += size;
            }
            if (applicationLabel == 0) {
                break;
            }
        }
        return changed;
    }

    /**
     * the background of the adaptive launcher icon is a reference to a color resource, replace it with another one
     * (a color value is not possible, launchers load the icon for a density and that needs a resource)
     */
    static byte[] iconBackground(byte[] in, int color) {
        ByteBuffer buffer = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
        int poolStart = buffer.getShort(2) & 0xFFFF;
        boolean utf8 = (buffer.getInt(poolStart + 16) & FLAG_UTF8) != 0;
        List<String> strings = readStrings(buffer, poolStart, utf8);
        int position = poolStart + buffer.getInt(poolStart + 4);
        int changed = 0;
        while (position < in.length) {
            int type = buffer.getShort(position) & 0xFFFF;
            int headerSize = buffer.getShort(position + 2) & 0xFFFF;
            int size = buffer.getInt(position + 4);
            if (type == CHUNK_XML_START_ELEMENT) {
                int body = position + headerSize;
                int attributeStart = buffer.getShort(body + 8) & 0xFFFF;
                int attributeSize = buffer.getShort(body + 10) & 0xFFFF;
                int attributeCount = buffer.getShort(body + 12) & 0xFFFF;
                if (strings.get(buffer.getInt(body + 4)).equals("background")) {
                    for (int i = 0; i < attributeCount; i++) {
                        int attribute = body + attributeStart + i * attributeSize;
                        if (strings.get(buffer.getInt(attribute + 4)).equals("drawable")) {
                            buffer.putInt(attribute + 16, color);
                            changed++;
                        }
                    }
                }
            }
            position += size;
        }
        System.out.println("icon: " + changed + " backgrounds set");
        return in;
    }

    private static List<String> readStrings(ByteBuffer buffer, int poolStart, boolean utf8) {
        int count = buffer.getInt(poolStart + 8);
        int headerSize = buffer.getShort(poolStart + 2) & 0xFFFF;
        int stringsStart = poolStart + buffer.getInt(poolStart + 20);
        List<String> strings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int position = stringsStart + buffer.getInt(poolStart + headerSize + i * 4);
            if (utf8) {
                // length in chars, then length in bytes
                position += (buffer.get(position) & 0x80) != 0 ? 2 : 1;
                int length = buffer.get(position) & 0xFF;
                if ((length & 0x80) != 0) {
                    length = ((length & 0x7F) << 8) | (buffer.get(position + 1) & 0xFF);
                    position += 2;
                } else {
                    position += 1;
                }
                strings.add(new String(buffer.array(), position, length, StandardCharsets.UTF_8));
            } else {
                int length = buffer.getShort(position) & 0xFFFF;
                if ((length & 0x8000) != 0) {
                    length = ((length & 0x7FFF) << 16) | (buffer.getShort(position + 2) & 0xFFFF);
                    position += 4;
                } else {
                    position += 2;
                }
                strings.add(new String(buffer.array(), position, length * 2, StandardCharsets.UTF_16LE));
            }
        }
        return strings;
    }

    private static byte[] writeStrings(List<String> strings, boolean utf8, int flags) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] offsets = new int[strings.size()];
        for (int i = 0; i < strings.size(); i++) {
            offsets[i] = data.size();
            String value = strings.get(i);
            if (utf8) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                writeUtf8Length(data, value.length());
                writeUtf8Length(data, bytes.length);
                data.write(bytes, 0, bytes.length);
                data.write(0);
            } else {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_16LE);
                int length = value.length();
                if (length > 0x7FFF) {
                    int high = (length >>> 16) | 0x8000;
                    data.write(high & 0xFF);
                    data.write(high >>> 8);
                }
                data.write(length & 0xFF);
                data.write((length >>> 8) & 0xFF);
                data.write(bytes, 0, bytes.length);
                data.write(0);
                data.write(0);
            }
        }
        while (data.size() % 4 != 0) {
            data.write(0);
        }

        int headerSize = 28;
        int stringsStart = headerSize + offsets.length * 4;
        ByteBuffer out = ByteBuffer.allocate(stringsStart + data.size()).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) CHUNK_STRING_POOL);
        out.putShort((short) headerSize);
        out.putInt(out.capacity());
        out.putInt(offsets.length);
        out.putInt(0);
        // the strings are not sorted anymore
        out.putInt(flags & ~1);
        out.putInt(stringsStart);
        out.putInt(0);
        for (int offset : offsets) {
            out.putInt(offset);
        }
        out.put(data.toByteArray());
        return out.array();
    }

    private static void writeUtf8Length(ByteArrayOutputStream out, int length) {
        if (length > 0x7F) {
            out.write((length >>> 8) | 0x80);
        }
        out.write(length & 0xFF);
    }

    /** the name of a package in the resource table has a fixed size */
    static byte[] arsc(byte[] in, String oldPackage, String newPackage) {
        ByteBuffer buffer = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
        int position = buffer.getShort(2) & 0xFFFF;
        int renamed = 0;
        while (position < in.length) {
            int type = buffer.getShort(position) & 0xFFFF;
            int size = buffer.getInt(position + 4);
            if (type == CHUNK_TABLE_PACKAGE) {
                int nameStart = position + 12;
                String name = new String(in, nameStart, 256, StandardCharsets.UTF_16LE);
                name = name.substring(0, name.indexOf('\0'));
                if (name.equals(oldPackage)) {
                    if (newPackage.length() > 127) {
                        throw new IllegalArgumentException("package name is too long");
                    }
                    byte[] bytes = newPackage.getBytes(StandardCharsets.UTF_16LE);
                    for (int i = 0; i < 256; i++) {
                        in[nameStart + i] = i < bytes.length ? bytes[i] : 0;
                    }
                    renamed++;
                }
            }
            position += size;
        }
        System.out.println("arsc: " + renamed + " packages renamed");
        return in;
    }

    /** copies an apk, the entries keep their compression, run zipalign afterwards */
    private static void apk(String in, String out, String oldPackage, String newPackage) throws Exception {
        try (ZipFile zip = new ZipFile(in); ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(Paths.get(out)))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("META-INF/") && entry.getName().matches("META-INF/[^/]+\\.(SF|RSA|DSA|EC|MF)")) {
                    // the old signature, the apk gets signed again
                    continue;
                }
                byte[] bytes = readAll(zip.getInputStream(entry));
                if (entry.getName().equals("AndroidManifest.xml")) {
                    bytes = manifest(bytes, oldPackage, newPackage, null);
                } else if (entry.getName().equals("resources.arsc")) {
                    bytes = arsc(bytes, oldPackage, newPackage);
                }
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setMethod(entry.getMethod());
                copy.setTime(entry.getTime());
                if (entry.getMethod() == ZipEntry.STORED) {
                    CRC32 crc = new CRC32();
                    crc.update(bytes);
                    copy.setSize(bytes.length);
                    copy.setCompressedSize(bytes.length);
                    copy.setCrc(crc.getValue());
                }
                output.putNextEntry(copy);
                output.write(bytes);
                output.closeEntry();
            }
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[65536];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
