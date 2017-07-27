
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.HttpNfcLease;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualApp;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * Exports VMDK(s) and OVF Descriptor for a VM or a vApp.
 */

public class ExportOvfToLocal 
{
   public static LeaseProgressUpdater leaseProgUpdater;

   public static void main(String[] args) throws Exception
    {
      if (args.length != 7) 
      {
        System.out.println("java ExportOvfToLocal <SdkUrl> <username> <password> <VappOrVmName> <hostip> <VirtualMachine|VirtualApp> <localDir>");
        System.out.println("java ExportOvfToLocal https://10.20.152.74/sdk root password NewVM1 10.20.152.74 VirtualMachine C:\\Temp\\ovf\\");
        return;
      }
      
      ServiceInstance si = new ServiceInstance(new URL(args[0]), args[1], args[2], true);

      String vAppOrVmName = args[3];
      String hostip = args[4];
      String entityType = args[5];
      String targetDir = args[6];

      HostSystem host = (HostSystem) si.getSearchIndex().findByIp(null, hostip, false); 
        
      System.out.println("Host Name : " + host.getName());
      System.out.println("Network : " + host.getNetworks()[0].getName());
      System.out.println("Datastore : " + host.getDatastores()[0].getName());

      InventoryNavigator iv = new InventoryNavigator(si.getRootFolder());
      
      HttpNfcLease hnLease = null;
      
      ManagedEntity me = null;
      if (entityType.equals("VirtualApp"))
      {
        me = iv.searchManagedEntity("VirtualApp", vAppOrVmName);
        hnLease = ((VirtualApp)me).exportVApp();
      }
      else
      {
        me = iv.searchManagedEntity("VirtualMachine", vAppOrVmName);
        try {
          hnLease = ((VirtualMachine)me).exportVm();
        } catch (FileFault e) {
          System.out.println("找不到文件:" + e.getLocalizedMessage());
        } catch (InvalidPowerState e) {
          System.out.println("虚拟机状态:" + e.getLocalizedMessage());
        } catch (InvalidState e) {
          System.out.println("状态不对:" + e.getLocalizedMessage());
        } catch (RuntimeFault e)  {
          System.out.println("运行错误:" + e.getLocalizedMessage());
        } catch (TaskInProgress e) {
          System.out.println("虚拟机正在忙:" + e.getLocalizedMessage());
        }
      }
        
      // Wait until the HttpNfcLeaseState is ready
      HttpNfcLeaseState hls;
      for(;;)
      {
        hls = hnLease.getState();
        if(hls == HttpNfcLeaseState.ready)
        {
          break;
        }
        if(hls == HttpNfcLeaseState.error)
        {
          si.getServerConnection().logout();
          return;
        }
      }
      
      System.out.println("HttpNfcLeaseState: ready ");
      HttpNfcLeaseInfo httpNfcLeaseInfo = hnLease.getInfo();
      httpNfcLeaseInfo.setLeaseTimeout(300*1000*1000);
      printHttpNfcLeaseInfo(httpNfcLeaseInfo);

      //Note: the diskCapacityInByte could be many time bigger than
      //the total size of VMDK files downloaded. 
      //As a result, the progress calculated could be much less than reality.
      long diskCapacityInByte = (httpNfcLeaseInfo.getTotalDiskCapacityInKB()) * 1024;

      leaseProgUpdater = new LeaseProgressUpdater(hnLease, 5000);
      leaseProgUpdater.start();

      long alredyWrittenBytes = 0;
      HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();
      if (deviceUrls != null) 
      {
        OvfFile[] ovfFiles = new OvfFile[deviceUrls.length];
        System.out.println("Downloading Files:");
        for (int i = 0; i < deviceUrls.length; i++) 
        {
          String deviceId = deviceUrls[i].getKey();
          String deviceUrlStr = deviceUrls[i].getUrl();
          String diskFileName = deviceUrlStr.substring(deviceUrlStr.lastIndexOf("/") + 1);
          String diskUrlStr = deviceUrlStr.replace("*", hostip);
          String diskLocalPath = targetDir + diskFileName;
          System.out.println("File Name: " + diskFileName);
          System.out.println("VMDK URL: " + diskUrlStr);
          String cookie = si.getServerConnection().getVimService().getWsc().getCookie();
          long lengthOfDiskFile = writeVMDKFile(diskLocalPath, diskUrlStr, cookie, alredyWrittenBytes, diskCapacityInByte);
          alredyWrittenBytes += lengthOfDiskFile;
          OvfFile ovfFile = new OvfFile();
          ovfFile.setPath(diskFileName);
          ovfFile.setDeviceId(deviceId);
          ovfFile.setSize(lengthOfDiskFile);
          ovfFiles[i] = ovfFile;
        }
        
        OvfCreateDescriptorParams ovfDescParams = new OvfCreateDescriptorParams();
        ovfDescParams.setOvfFiles(ovfFiles);
        OvfCreateDescriptorResult ovfCreateDescriptorResult = 
          si.getOvfManager().createDescriptor(me, ovfDescParams);

        String ovfPath = targetDir + vAppOrVmName + ".ovf";
        FileWriter out = new FileWriter(ovfPath);
        out.write(ovfCreateDescriptorResult.getOvfDescriptor());
        out.close();
        System.out.println("OVF Desriptor Written to file: " + ovfPath);
      } 
      
      System.out.println("Completed Downloading the files");
      leaseProgUpdater.interrupt();
      hnLease.httpNfcLeaseProgress(100);
      hnLease.httpNfcLeaseComplete();

      si.getServerConnection().logout();
    }
  
  
  private static void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info) 
  {
    System.out.println("########################  HttpNfcLeaseInfo  ###########################");
    System.out.println("Lease Timeout: " + info.getLeaseTimeout());
    System.out.println("Total Disk capacity: "  + info.getTotalDiskCapacityInKB());
    HttpNfcLeaseDeviceUrl[] deviceUrlArr = info.getDeviceUrl();
    if (deviceUrlArr != null) 
    {
      int deviceUrlCount = 1;
      for (HttpNfcLeaseDeviceUrl durl : deviceUrlArr) 
      {
        System.out.println("HttpNfcLeaseDeviceUrl : "
            + deviceUrlCount++);
        System.out.println("  Device URL Import Key: "
            + durl.getImportKey());
        System.out.println("  Device URL Key: " + durl.getKey());
        System.out.println("  Device URL : " + durl.getUrl());
        System.out.println("  SSL Thumbprint : "  + durl.getSslThumbprint());
      }
    } 
    else
    {
      System.out.println("No Device URLS Found");
    }
  }

  private static long writeVMDKFile(String localFilePath, String diskUrl, String cookie, 
      long bytesAlreadyWritten, long totalBytes) throws IOException 
  {
    HttpsURLConnection conn = getHTTPConnection(diskUrl, cookie);
    InputStream in = conn.getInputStream();
    OutputStream out = new FileOutputStream(new File(localFilePath));
    byte[] buf = new byte[102400];
    int len = 0;
    long bytesWritten = 0;
    while ((len = in.read(buf)) > 0) 
    {
      out.write(buf, 0, len);
      bytesWritten += len;
      int percent = (int)(((bytesAlreadyWritten + bytesWritten) * 100) / totalBytes);
      leaseProgUpdater.setPercent(percent);
      System.out.println("written: " + bytesWritten + "; percent : " + percent);
    }
    in.close();
    out.close();
    return bytesWritten;
  }

  private static HttpsURLConnection getHTTPConnection(String urlStr, String cookieStr) throws IOException 
  {
    trustAllCert();
    HostnameVerifier hv = new HostnameVerifier() 
    {
      public boolean verify(String urlHostName, SSLSession session) 
      {
        return true;
      }
    };
    HttpsURLConnection.setDefaultHostnameVerifier(hv);
    URL url = new URL(urlStr);
    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setAllowUserInteraction(true);
    conn.setRequestProperty("Cookie",  cookieStr);
    conn.connect();
    return conn;
  }

  private static class TrustAllManager implements X509TrustManager {
    private TrustAllManager() {
    }

    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
    }
  }

  private static void trustAllHttpsCertificates() throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustAllCerts = new TrustManager[]{new TrustAllManager()};
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init((KeyManager[])null, trustAllCerts, (SecureRandom)null);
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }

  // solve ssl Auth.
  public static void trustAllCert() {
    try {
      trustAllHttpsCertificates();
      HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
        public boolean verify(String urlHostName, SSLSession session) {
          return true;
        }
      });
    } catch (Exception var1) {
      System.out.println("trustAllCert error: " +  var1.getLocalizedMessage());;
    }
  }

}
