"""Generate isolated HTTPS tests of the actual ChatterinoHttp production adapter.

Only test VMs seed globals. DNS/socket routing never leaves loopback, while the
production adapter still sees/validates the exact real HTTPS URL and hostname.
"""
from pathlib import Path
import sys

PROBE = r'''package bttv.badge;
import com.bttv.badgehttp.okhttp3.*;
import com.sun.net.httpserver.*;
import javax.net.SocketFactory;import javax.net.ssl.*;
import java.io.*;import java.net.*;import java.nio.file.*;import java.security.*;import java.util.*;
import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class AdapterProbe {
 static final String API="https://api.chatterino.com/badges",PNG="https://fourtf.com/chatterino/badges/dev2x.png";
 static final AtomicInteger port=new AtomicInteger(),mode=new AtomicInteger(),requests=new AtomicInteger(),authCalls=new AtomicInteger(),gets=new AtomicInteger(),puts=new AtomicInteger();
 static final List<String> events=new CopyOnWriteArrayList<>();
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static OkHttpClient base()throws Exception{java.lang.reflect.Method m=ChatterinoHttp.class.getDeclaredMethod("newClient");m.setAccessible(true);return (OkHttpClient)m.invoke(null);}
 static SocketFactory sockets(){return new SocketFactory(){
  public Socket createSocket(){return new Socket(){public void connect(SocketAddress endpoint,int timeout)throws IOException{super.connect(new InetSocketAddress("127.0.0.1",port.get()),timeout);}};}
  public Socket createSocket(String h,int p)throws IOException{throw new IOException("fixture no implicit connect");}public Socket createSocket(String h,int p,InetAddress a,int n)throws IOException{throw new IOException();}public Socket createSocket(InetAddress a,int p)throws IOException{throw new IOException();}public Socket createSocket(InetAddress a,int p,InetAddress b,int n)throws IOException{throw new IOException();}
 };}
 static OkHttpClient client()throws Exception{return base().newBuilder().dns(h->Collections.singletonList(InetAddress.getByName("127.0.0.1"))).socketFactory(sockets()).connectTimeout(600,TimeUnit.MILLISECONDS).readTimeout(400,TimeUnit.MILLISECONDS).callTimeout(1800,TimeUnit.MILLISECONDS).build();}
 static void capture(String kind,Map<String,List<String>> headers){
  Set<String> names=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);names.addAll(headers.keySet());
  for(String n:names){String low=n.toLowerCase(Locale.ROOT);check(!Arrays.asList("cookie","cookie2","authorization","proxy-authorization").contains(low)&&!low.contains("oauth")&&!low.contains("account")&&!low.startsWith("x-twitch"),"private header on wire");}
  requests.incrementAndGet();events.add(kind+" cookie=false cookie2=false authorization=false proxy_authorization=false private_header=false");
 }
 interface IO {void run()throws Exception;}
 static void fails(IO f,Class<? extends Throwable> kind)throws Exception{try{f.run();throw new AssertionError("expected failure "+kind);}catch(Exception e){check(kind.isInstance(e),"unexpected category "+e.getClass());}}
 static void get(OkHttpClient c,String u,int n,String mime)throws IOException{ChatterinoHttp.get(c,u,n,mime);}
 static void close(OkHttpClient c){c.connectionPool().evictAll();check(c.dispatcher().runningCallsCount()==0,"calls leaked");c.dispatcher().executorService().shutdownNow();}
 public static void run(String key,String output,boolean untrusted)throws Exception{
  CookieHandler oldCookie=CookieHandler.getDefault();java.net.Authenticator oldAuth=java.net.Authenticator.getDefault();ProxySelector oldProxy=ProxySelector.getDefault();SSLContext oldTls=SSLContext.getDefault();
  KeyStore store=KeyStore.getInstance("JKS");try(InputStream in=Files.newInputStream(Paths.get(key))){store.load(in,"fixture-only".toCharArray());}
  KeyManagerFactory km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(store,"fixture-only".toCharArray());SSLContext ssl=SSLContext.getInstance("TLS");ssl.init(km.getKeyManagers(),null,null);
  ExecutorService serverWorkers=Executors.newFixedThreadPool(4,r->{Thread t=new Thread(r,"fixture-only-server");t.setDaemon(true);return t;});
  HttpsServer server=HttpsServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setHttpsConfigurator(new HttpsConfigurator(ssl));server.setExecutor(serverWorkers);
  server.createContext("/",e->{int scenario=mode.get();capture("case="+scenario,e.getRequestHeaders());int status=scenario==401?401:scenario==403?403:scenario==302?302:200;
   e.getResponseHeaders().set("Content-Type",e.getRequestURI().getPath().endsWith(".png")?"image/png":"application/json");
   e.getResponseHeaders().set("Set-Cookie","fixture_response_cookie=synthetic; Secure; Path=/");
   if(status==401)e.getResponseHeaders().set("WWW-Authenticate","Basic realm=\"fixture\"");
   if(status==302)e.getResponseHeaders().set("Location",PNG);
   byte[] body=scenario==900?new byte[4096]:new byte[]{123,125};
   if(scenario==901)try{Thread.sleep(1800);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
   try{e.sendResponseHeaders(status,scenario==902?0:body.length);e.getResponseBody().write(body);}catch(IOException closed){}finally{e.close();}
  });server.start();port.set(server.getAddress().getPort());
  CookieManager seeded=new CookieManager(null,CookiePolicy.ACCEPT_ALL){public Map<String,List<String>> get(URI u,Map<String,List<String>> h)throws IOException{gets.incrementAndGet();return super.get(u,h);}public void put(URI u,Map<String,List<String>> h)throws IOException{puts.incrementAndGet();super.put(u,h);}};
  for(String host:new String[]{"api.chatterino.com","fourtf.com"}){HttpCookie cookie=new HttpCookie("fixture_cookie","synthetic_only");cookie.setDomain(host);cookie.setSecure(true);cookie.setPath("/");seeded.getCookieStore().add(URI.create("https://"+host),cookie);}
  java.net.Authenticator synthetic=new java.net.Authenticator(){protected PasswordAuthentication getPasswordAuthentication(){authCalls.incrementAndGet();return new PasswordAuthentication("fixture-only","fixture-only".toCharArray());}};
  CookieHandler.setDefault(seeded);java.net.Authenticator.setDefault(synthetic);OkHttpClient c=null;List<String> passed=new ArrayList<>();
  try{
   c=client();final OkHttpClient use=c;
   check(c.cookieJar()==CookieJar.NO_COOKIES&&c.authenticator()==com.bttv.badgehttp.okhttp3.Authenticator.NONE&&c.proxyAuthenticator()==com.bttv.badgehttp.okhttp3.Authenticator.NONE,"cookie/auth settings");
   check(!c.followRedirects()&&!c.followSslRedirects()&&!c.retryOnConnectionFailure()&&c.cache()==null&&c.interceptors().isEmpty()&&c.networkInterceptors().isEmpty(),"independent settings");
   if(untrusted){fails(()->get(use,API,100,"application/json"),SSLHandshakeException.class);check(requests.get()==0,"untrusted reached HTTP");passed.add("untrusted_tls_rejected");}
   else{
    get(c,API,100,"application/json");passed.add("api_200");get(c,PNG,100,"image/png");passed.add("image_200");
    for(int code:new int[]{401,403,302}){mode.set(code);int previous=requests.get();try{get(c,API,100,"application/json");throw new AssertionError("status accepted");}catch(BadgeImages.HttpFailure expected){check(expected.status==code,"wrong status");}check(requests.get()==previous+1,"redirect/auth retry");passed.add("status_"+code+"_no_retry");}
    mode.set(0);for(int i=0;i<3;i++)get(c,API,100,"application/json");passed.add("set_cookie_repeat_ignored");
    ExecutorService parallel=Executors.newFixedThreadPool(3);try{List<Future<?>> futures=new ArrayList<>();for(int i=0;i<12;i++)futures.add(parallel.submit(()->{try{get(use,PNG,100,"image/png");}catch(IOException e){throw new UncheckedIOException(e);}}));for(Future<?> f:futures)f.get(5,TimeUnit.SECONDS);}finally{parallel.shutdownNow();check(parallel.awaitTermination(3,TimeUnit.SECONDS),"worker leak");}passed.add("concurrent_12");
    mode.set(900);fails(()->get(use,API,64,"application/json"),IOException.class);passed.add("content_length_cap");
    mode.set(902);fails(()->get(use,API,1,"application/json"),IOException.class);passed.add("streaming_byte_cap");
    mode.set(901);long begin=System.nanoTime();fails(()->get(use,API,100,"application/json"),IOException.class);check(System.nanoTime()-begin<TimeUnit.SECONDS.toNanos(3),"read timeout unbounded");passed.add("read_timeout");
    mode.set(0);
    KeyStore wrongKeys=KeyStore.getInstance("JKS");try(InputStream in=Files.newInputStream(Paths.get(key+".wrong"))){wrongKeys.load(in,"fixture-only".toCharArray());}
    KeyManagerFactory wrongKm=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());wrongKm.init(wrongKeys,"fixture-only".toCharArray());SSLContext wrongTls=SSLContext.getInstance("TLS");wrongTls.init(wrongKm.getKeyManagers(),null,null);
    HttpsServer wrongServer=HttpsServer.create(new InetSocketAddress("127.0.0.1",0),0);wrongServer.setHttpsConfigurator(new HttpsConfigurator(wrongTls));wrongServer.setExecutor(serverWorkers);wrongServer.createContext("/",e->{throw new AssertionError("mismatched hostname reached HTTP");});wrongServer.start();port.set(wrongServer.getAddress().getPort());
    try{fails(()->get(use,API,100,"application/json"),SSLPeerUnverifiedException.class);}finally{wrongServer.stop(0);port.set(server.getAddress().getPort());}passed.add("hostname_rejected");
    // The production URL is unchanged; this test socket alone routes TCP to a saturated backlog.
    try(ServerSocket blocked=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){List<Socket> fillers=new ArrayList<>();AtomicInteger tcp=new AtomicInteger(),tls=new AtomicInteger();try{
     boolean full=false;for(int i=0;i<64;i++){Socket s=new Socket();fillers.add(s);try{s.connect(new InetSocketAddress("127.0.0.1",blocked.getLocalPort()),100);}catch(SocketTimeoutException expected){full=true;break;}}check(full,"backlog not saturated");port.set(blocked.getLocalPort());
     OkHttpClient timed=c.newBuilder().eventListener(new com.bttv.badgehttp.okhttp3.EventListener(){public void connectStart(Call call,InetSocketAddress a,Proxy p){tcp.incrementAndGet();}public void secureConnectStart(Call call){tls.incrementAndGet();}}).build();try{fails(()->get(timed,API,100,"application/json"),SocketTimeoutException.class);check(tcp.get()>0&&tls.get()==0,"not TCP timeout");}finally{close(timed);}passed.add("connect_timeout_before_tls");
    }finally{for(Socket s:fillers)s.close();port.set(server.getAddress().getPort());}}
    try(ServerSocket proxy=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){port.set(proxy.getLocalPort());Future<?> worker=serverWorkers.submit(()->{try(Socket socket=proxy.accept()){socket.setSoTimeout(2000);BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream()));String first=r.readLine();check(first.startsWith("CONNECT api.chatterino.com:"),"proxy target");Map<String,List<String>> headers=new TreeMap<>();String line;while((line=r.readLine())!=null&&!line.isEmpty()){int at=line.indexOf(':');headers.put(line.substring(0,at),Collections.singletonList(line.substring(at+1)));}capture("proxy407",headers);socket.getOutputStream().write("HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=\"fixture\"\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));socket.getOutputStream().flush();}catch(IOException e){throw new UncheckedIOException(e);}});
     OkHttpClient proxied=c.newBuilder().proxy(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",proxy.getLocalPort()))).build();try{fails(()->get(proxied,API,100,"application/json"),IOException.class);worker.get(3,TimeUnit.SECONDS);}finally{close(proxied);}passed.add("proxy407_no_auth");
    }finally{port.set(server.getAddress().getPort());}
    int previous=requests.get();fails(()->get(use,"https://evil.invalid/badges",10,"application/json"),IOException.class);check(requests.get()==previous,"non-allowlisted request");passed.add("allowlist_rejected_before_wire");
    check("example.com".equals(HttpUrl.get("https://www.example.com").topPrivateDomain()),"private suffix resource");passed.add("private_resource");
   }
   check(gets.get()==0&&puts.get()==0&&authCalls.get()==0,"global cookie/auth used");check(seeded.getCookieStore().getCookies().size()==2,"cookie store modified");check(CookieHandler.getDefault()==seeded&&java.net.Authenticator.getDefault()==synthetic&&ProxySelector.getDefault()==oldProxy&&SSLContext.getDefault()==oldTls,"globals mutated");
   check(c.dispatcher().runningCallsCount()==0,"response not closed");c.connectionPool().evictAll();check(c.connectionPool().connectionCount()==0,"connections leaked");passed.add("closure_and_globals");
   Files.writeString(Paths.get(output),"PASS production ChatterinoHttp adapter; not ART/Samsung\ncases="+passed+"\nauth=0 cookie_get=0 cookie_put=0 cookie_store=unchanged\nrequests="+requests.get()+"\n"+String.join("\n",events)+"\n",StandardOpenOption.CREATE_NEW);
   System.out.println("PASS production adapter cases="+passed.size()+" requests="+requests.get());
  }finally{CookieHandler.setDefault(oldCookie);java.net.Authenticator.setDefault(oldAuth);server.stop(0);serverWorkers.shutdownNow();if(c!=null)close(c);}
 }
 public static void main(String[] a)throws Exception{run(a[0],a[1],a.length>2);}
}
'''


def generate(path):
    p=Path(path)/'bttv/badge/AdapterProbe.java'
    p.parent.mkdir(parents=True,exist_ok=True)
    if p.exists(): raise ValueError('Refusing fixture overwrite')
    p.write_text(PROBE,encoding='utf-8')


if __name__=='__main__': generate(sys.argv[1])
