import brut.androlib.apk.ApkInfo;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;

/** Exact manifest export, preserving existing Apktool unknownFiles and file bytes. */
class export_chatterino_resources {
    static String sha(byte[] b) throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte v : MessageDigest.getInstance("SHA-256").digest(b)) out.append(String.format("%02X", v));
        return out.toString();
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IOException("jar, decoded, pinned hash, exact TSV manifest required");
        Path jar = Paths.get(args[0]), root = Paths.get(args[1]), yml = root.resolve("apktool.yml");
        if (!sha(Files.readAllBytes(jar)).equals(args[2])) throw new IOException("Private artifact hash mismatch");
        Map<String,String> resources = new TreeMap<>();
        for (String line : Files.readAllLines(Paths.get(args[3]))) {
            String[] item = line.split("\t", -1);
            if (item.length != 2 || !item[1].matches("[A-F0-9]{64}")
                    || !item[0].matches("(com/bttv/badgehttp/|META-INF/bttv-badgehttp-components/)[A-Za-z0-9_./$-]+")
                    || item[0].contains("..") || resources.put(item[0],item[1]) != null)
                throw new IOException("Unsafe/duplicate resource manifest entry");
        }
        if (resources.size() != 386) throw new IOException("Incomplete resource manifest");
        ApkInfo info;
        try (InputStream input = Files.newInputStream(yml)) { info = ApkInfo.load(input); }
        Map<String,String> previous = new TreeMap<>();
        if (info.unknownFiles != null) previous.putAll(info.unknownFiles);
        Map<String,byte[]> payload = new TreeMap<>();
        try (JarFile input = new JarFile(jar.toFile())) {
            Set<String> all = new HashSet<>(), expected = new TreeSet<>();
            for (JarEntry e : Collections.list(input.entries())) {
                if (!all.add(e.getName())) throw new IOException("Duplicate artifact entry");
                if (e.getName().endsWith("module-info.class")) throw new IOException("Executable JPMS descriptor");
                if (!e.getName().endsWith(".class")) expected.add(e.getName());
            }
            if (!resources.keySet().equals(expected)) throw new IOException("Resource inventory mismatch");
            for (Map.Entry<String,String> item : resources.entrySet()) {
                Path target = root.resolve("unknown").resolve(item.getKey());
                if (previous.containsKey(item.getKey()) || Files.exists(target)) throw new IOException("Existing resource collision");
                byte[] data;
                try (InputStream in = input.getInputStream(input.getJarEntry(item.getKey()))) { data = in.readAllBytes(); }
                if (!sha(data).equals(item.getValue())) throw new IOException("Resource byte mismatch");
                payload.put(item.getKey(),data);
            }
        }
        String original = Files.readString(yml);
        List<String> lines = new ArrayList<>(Arrays.asList(original.split("\n", -1)));
        int start = -1, end;
        for (int i=0;i<lines.size();i++) if (lines.get(i).startsWith("unknownFiles:")) {
            if (start != -1) throw new IOException("Duplicate YAML section"); start=i;
        }
        if (start < 0 || !(lines.get(start).trim().equals("unknownFiles:") || lines.get(start).trim().equals("unknownFiles: {}")))
            throw new IOException("Unsupported unknownFiles mapping");
        end=start+1;
        while (end<lines.size() && (lines.get(end).trim().isEmpty() || Character.isWhitespace(lines.get(end).charAt(0)))) end++;
        List<String> addition = new ArrayList<>();
        for (String name:resources.keySet()) addition.add("  '"+name+"': '8'");
        lines.set(start,"unknownFiles:"); lines.addAll(end,addition);
        String revised=String.join("\n",lines);
        ApkInfo check=ApkInfo.load(new ByteArrayInputStream(revised.getBytes(StandardCharsets.UTF_8)));
        Map<String,String> expected=new TreeMap<>(previous);
        for(String name:resources.keySet()) expected.put(name,"8");
        if(!expected.equals(check.unknownFiles)) throw new IOException("UnknownFiles preservation failed");
        for(Map.Entry<String,byte[]> item:payload.entrySet()) {
            Path target=root.resolve("unknown").resolve(item.getKey());Files.createDirectories(target.getParent());
            Files.write(target,item.getValue(),StandardOpenOption.CREATE_NEW);
        }
        Files.writeString(yml,revised);
        System.out.println("PASS private resources="+resources.size()+" existing unknownFiles="+previous.size()+" preserved");
    }
}
