package bttv.updater;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import com.android.apksig.ApkVerifier;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Full SDK apksig byte verification PLUS package metadata. Android remains final install authority. */
final class UpdateVerifier {
    static UpdateRelease.Installed installed(Context context) throws Exception {
        UpdateRelease.require(UpdateRelease.PACKAGE.equals(context.getPackageName()),"Unexpected installed package");
        PackageInfo p=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        Signature[] signers=p.signingInfo==null?null:p.signingInfo.getApkContentsSigners();
        UpdateRelease.require(signers!=null && signers.length==1,"Installed signing identity is unavailable");
        String signer=UpdateTransfer.hex(MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray()));
        return new UpdateRelease.Installed(p.versionName,p.getLongVersionCode(),signer,Build.VERSION.SDK_INT,Arrays.asList(Build.SUPPORTED_ABIS));
    }

    static void verify(Context context,UpdateRelease release,File apk,UpdateTransfer.Cancel cancel) throws Exception {
        cancel.check();
        UpdateRelease.require(apk.isFile() && apk.length()==release.size,"APK size is invalid");
        UpdateRelease.require(UpdateTransfer.digest(apk,cancel).equals(release.sha256),"APK SHA-256 mismatch");
        UpdateRelease.Installed installed=installed(context);
        UpdateRelease.require(release.code>installed.code && release.signer.equals(installed.signer),"Update identity changed");
        ApkVerifier.Result signature=new ApkVerifier.Builder(apk).setMinCheckedPlatformVersion(29).build().verify();
        cancel.check();
        UpdateRelease.require(signature.isVerified() && signature.getSignerCertificates().size()==1,"APK cryptographic signature verification failed");
        String signer=UpdateTransfer.hex(MessageDigest.getInstance("SHA-256").digest(signature.getSignerCertificates().get(0).getEncoded()));
        UpdateRelease.require(signer.equals(installed.signer) && signer.equals(release.signer),"APK signer does not match this installation");
        PackageInfo p=context.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
        UpdateRelease.require(p!=null && UpdateRelease.PACKAGE.equals(p.packageName),"APK package is invalid");
        UpdateRelease.require(p.getLongVersionCode()==release.code && p.getLongVersionCode()>installed.code,"APK is not the expected newer version");
        UpdateRelease.require(release.name.equals(p.versionName) && release.tag.equals("v"+p.versionName),"APK versionName differs from release");
        UpdateRelease.require(p.applicationInfo!=null && p.applicationInfo.minSdkVersion==release.minSdk && release.minSdk<=installed.sdk,"APK SDK metadata mismatch");
        UpdateRelease.require(p.splitNames==null || p.splitNames.length==0,"Split APK updates are unsupported");
        nativeAndZip(apk,new File(context.getApplicationInfo().sourceDir),release.abis,cancel);
    }

    static void nativeAndZip(File apk,File installed,List<String> abis,UpdateTransfer.Cancel cancel) throws Exception {
        try(ZipFile z=new ZipFile(apk); ZipFile baseline=new ZipFile(installed)) {
            Set<String> names=new HashSet<>(); Set<String> expected=new HashSet<>();
            Enumeration<? extends ZipEntry> before=baseline.entries();
            while(before.hasMoreElements()) { String name=before.nextElement().getName(); for(String abi:abis)if(name.startsWith("lib/"+abi+"/") && name.endsWith(".so"))expected.add(name); }
            long expanded=0; int count=0;
            Enumeration<? extends ZipEntry> entries=z.entries();
            while(entries.hasMoreElements()) {
                cancel.check(); ZipEntry e=entries.nextElement(); String name=e.getName();
                UpdateRelease.require(++count<=100000 && names.add(name) && !name.startsWith("/") && !Arrays.asList(name.split("/")).contains(".."),"Unsafe or duplicate ZIP entry");
                if(e.isDirectory())continue;
                UpdateRelease.require(e.getSize()>=0 && e.getSize()<=UpdateRelease.MAX_APK,"Invalid ZIP entry size");
                expanded+=e.getSize(); UpdateRelease.require(expanded<=2147483648L,"Expanded APK is too large");
                for(String abi:abis) if(name.startsWith("lib/"+abi+"/") && name.endsWith(".so")) {
                    byte[] elf=new byte[20];
                    try(DataInputStream input=new DataInputStream(z.getInputStream(e))) { input.readFully(elf); }
                    int machine=(elf[18]&255)|((elf[19]&255)<<8);
                    UpdateRelease.require(elf[0]==127 && elf[1]=='E' && elf[2]=='L' && elf[3]=='F' && elf[5]==1
                            && (abi.equals("arm64-v8a")?elf[4]==2 && machine==183:elf[4]==1 && machine==40),"Invalid ARM native library");
                }
            }
            UpdateRelease.require(names.contains("AndroidManifest.xml") && names.contains("resources.arsc") && names.contains("classes.dex"),"Incomplete APK container");
            for(String abi:abis) {
                UpdateRelease.require(names.contains("lib/"+abi+"/libglide-webp.so"),"Missing ARM decoder");
                UpdateRelease.require(expected.stream().anyMatch(n->n.startsWith("lib/"+abi+"/")),"Installed ABI baseline is unavailable");
            }
            UpdateRelease.require(names.containsAll(expected),"APK drops required installed ARM libraries");
        }
    }
}
