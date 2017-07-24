package com.sunment.cloud.vmware.folder;

import com.sunment.cloud.util.ObjectUtil;
import com.vmware.vim25.SessionManagerGenericServiceTicket;
import com.vmware.vim25.SessionManagerHttpServiceRequestSpec;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.SessionManager;
import net.dongliu.requests.Requests;

import javax.net.ssl.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class FolderTest {
	// vcIp,username and password of vCenter
	private static String vcIp = "192.168.20.64";//"192.168.6.199";
	private static String userName = "administrator@vsphere.local";
	private static String password = "2wsx@WSX"; //"P@ssw0rd";

	// main
	public static void main(String[] args) throws Exception{
		httpAccessToHostFile();
	}

	// get ServiceInstance of vc
	public static ServiceInstance getInitSi() throws Exception {
		ServiceInstance si  = new ServiceInstance(new URL("https://" + vcIp + "/sdk"), userName, password, true);
		return si;
	}

	// main class of datastore file operation
	public static void httpAccessToHostFile() throws Exception {
		// ip of host in which file datastore is
		String hostIp = "192.168.20.71";//"192.168.6.3";//"192.168.6.2";

		// source file of downloading
		String destinationFile = "刘畅测试机器（勿删）/刘畅测试机器（勿删）_1-000001-delta.vmdk";//"testLzy1";

		// destination file of uploading
		String destinationFilePath = "asdfasdf/刘畅测试机器（勿删）_1-000001-delta.vmdk"; // "刘畅测试机器（勿删）/刘畅测试机器（勿删）_1-000001-delta.vmdk";//"A计算机/testZip.zip";//"testLzy1";

		// datastore name in which the file is
		String dsName = "datastore1 (1)";//"6.3本地SAS";//"testLzy";//"6.2本地SSD";

		// source file of uploading
		String sourceFilePath = "/home/zhenmie/Documents/vmware/ds/file/刘畅测试机器（勿删）_1-000001-delta.vmdk";//"/home/zhenmie/Downloads/wine-qqintl.zip";//"/home/zhenmie/Documents/vmware/ds/file/efficent-upload-files-to-datastore.pl";

		// no needed
		String dcName = "新建数据中心 1";

		// destination file of  downloading
		String downloadDestFile = "/home/zhenmie/Documents/vmware/ds/file/刘畅测试机器（勿删）_1-000001-delta.vmdk";

		ServiceInstance si = getInitSi();
		//String sessionCookie = si.getServerConnection().getSessionStr();

		// create url
		String putUrl = "https://" + hostIp + "/folder/" + destinationFilePath +
				"?dcPath=" + "ha-datacenter" + "&dsName=" + dsName;

		String getUrl = "https://" + hostIp + "/folder/" + destinationFile +
				"?dcPath=" + "ha-datacenter" + "&dsName=" + dsName;

		// get ticket from url
		SessionManagerHttpServiceRequestSpec spec = createPutSpec(si, putUrl);//createGetSpec(si, getUrl);
		SessionManager sessionManager = si.getSessionManager();
		SessionManagerGenericServiceTicket ticket = sessionManager.acquireGenericServiceTicket(spec);
		System.out.println("id:" + ticket.getId() + "; hostName:" + ticket.getHostName() +
				"; sslThumbprint:" + ticket.getSslThumbprint());

		// no need
		//doHttpPostFromPython(sessionCookie, dcName, dsName, sourceFilePath, destinationFile);

		// upload
		doHttpPut(si, putUrl, sourceFilePath, ticket);

		// download
		//doHttpGet(si, getUrl, downloadDestFile, ticket);
	}

	/**
	 * download file from datastore of url folder, to fiel of downloadDestFile, using ticket
	 * @param si	no needed
	 * @param getUrl
	 * @param downloadDestFile
     * @param ticket
     */
	private static void doHttpGet(ServiceInstance si, String getUrl, String downloadDestFile, SessionManagerGenericServiceTicket ticket) {
		getUrl = getUrl.replaceAll("\\ ", "%20");

		HttpsURLConnection conn = null;
		BufferedInputStream bis = null;
		OutputStream os = null;
		try {
			URL fileURL = new URL(getUrl);

			trustAllCert();

			conn = (HttpsURLConnection) fileURL.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setAllowUserInteraction(true);

			String headTicket = "vmware_cgi_ticket=" + ticket.getId();
			conn.setRequestProperty("Cookie", headTicket);

			bis = new BufferedInputStream(conn.getInputStream());

			File downloadFile = new File(downloadDestFile);

			os = new FileOutputStream(downloadFile);

			int size = 0;
			int len = 0;
			byte[] buf = new byte[1024];

			while((size = bis.read(buf)) != -1) {
				len += size;
				os.write(buf, 0, size);
			}
			System.out.println("下载文件长度:" + len);
		} catch (MalformedURLException e) {
			System.out.println("url链接错误:" + e.getLocalizedMessage());
		} catch (IOException e) {
			System.out.println("io链接错误:" + e.getLocalizedMessage());
		} finally {
			try {
				if(ObjectUtil.isNotNullObject(bis)) {
					bis.close();
				}
				if(ObjectUtil.isNotNullObject(os)) {
					os.close();
				}
			} catch (IOException e) {
				System.out.println("流关闭出错:" + e.getLocalizedMessage());
			}

			if(ObjectUtil.isNotNullObject(conn)) {
				conn.disconnect();
			}
		}
	}

	/**
	 * upload file to datastore of url folder, from file of sourceFilePath, using ticket.
	 * @param si
	 * @param url
	 * @param sourceFilePath
	 * @param ticket
     * @throws Exception
     */
	private static void doHttpPut(ServiceInstance si, String url,
								  String sourceFilePath, SessionManagerGenericServiceTicket ticket) throws  Exception{
		url = url.replaceAll("\\ ", "%20");

		HttpsURLConnection conn = null;
		try {
			URL fileURL = new URL(url);

			trustAllCert();

			conn = (HttpsURLConnection) fileURL.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setAllowUserInteraction(true);
		} catch (MalformedURLException e) {
			System.out.println("url链接错误:" + e.getLocalizedMessage());
		} catch (IOException e) {
			System.out.println("io链接错误:" + e.getLocalizedMessage());
		}


		String headTicket = "vmware_cgi_ticket=" + ticket.getId();

		//conn.setRequestProperty("Content-Type", "application/octet-stream");

		//conn.setRequestProperty("Content-Length", "1024");
		conn.setRequestProperty("Cookie", headTicket);
		conn.setRequestMethod("PUT");

		// setChunkedStreamingMode to -1 turns off chunked mode
		// setChunkedStreamingMode to 0 asks for system default
		// NOTE:
		// larger values mean faster connections at the
		// expense of more heap consumption.
		conn.setChunkedStreamingMode(0);

		OutputStream out = null;
		InputStream in = null;
		boolean verbose = true;

		try {
			out = conn.getOutputStream();
			in = new BufferedInputStream(new FileInputStream(sourceFilePath));

			int bufLen = 9 * 1024;
			byte[] buf = new byte[bufLen];
			byte[] tmp = null;
			int len = 0;

			final String[] spinner = new String[] {"\u0008/", "\u0008-", "\u0008\\", "\u0008|" };
			System.out.printf(".");
			int i = 0;
			while ((len = in.read(buf, 0, bufLen)) != -1) {
				tmp = new byte[len];
				System.arraycopy(buf, 0, tmp, 0, len);
				out.write(tmp, 0, len);
				if (verbose) {
					System.out.printf("%s", spinner[i++ % spinner.length]);
				}
			}
			System.out.printf("\u0008");

		} catch (FileNotFoundException e) {
			System.out.println("文件找不到:" + e.getLocalizedMessage());
		} finally {
			try {
				if(in!=null) in.close();
				if(out!=null) out.close();
				conn.getResponseCode();
			} catch (IOException e) {
				System.out.println("io出错:" + e.getLocalizedMessage());
			}
			conn.disconnect();
		}
	}

	private static SessionManagerHttpServiceRequestSpec createPutSpec(ServiceInstance si,
																	  String url) {
		SessionManagerHttpServiceRequestSpec spec = new SessionManagerHttpServiceRequestSpec();
		spec.setMethod("httpPut");	//SessionManagerHttpServiceRequestSpecMethod
		spec.setUrl(url);

		return spec;
	}

	private static SessionManagerHttpServiceRequestSpec createGetSpec(ServiceInstance si,
																	  String url) {
		SessionManagerHttpServiceRequestSpec spec = new SessionManagerHttpServiceRequestSpec();
		spec.setMethod("httpGet");	//SessionManagerHttpServiceRequestSpecMethod, also see vim.SessionManager.HttpServiceRequestSpec.Method
		spec.setUrl(url);

		return spec;
	}

	/**
	 * Ignore Certification
	 */
	private static TrustManager ignoreCertificationTrustManger = new X509TrustManager() {


		private X509Certificate[] certificates;


		@Override
		public void checkClientTrusted(X509Certificate certificates[],
									   String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] ax509certificate,
									   String s) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			// TODO Auto-generated method stub
			return null;
		}
	};

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

	private static void trustAllHttpsCertificates() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] trustAllCerts = new TrustManager[]{new TrustAllManager()};
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init((KeyManager[])null, trustAllCerts, (SecureRandom)null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
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

	private static void doHttpPostFromPython(String sessionCookie, String dcName,
											 String dsName, String sourceFilePath, String destinationFile) throws IOException {
		String url = "https://" + vcIp + ":443" + "/folder/" + destinationFile;
		System.out.println("url : " + url);

		Map<String, Object> params = new HashMap<>();
		params.put("dsName", dsName);
		params.put("dcPath", dcName);

		Map<String, Object> headers = new HashMap<>();
		headers.put("Content-Type", "application/octet-stream");

		String[] split1 = sessionCookie.split("=", 2);
		String[] split2 = split1[1].split(";", 2);
		String cookie_name = split1[0];
		String cookie_value = split2[0];
		String cookie_path = split2[1].split(";" ,2)[0];
		cookie_path = cookie_path.trim();
		String cookie_text = " " + cookie_value + "; $" + cookie_path;
		System.out.println(cookie_name + " __ " + cookie_value + " __ " + cookie_path + " __ " + cookie_text);
		Map<String, Object> cookies = new HashMap<>();
		cookies.put(cookie_name, cookie_text);

		InputStream is = null;
		try {
			is = new FileInputStream(new File(sourceFilePath));
			String responce = Requests.put(url).params(params).body(is).headers(headers).cookies(cookies).send().readToText();
			System.out.println("responce : " + responce);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			is.close();
		}
	}
}

