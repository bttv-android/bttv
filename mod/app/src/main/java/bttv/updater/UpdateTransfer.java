package bttv.updater;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/** Bounded streaming HTTPS only. No Activity, worker service, authentication or shared storage. */
final class UpdateTransfer {
    interface Connections { HttpURLConnection open(URL url) throws IOException; }
    interface Progress { void update(long bytes,long total); }
    static final Connections HTTPS = url -> (HttpURLConnection)url.openConnection();

    static final class Cancel {
        private boolean cancelled;
        private HttpURLConnection connection;
        synchronized void check() throws IOException { UpdateRelease.require(!cancelled,"Cancelled"); }
        synchronized void bind(HttpURLConnection value) throws IOException {
            check(); connection=value;
        }
        synchronized void unbind() { connection=null; }
        synchronized void cancel() {
            cancelled=true;
            if(connection!=null) connection.disconnect();
        }
    }

    static final class HttpFailure extends IOException {
        final long retryAt;
        HttpFailure(int status,long retryAt) { super("Official release server returned HTTP "+status); this.retryAt=retryAt; }
    }

    static HttpURLConnection open(String source,boolean metadata,Cancel cancel,Connections factory) throws IOException {
        String current=source;
        for(int hop=0;hop<6;hop++) {
            cancel.check();
            UpdateRelease.require(allowed(current,metadata),"Unexpected HTTPS redirect");
            HttpURLConnection c=factory.open(new URL(current));
            cancel.bind(c);
            c.setInstanceFollowRedirects(false); c.setConnectTimeout(15000); c.setReadTimeout(20000);
            c.setUseCaches(false); c.setRequestProperty("User-Agent","bttv-android-manual-updater");
            c.setRequestProperty("Accept",metadata?"application/vnd.github+json":UpdateRelease.MIME);
            c.setRequestProperty("Accept-Encoding","identity");
            int status;
            try { status=c.getResponseCode(); }
            catch(IOException e) { c.disconnect(); cancel.unbind(); throw e; }
            if(status==301 || status==302 || status==303 || status==307 || status==308) {
                String location=c.getHeaderField("Location"); c.disconnect(); cancel.unbind();
                UpdateRelease.require(location!=null,"Missing redirect destination");
                current=new URL(new URL(current),location).toExternalForm(); continue;
            }
            if(status!=200) {
                long retry=retryAt(c,System.currentTimeMillis()); c.disconnect(); cancel.unbind();
                throw new HttpFailure(status,retry);
            }
            return c;
        }
        throw new IOException("Too many HTTPS redirects");
    }

    static boolean allowed(String value,boolean metadata) {
        try {
            URI u=new URI(value);
            if(!"https".equals(u.getScheme()) || u.getUserInfo()!=null || u.getPort()!=-1 || u.getFragment()!=null) return false;
            if(metadata) return UpdateRelease.API.equals(value);
            if("github.com".equals(u.getHost())) return u.getRawPath().startsWith("/bttv-android/bttv/releases/download/") && u.getQuery()==null;
            return "release-assets.githubusercontent.com".equals(u.getHost()) || "objects.githubusercontent.com".equals(u.getHost());
        } catch(Exception e) { return false; }
    }

    static String metadata(Cancel cancel,Connections factory) throws IOException {
        HttpURLConnection c=open(UpdateRelease.API,true,cancel,factory);
        try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            long length=c.getContentLengthLong(); UpdateRelease.require(length<=UpdateRelease.MAX_METADATA,"Metadata is too large");
            byte[] buffer=new byte[8192]; int n,total=0;
            while((n=in.read(buffer))!=-1) {
                cancel.check(); total+=n; UpdateRelease.require(total<=UpdateRelease.MAX_METADATA,"Metadata is too large"); out.write(buffer,0,n);
            }
            UpdateRelease.require(length<0 || length==total,"Incomplete metadata response");
            return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
        } finally { c.disconnect(); cancel.unbind(); }
    }

    static File download(UpdateRelease release,File directory,Cancel cancel,Progress progress,Connections factory) throws Exception {
        UpdateRelease.require(directory.isDirectory() || directory.mkdirs(),"Cannot create updater cache");
        UpdateRelease.require(directory.getUsableSpace()>release.size*2+16777216,"Insufficient storage for update");
        String identity=release.assetId+"-"+release.sha256;
        File part=new File(directory,identity+".part"), complete=new File(directory,identity+".apk");
        // A previous file never bypasses transfer/verification. Only updater-owned names are touched.
        UpdateRelease.require(!part.exists() || part.delete(),"Cannot remove stale partial update");
        UpdateRelease.require(!complete.exists() || complete.delete(),"Cannot replace stale update");
        boolean success=false; HttpURLConnection c=null;
        try {
            c=open(release.url,false,cancel,factory);
            long length=c.getContentLengthLong();
            UpdateRelease.require(length<0 || length==release.size,"Download size differs from release");
            MessageDigest digest=MessageDigest.getInstance("SHA-256"); long total=0;
            try(InputStream in=c.getInputStream(); FileOutputStream out=new FileOutputStream(part)) {
                byte[] buffer=new byte[32768]; int n;
                while((n=in.read(buffer))!=-1) {
                    cancel.check(); total+=n;
                    UpdateRelease.require(total<=release.size && total<=UpdateRelease.MAX_APK,"Update exceeds expected size");
                    out.write(buffer,0,n); digest.update(buffer,0,n); progress.update(total,release.size);
                }
                out.getFD().sync();
            }
            cancel.check();
            UpdateRelease.require(total==release.size,"Incomplete APK download");
            UpdateRelease.require(release.sha256.equals(hex(digest.digest())),"APK SHA-256 mismatch");
            UpdateRelease.require(part.renameTo(complete),"Cannot finalize update");
            cancel.check(); success=true; return complete;
        } finally {
            if(c!=null)c.disconnect(); cancel.unbind();
            if(!success) { if(part.exists() && !part.delete()) part.deleteOnExit(); if(complete.exists() && !complete.delete()) complete.deleteOnExit(); }
        }
    }

    static String digest(File file,Cancel cancel) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)) {
            byte[] buffer=new byte[32768]; int n;
            while((n=in.read(buffer))!=-1) { cancel.check(); digest.update(buffer,0,n); }
        }
        return hex(digest.digest());
    }
    static String hex(byte[] bytes) {
        StringBuilder s=new StringBuilder(bytes.length*2);
        for(byte b:bytes) s.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));
        return s.toString().toUpperCase(Locale.ROOT);
    }
    static long retryAt(HttpURLConnection c,long now) {
        long retry=now+60000;
        String value=c.getHeaderField("Retry-After"), reset=c.getHeaderField("X-RateLimit-Reset");
        try {
            if(value!=null) {
                if(value.matches("[0-9]{1,10}")) retry=Math.max(retry,now+Long.parseLong(value)*1000);
                else { SimpleDateFormat format=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US); retry=Math.max(retry,format.parse(value).getTime()); }
            }
            if("0".equals(c.getHeaderField("X-RateLimit-Remaining")) && reset!=null && reset.matches("[0-9]{1,11}"))
                retry=Math.max(retry,Long.parseLong(reset)*1000);
        } catch(Exception ignored) { /* Conservative default backoff; never display unfiltered server text. */ }
        return retry;
    }
}
