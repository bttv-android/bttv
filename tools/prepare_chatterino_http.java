import java.io.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;import java.util.jar.*;
import org.objectweb.asm.*;import org.objectweb.asm.commons.*;import org.objectweb.asm.tree.*;
import kotlin.Metadata;import kotlin.metadata.jvm.*;
/** No shrinking. Fixed input/schema/output allowlist, not a general relocation tool.
 * Official Kotlin 2.2.21 parsed models are rewritten at BUILD time only. The fixed
 * metadata-tool hashes and final byte hash fail closed on any schema/output drift.
 * The supported runtime interface is Java, not Kotlin compiler/reflection APIs. */
public final class prepare_chatterino_http {
 static final Map<String,String> PINS=new LinkedHashMap<>();
 static {
  PINS.put("okhttp-4.12.0.jar","B1050081B14BB7A3A7E55A4D3EF01B5DCFABC453B4573A4FC019767191D5F4E0");
  PINS.put("okio-jvm-3.6.0.jar","67543F0736FC422AE927ED0E504B98BC5E269FDA0D3500579337CB713DA28412");
  PINS.put("kotlin-stdlib-1.9.10.jar","55E989C512B80907799F854309F3BC7782C5B3D13932442D0379D5C472711504");
  PINS.put("kotlin-stdlib-jdk7-1.9.10.jar","AC6361BF9AD1ED382C2103D9712C47CDEC166232B4903ED596E8876B0681C9B7");
  PINS.put("kotlin-stdlib-jdk8-1.9.10.jar","A4C74D94D64CE1ABE53760FE0389DD941F6FC558D0DAB35E47C085A11EC80F28");
  PINS.put("kotlin-stdlib-common-1.9.10.jar","CDE3341BA18A2BA262B0B7CF6C55B20C90E8D434E42C9A13E6A3F770DB965A88");
  PINS.put("annotations-13.0.jar","ACE2A10DC8E2D5FD34925ECAC03E4988B2C0F851650C94B8CEF49BA1BD111478");
 }
 static String hash(Path p)throws Exception {
  java.security.MessageDigest d=java.security.MessageDigest.getInstance("SHA-256");
  try(InputStream in=Files.newInputStream(p)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}
  StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format("%02X",b));return s.toString();
 }
 static void tool(Class<?> c,String expected)throws Exception {
  Path p=Paths.get(c.getProtectionDomain().getCodeSource().getLocation().toURI());
  if(!hash(p).equals(expected))throw new SecurityException("Unpinned build tool "+c.getName());
 }
 static void safe(String n,long size) {
  if(n.startsWith("/")||n.contains("\\")||n.contains("..")||n.contains(":")||n.indexOf('\0')>=0||size<0||size>4*1024*1024)
   throw new SecurityException("Unsafe archive entry");
  if(n.startsWith("META-INF/services/")||n.endsWith(".so"))throw new SecurityException("Unexpected runtime service/native entry");
 }
 static final String PREFIX="com/bttv/badgehttp/";static Set<String> classes=new TreeSet<>();
 static final String[] ROOTS={"okhttp3/","okio/","kotlin/","androidx/","org/jetbrains/annotations/","org/intellij/lang/annotations/"};
 static String name(String n){for(String p:ROOTS)if(n.startsWith(p))return PREFIX+n;return n;}
 static String metaName(String s){
  if(classes.contains(s)||classes.contains(s.replace('.','$')))return PREFIX+s;
  for(String n:classes)if(s.contains("L"+n+";"))s=s.replace("L"+n+";","L"+PREFIX+n+";");
  return s;
 }
 static Object walk(Object o,IdentityHashMap<Object,Object> seen)throws Exception{
  if(o==null)return null;if(o instanceof String)return metaName((String)o);
  if(seen.containsKey(o))return seen.get(o);seen.put(o,o);
  if(o instanceof List){List copy=new ArrayList();seen.put(o,copy);for(Object value:(List)o)copy.add(walk(value,seen));return copy;}
  if(o instanceof Map){Map copy=new LinkedHashMap();seen.put(o,copy);for(Object en:((Map)o).entrySet()){Map.Entry e=(Map.Entry)en;copy.put(walk(e.getKey(),seen),walk(e.getValue(),seen));}return copy;}
  if(!o.getClass().getName().startsWith("kotlin.metadata."))return o;
  // Fixed, hash-pinned official model schema. Never reflect over application/runtime objects.
  for(Class c=o.getClass();c!=null&&c.getName().startsWith("kotlin.metadata.");c=c.getSuperclass())
   for(Field f:c.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getType().isPrimitive()){
    f.setAccessible(true);Object old=f.get(o),value=walk(old,seen);if(value!=old)f.set(o,value);
   }
  return o;
 }
 static AnnotationNode node(Metadata m){
  AnnotationNode a=new AnnotationNode("Lkotlin/Metadata;");a.values=new ArrayList<>();
  Object[] kv={"k",m.k(),"mv",Arrays.stream(m.mv()).boxed().collect(java.util.stream.Collectors.toList()),"d1",Arrays.asList(m.d1()),"d2",Arrays.asList(m.d2()),"xs",m.xs(),"pn",m.pn(),"xi",m.xi()};
  a.values.addAll(Arrays.asList(kv));return a;
 }
 static final class Mapping extends Remapper{
  public String map(String s){return name(s);}
  public Object mapValue(Object v){
   if(v instanceof String){String s=(String)v;for(String p:ROOTS){if(s.startsWith(p))return PREFIX+s;if(s.startsWith(p.replace('/','.')))return PREFIX.replace('/','.')+s;}}
   return super.mapValue(v);
  }
 }
 public static void main(String[] args)throws Exception{
  if(args.length!=8)throw new IllegalArgumentException("output plus exact seven pinned inputs required");
  tool(KotlinClassMetadata.class,"E181F32A21A3CDF195F974004A42F0067C2BD9DEAA2A883636748D83FDEFD99D");
  tool(Metadata.class,"6558A3D233DA56A20934B32159F9DB5F86ED5816EF098F78A2C223DC6ABB79DD");
  tool(ClassReader.class,"B88EF66468B3C978AD0C97FD6E90979E56155B4AC69089BA7A44E9AA7FFE9ACF");
  tool(Remapper.class,"FED348EF05958E3E846A3AC074A12AF5F7936EF3D21CE44A62C4FA08A771927D");
  tool(ClassNode.class,"CFD7A0874F9DE36A999C127FEEADFBFE6E04D4A71EE954D7AF3D853F0BE48A6C");
  Path out=Paths.get(args[0]);List<Path> jars=new ArrayList<>();for(int i=1;i<args.length;i++)jars.add(Paths.get(args[i]));
  if(Files.exists(out)||Files.exists(out.resolveSibling(out.getFileName()+".inventory.tsv")))throw new IOException("Output already exists");
  int pin=0;for(Map.Entry<String,String> e:PINS.entrySet()) {
   Path p=jars.get(pin++);if(!p.getFileName().toString().equals(e.getKey())||!hash(p).equals(e.getValue()))throw new SecurityException("Unpinned runtime input");
   Set<String> names=new HashSet<>();try(JarFile z=new JarFile(p.toFile())) {
    long total=0;for(JarEntry item:Collections.list(z.entries())){
     safe(item.getName(),item.getSize());if(!names.add(item.getName()))throw new SecurityException("Duplicate input path");
     if((total+=item.getSize())>32*1024*1024)throw new SecurityException("Oversized archive");
     if(item.getName().endsWith(".class")&&!item.getName().endsWith("module-info.class")&&name(item.getName()).equals(item.getName()))throw new SecurityException("Unisolated class");
    }
   }
  }
  for(Path p:jars)try(JarFile z=new JarFile(p.toFile())){for(JarEntry e:Collections.list(z.entries()))if(e.getName().endsWith(".class")&&!e.getName().endsWith("module-info.class"))classes.add(e.getName().substring(0,e.getName().length()-6));}
  if(classes.size()!=1422)throw new SecurityException("Incomplete runtime closure");
  MetadataCheck.originals.addAll(classes);
  TreeMap<String,byte[]> entries=new TreeMap<>();int annotations=0,modules=0,descriptors=0;List<String> inventory=new ArrayList<>();
  for(Path p:jars)try(JarFile z=new JarFile(p.toFile())){
   for(JarEntry e:Collections.list(z.entries())){
    if(e.isDirectory())continue;String n=e.getName();byte[] b;try(InputStream in=z.getInputStream(e)){b=in.readAllBytes();}
    String target=name(n);
    if(n.endsWith(".class")&&!n.endsWith("module-info.class")){
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);
     if(c.visibleAnnotations!=null)for(int i=0;i<c.visibleAnnotations.size();i++){
      AnnotationNode a=c.visibleAnnotations.get(i);if(a.desc.equals("Lkotlin/Metadata;")){
       Metadata original=MetadataCheck.annotation(a);KotlinClassMetadata model=KotlinClassMetadata.readStrict(original);
       if(model==null)throw new IllegalStateException("Unrecognized metadata "+n);
       walk(model,new IdentityHashMap<>());Metadata updated=model.write();
       // Metadata packageName is optional, but a nonempty explicit original package must move.
       AnnotationNode replacement=node(updated);int pn=replacement.values.indexOf("pn");
       if(pn>=0&&!updated.pn().isEmpty())replacement.values.set(pn+1,PREFIX.replace('/','.')+updated.pn());
       c.visibleAnnotations.set(i,replacement);annotations++;
      }
     }
     ClassWriter w=new ClassWriter(0);c.accept(new ClassRemapper(w,new Mapping()));b=w.toByteArray();
    }else{
     if(n.endsWith(".kotlin_module")){
      KotlinModuleMetadata model=KotlinModuleMetadata.read(b);if(model==null)throw new IllegalStateException(n);
      walk(model,new IdentityHashMap<>());
      Map<String,kotlin.metadata.jvm.KmPackageParts> packages=model.getKmModule().getPackageParts();
      Map<String,kotlin.metadata.jvm.KmPackageParts> relocated=new LinkedHashMap<>();
      for(Map.Entry<String,kotlin.metadata.jvm.KmPackageParts> en:packages.entrySet())relocated.put(PREFIX.replace('/','.')+en.getKey(),en.getValue());
      packages.clear();packages.putAll(relocated);b=model.write();modules++;
     }
     if(n.startsWith("META-INF/"))target="META-INF/bttv-badgehttp-components/"+p.getFileName()+"/"+n.substring(9);
     // Retain JPMS provenance bytes, but never present inert descriptors as Android classes.
     if(n.endsWith("module-info.class")) { target += ".metadata"; descriptors++; }
    }
    if(entries.put(target,b)!=null)throw new IllegalStateException("duplicate "+target);
    inventory.add(p+"\t"+n+"\t"+target+"\t"+b.length);
   }
  }
  for(Map.Entry<String,byte[]> e:entries.entrySet())MetadataCheck.check(e.getKey(),e.getValue());
  if(annotations!=1347||modules!=6||descriptors!=3||MetadataCheck.parsed!=1347||MetadataCheck.modules!=6||!MetadataCheck.stale.isEmpty())throw new SecurityException("Metadata verification failed");
  if(entries.keySet().stream().anyMatch(n->n.endsWith("module-info.class")))throw new SecurityException("Executable JPMS descriptor");
  Path temp=Files.createTempFile(out.toAbsolutePath().getParent(),"chatterino-verified-",".jar");
  try {
   try(JarOutputStream z=new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(temp),131072))){for(Map.Entry<String,byte[]> e:entries.entrySet()){JarEntry n=new JarEntry(e.getKey());n.setTime(0);z.putNextEntry(n);z.write(e.getValue());z.closeEntry();}}
   if(!hash(temp).equals("A15D2FE8500753E35632714AA79FF5EE9A05F9878837D60AD545B0626410ADE1"))throw new SecurityException("Unrecognized output bytes");
   Files.move(temp,out);
  } finally { Files.deleteIfExists(temp); }
  Files.write(out.resolveSibling(out.getFileName()+".inventory.tsv"),inventory,StandardOpenOption.CREATE_NEW);
  System.out.println("classes="+classes.size()+" official_metadata_annotations_rewritten="+annotations+" official_modules_rewritten="+modules);
 }
}

/** Independent strict official-reader verification of rewritten annotations/modules. */
final class MetadataCheck {
 static Set<String> originals=new TreeSet<>(),stale=new TreeSet<>();
 static Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<>());
 static int parsed,modules;static String active;
 static void string(String s){
  if(originals.contains(s)||originals.contains(s.replace('.','$')))stale.add(active+" => "+s);
  for(String n:originals)if(s.contains("L"+n+";"))stale.add(active+" => "+s);
 }
 static void walk(Object o)throws Exception{
  if(o==null||!seen.add(o))return;
  if(o instanceof String){string((String)o);return;}
  if(o instanceof Iterable){for(Object a:(Iterable<?>)o)walk(a);return;}
  if(o instanceof Map){for(Object e:((Map<?,?>)o).entrySet()){walk(((Map.Entry<?,?>)e).getKey());walk(((Map.Entry<?,?>)e).getValue());}return;}
  if(o.getClass().isArray()){if(!o.getClass().getComponentType().isPrimitive())for(Object a:(Object[])o)walk(a);return;}
  if(!o.getClass().getName().startsWith("kotlin.metadata."))return;
  for(java.lang.reflect.Method m:o.getClass().getMethods())if(m.getName().startsWith("get")&&m.getParameterCount()==0&&!m.getName().equals("getClass")&&!m.getReturnType().isPrimitive())walk(m.invoke(o));
 }
 static Metadata annotation(AnnotationNode a){
  Map<String,Object> v=new HashMap<>();for(int i=0;i<a.values.size();i+=2)v.put((String)a.values.get(i),a.values.get(i+1));
  return (Metadata)Proxy.newProxyInstance(Metadata.class.getClassLoader(),new Class[]{Metadata.class},(p,m,args)->{
   if(m.getName().equals("annotationType"))return Metadata.class;
   Object x=v.get(m.getName());if(x==null)return m.getDefaultValue();
   if(m.getReturnType()==int[].class)return ((List<?>)x).stream().mapToInt(n->(Integer)n).toArray();
   if(m.getReturnType()==String[].class)return ((List<?>)x).toArray(new String[0]);return x;
  });
 }
 static void check(String name,byte[] b)throws Exception {
  active=name;if(name.endsWith("module-info.class"))return;
  if(name.endsWith(".class")){
   ClassNode c=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   if(c.visibleAnnotations!=null)for(AnnotationNode a:c.visibleAnnotations)if(a.desc.equals("Lcom/bttv/badgehttp/kotlin/Metadata;")){
    KotlinClassMetadata md=KotlinClassMetadata.readStrict(annotation(a));if(md==null)throw new SecurityException("Unparsed metadata");parsed++;walk(md);
   }
  }else if(name.endsWith(".kotlin_module")){modules++;walk(KotlinModuleMetadata.read(b));}
 }
}
