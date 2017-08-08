package com.sunment.cloud.vmware.client.res.ovf;

import com.sunment.cloud.res.ovf.OvfOperationSpec;
import com.sunment.cloud.util.FileUtil;
import com.sunment.cloud.util.ObjectUtil;
import com.sunment.cloud.util.VcopseAndVcenterUtil;
import com.sunment.cloud.vmware.client.factory.VmwClientFactory;
import com.sunment.cloud.vmware.client.platform.PlatformConfig;
import com.sunment.cloud.vmware.client.res.folder.VmwFolderClient;
import com.sunment.cloud.vmware.client.res.host.VmwHostSystemClient;
import com.sunment.cloud.vmware.client.res.ovf.thread.LeaseProgressUpdater;
import com.sunment.cloud.vmware.client.res.vm.VmwVirtualMachineClient;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.util.MorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ovf 文件的操作,如导出和导入
 * Created by zhenmie on 17年8月8日.
 */
public class VmwOvfClient {
    private Logger logger = LoggerFactory.getLogger(VmwOvfClient.class);
    private PlatformConfig config;

    private static final int CHUCK_LEN = 64 * 1024;

    private VmwVirtualMachineClient vmClient;
    private VmwHostSystemClient hostClient;
    private VmwFolderClient folderClient;

    public VmwOvfClient(PlatformConfig config) {
        super();
        this.config = config;
        hostClient = new VmwHostSystemClient(config);
        vmClient = new VmwVirtualMachineClient(config);
        folderClient = new VmwFolderClient(config);
    }

    /**
     * 获得vmware 服务连接
     * @return
     * @throws Exception
     */
    private ServiceInstance getServiceInstance() throws Exception {
        return VmwClientFactory.getServiceInstance(config);
    }

    /**
     * 导出ovf 到 本地磁盘
     * 注意,请一定要确保虚拟机里没有挂载iso文件.
     * @param spec      详细请看OvfOperationSpec的createExportOvfSpecInst()
     * @return          "成功后会返回"
     * @throws Exception
     */
    public String exportOvfToLocalDs(OvfOperationSpec spec) throws Exception {
        String result = "导出ovf成功";

        ServiceInstance si = getServiceInstance();

        HostSystem host = hostClient.getHostSystem(spec.getSourceHostName());
        if(ObjectUtil.isNullObject(host) || ObjectUtil.isNullObject(host.getName())){
            throw new Exception("导出ovf文件出错:源主机实体为空.");
        }

        HttpNfcLease hnLease = null;

        ManagedEntity me = null;
        if ("VirtualApp".equals(spec.getEntityType()))  // 如果待导出的实体是vApp
        {
            InventoryNavigator iv = new InventoryNavigator(si.getRootFolder());
            me = iv.searchManagedEntity("VirtualApp", spec.getEntityName());
            hnLease = ((VirtualApp)me).exportVApp();
        }
        else               // 其余的情况都当作虚拟机来操作
        {
            me = vmClient.findVirtualMachineByUUid(spec.getEntityUuid());
            try {
                hnLease = ((VirtualMachine)me).exportVm();
            } catch (FileFault e) {
                throw new Exception("exportVm方法找不到文件:" + e.getLocalizedMessage());
            } catch (InvalidPowerState e) {
                throw new Exception("exportVm方法虚拟机状态:" + e.getLocalizedMessage());
            } catch (InvalidState e) {
                throw new Exception("exportVm方法状态不对:" + e.getLocalizedMessage());
            } catch (RuntimeFault e)  {
                throw new Exception("exportVm方法运行错误:" + e.getLocalizedMessage());
            } catch (TaskInProgress e) {
                throw new Exception("exportVm方法虚拟机正在忙:" + e.getLocalizedMessage());
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
                throw new Exception("导出ovf文件出错:lease状态为error.");
            }
        }

        logger.info("HttpNfcLeaseState: ready ");
        HttpNfcLeaseInfo httpNfcLeaseInfo = hnLease.getInfo();
        httpNfcLeaseInfo.setLeaseTimeout(300*1000*1000);
        printHttpNfcLeaseInfo(httpNfcLeaseInfo);

        /**
         * 这个值可能会大于实质容量值,会令到后面的进度条显示的数字极小.
         * Note: the diskCapacityInByte could be many time bigger than
         * the total size of VMDK files downloaded.
         * As a result, the progress calculated could be much less than reality.
         */
        long diskCapacityInByte = (httpNfcLeaseInfo.getTotalDiskCapacityInKB()) * 1024;

        LeaseProgressUpdater leaseProgUpdater = new LeaseProgressUpdater(hnLease, 5000);
        leaseProgUpdater.start();

        long alredyWrittenBytes = 0;
        HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();
        if (deviceUrls != null)
        {
            OvfFile[] ovfFiles = new OvfFile[deviceUrls.length];
            logger.info("Downloading Files:");
            for (int i = 0; i < deviceUrls.length; i++)
            {
                String deviceId = deviceUrls[i].getKey();
                String deviceUrlStr = deviceUrls[i].getUrl();
                String diskFileName = deviceUrlStr.substring(deviceUrlStr.lastIndexOf("/") + 1);
                String diskUrlStr = deviceUrlStr.replace("*", spec.getSourceHostIp());
                String diskLocalPath = spec.getExportTargetDir() + diskFileName;

                // 检查是否有这个目录,如果没有就新建.
                File targetDirFile = new File(diskLocalPath);
                if(!targetDirFile.exists()) {
                    FileUtil.createMissDir(targetDirFile);
                }

                logger.info("文件名称: " + diskFileName);
                logger.info("VMDK URL: " + diskUrlStr);
                String cookie = si.getServerConnection().getVimService().getWsc().getCookie();
                long lengthOfDiskFile = 0;
                try {
                    lengthOfDiskFile = writeVMDKFile(diskLocalPath, diskUrlStr, cookie, alredyWrittenBytes, diskCapacityInByte, leaseProgUpdater);
                } catch (IOException e) { // 出问题,就关闭lease,兼关闭 leaseUpdate线程.
                    logger.info("写VMDKFile出错:" + e.getLocalizedMessage());
                    leaseProgUpdater.interrupt();
                    hnLease.httpNfcLeaseProgress(100);
                    hnLease.httpNfcLeaseAbort(null);

                    si.getServerConnection().logout();

                    return "写VMDKFile出错";
                }

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

            String ovfPath = spec.getExportTargetDir() + spec.getEntityName() + ".ovf";
            FileWriter out = new FileWriter(ovfPath);
            out.write(ovfCreateDescriptorResult.getOvfDescriptor());
            out.close();
            logger.info("OVF描述信息已写入文件: " + ovfPath);
        }

        logger.info("下载文件成功!");
        leaseProgUpdater.interrupt();
        hnLease.httpNfcLeaseProgress(100);
        hnLease.httpNfcLeaseComplete();

        si.getServerConnection().logout();

        result = "导出ovf成功.";
        return result;
    }

    public String importLocalOvfFile(OvfOperationSpec spec) throws Exception {
        String result = "";

        ServiceInstance si = getServiceInstance();

        HostSystem host = hostClient.getHostSystemByUuid(spec.getImportHostUuid());
        if(ObjectUtil.isNullObject(host) || ObjectUtil.isNullObject(host.getName())) {
            throw new Exception("导入ovf文件出错:目标主机实体为空.");
        }

        // 如果spec里面的资源池uuid为空,就直接用主机顶层的资源池
        ResourcePool rp = null;
        if(ObjectUtil.isNullString(spec.getImportResourcePoolUuid())) {
            rp = ((ComputeResource)host.getParent()).getResourcePool();
        } else {
            rp = (ResourcePool)MorUtil.createExactManagedEntity(si.getServerConnection(),
                    MorUtil.createMOR("ResourcePool", spec.getImportResourcePoolUuid()));
        }

        OvfCreateImportSpecResult ovfImportResult = createOvfCreateImportSpecResult(si, host, spec, rp);

        if(ovfImportResult==null)
        {
            si.getServerConnection().logout();
            throw new Exception("导入ovf文件出错:ovfImportResult为空.spec里面的属性有问题.");
        }

        Folder vmFolder = null;
        if(ObjectUtil.isNullString(spec.getImportFolderUuid())) {   // 如果设定待放置文件夹, 用默认的文件夹
            vmFolder = (Folder) host.getVms()[0].getParent();
        } else {
            vmFolder = folderClient.getFolderByuuid(spec.getImportFolderUuid());
        }

        long totalBytes = addTotalBytes(ovfImportResult);
        logger.info("Total bytes: " + totalBytes);

        HttpNfcLease httpNfcLease = null;

        httpNfcLease = rp.importVApp(ovfImportResult.getImportSpec(), vmFolder, host);

        // Wait until the HttpNfcLeaseState is ready
        HttpNfcLeaseState hls;
        for(;;)
        {
            hls = httpNfcLease.getState();
            if(hls == HttpNfcLeaseState.ready || hls == HttpNfcLeaseState.error)
            {
                break;
            }
        }

        if (hls.equals(HttpNfcLeaseState.ready))
        {
            logger.info("HttpNfcLeaseState: ready! 开始上传文件");
            HttpNfcLeaseInfo httpNfcLeaseInfo = (HttpNfcLeaseInfo) httpNfcLease.getInfo();
            printHttpNfcLeaseInfo(httpNfcLeaseInfo);

            LeaseProgressUpdater leaseUpdater = new LeaseProgressUpdater(httpNfcLease, 5000);
            leaseUpdater.start();

            HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();

            long bytesAlreadyWritten = 0;
            for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrls)
            {
                String deviceKey = deviceUrl.getImportKey();
                for (OvfFileItem ovfFileItem : ovfImportResult.getFileItem())
                {
                    if (deviceKey.equals(ovfFileItem.getDeviceId()))
                    {
                        logger.info("Import key==OvfFileItem device id: " + deviceKey);
                        String absoluteFile = new File(spec.getImportOvfFilePath()).getParent() + File.separator + ovfFileItem.getPath();
                        String urlToPost = deviceUrl.getUrl().replace("*", host.getName());     // 需要主机ip,用主机name来代替,注意可能name不是ip的情况.

                        try {
                            uploadVmdkFile(ovfFileItem.isCreate(), absoluteFile, urlToPost, bytesAlreadyWritten, totalBytes, leaseUpdater);
                        } catch (IOException e) {   // 出问题,就关闭lease,兼关闭 leaseUpdate线程.
                            logger.info("上传VMDKFile出错:" + e.getLocalizedMessage());
                            leaseUpdater.interrupt();
                            httpNfcLease.httpNfcLeaseProgress(100);
                            httpNfcLease.httpNfcLeaseAbort(null);

                            si.getServerConnection().logout();

                            return "上传VMDKFile出错";
                        }

                        bytesAlreadyWritten += ovfFileItem.getSize();
                        logger.info("成功上传VMDK文件:" + absoluteFile);
                    }
                }
            }

            leaseUpdater.interrupt();
            httpNfcLease.httpNfcLeaseProgress(100);
            httpNfcLease.httpNfcLeaseComplete();
        }
        si.getServerConnection().logout();

        result = "导入ovf文件成功.";
        return result;
    }

    /**
     * 根据spec 来建立 OvfCreateImportSpecResult
     * @param si
     * @param host
     * @param spec
     * @return
     * @throws Exception
     */
    private OvfCreateImportSpecResult createOvfCreateImportSpecResult(ServiceInstance si, HostSystem host,
                                                                      OvfOperationSpec spec, ResourcePool rp) throws Exception {
        List<String> networkNames = new ArrayList<String>();

        OvfCreateImportSpecParams importSpecParams = new OvfCreateImportSpecParams();
        importSpecParams.setHostSystem(host.getMOR());
        importSpecParams.setLocale("US");
        importSpecParams.setEntityName(spec.getImportTargetEntityNewName());
        importSpecParams.setDeploymentOption("");

        if(ObjectUtil.isNullString(spec.getImportDiskType())) { // 默认磁盘格式:thin
            importSpecParams.setDiskProvisioning(OvfCreateImportSpecParamsDiskProvisioningType.thin.toString());
        } else {
            importSpecParams.setDiskProvisioning(spec.getImportDiskType());
        }

        // 获取 ovf文件信息, 和网络标签名称.
        String ovfDescriptor = readOvfContent(spec.getImportOvfFilePath(), networkNames);
        if (ovfDescriptor == null)
        {
            si.getServerConnection().logout();
            throw new Exception("导入ovf文件:本地ovf文件为空或路径不对.");
        }

        logger.info("ovfDesc:" + ovfDescriptor);

        // 设置 网络映射
        if(ObjectUtil.isEmptyList(networkNames)) {  // 空的网络映射
            importSpecParams.setNetworkMapping(new OvfNetworkMapping[] { });
        } else {
            OvfNetworkMapping[] ovfNetworkMappingsArray = new OvfNetworkMapping[networkNames.size()];

            // spec里的网络属性是否为空.
            boolean isSpecNetworksEmpty = false;
            if(ObjectUtil.isEmptyList(spec.getImportNetworkUuids())) {
                isSpecNetworksEmpty = true;
            }

            for(int i = 0; i < networkNames.size(); i++) {
                OvfNetworkMapping networkMapping = new OvfNetworkMapping();
                networkMapping.setName(networkNames.get(i));

                if(isSpecNetworksEmpty) {
                    networkMapping.setNetwork(host.getNetworks()[0].getMOR());  // 如果spec里的网络uuid为空,就求其拿主机的第一个网络.
                } else {
                    int j = i + 1;
                    if(j > spec.getImportNetworkUuids().size()) {   // spec里的网络uuid数目不够的话,就用第一个的uuid的MOR.
                        ManagedObjectReference networkMor = MorUtil.createMOR("Network", spec.getImportNetworkUuids().get(0));
                        networkMapping.setNetwork(networkMor);
                    } else {                                        // 按顺序映射网络
                        ManagedObjectReference networkMor = MorUtil.createMOR("Network", spec.getImportNetworkUuids().get(i));
                        networkMapping.setNetwork(networkMor);
                    }
                }
                ovfNetworkMappingsArray[i] = networkMapping;
            }
            importSpecParams.setNetworkMapping(ovfNetworkMappingsArray);
        }

        importSpecParams.setPropertyMapping(null);

        // 如果spec里面的数据存储为空,就直接用主机里的某一个数据存储.
        Datastore ds = null;
        if(ObjectUtil.isNullString(spec.getImportDatastoreUuid())) {
            ds = host.getDatastores()[0];
        } else {
            ds = (Datastore) MorUtil.createExactManagedEntity(si.getServerConnection(),
                    MorUtil.createMOR("Datastore", spec.getImportDatastoreUuid()));
        }

        OvfCreateImportSpecResult ovfImportResult = si.getOvfManager().createImportSpec(
                ovfDescriptor, rp, ds, importSpecParams);

        return ovfImportResult;
    }

    private String readOvfContent(String ovfFilePath, List<String> networkNameList)  throws IOException
    {
        StringBuffer strContent = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(ovfFilePath)));
        String lineStr;
        while ((lineStr = in.readLine()) != null)
        {
            strContent.append(lineStr);

            // 找出网络标签,这行的格式如: <Network ovf:name="lzyNet_devstack_dontMove">, 需要获取的是lzyNet_devstack_dontMove
            if(lineStr.contains("<Network ovf:name=")) {
                String[] splitString = lineStr.split("\"");
                if(ObjectUtil.isNotNullArray(splitString) && splitString.length > 2) {
                    networkNameList.add(splitString[1]);
                }
            }
        }
        in.close();
        return strContent.toString();
    }

    /**
     * 获取 httpsConnection, 设置了cookie的
     * @param urlStr
     * @param cookieStr
     * @return
     * @throws IOException
     */
    private HttpsURLConnection getHTTPConnection(String urlStr, String cookieStr) throws IOException
    {
        HttpsURLConnection conn = VcopseAndVcenterUtil.createTrustAllCertsHttpsConnection(urlStr);

        conn.setRequestProperty("Cookie",  cookieStr);
        conn.connect();
        return conn;
    }

    /**
     * 写VmdkFile,顺便统计lease的进度,和设置LeaseProgressUpdater的进度
     * @param localFilePath
     * @param diskUrl
     * @param cookie
     * @param bytesAlreadyWritten
     * @param totalBytes
     * @return
     * @throws IOException
     */
    private long writeVMDKFile(String localFilePath, String diskUrl, String cookie,
                               long bytesAlreadyWritten, long totalBytes,
                               LeaseProgressUpdater leaseProgressUpdater) throws IOException
    {
        HttpsURLConnection conn = null;
        try {
             conn = getHTTPConnection(diskUrl, cookie);
        } catch (IOException e) {
            throw new IOException("建立https链接时出错:" + e.getLocalizedMessage());
        }

        InputStream in = null;
        OutputStream out = null;
        long bytesWritten = 0;

        try {
            in = conn.getInputStream();
            out = new FileOutputStream(new File(localFilePath));

            byte[] buf = new byte[102400];
            int len = 0;

            while ((len = in.read(buf)) > 0)
            {
                out.write(buf, 0, len);
                bytesWritten += len;
                int percent = (int)(((bytesAlreadyWritten + bytesWritten) * 100) / totalBytes);
                leaseProgressUpdater.setPercent(percent);   // 会很少,因为 totalBytes会比实质容量大几倍
                //System.out.println("written: " + bytesWritten + "; percent : " + percent);
            }
        } catch (IOException e) {
            throw new IOException("写VMDK文件时出错:" + e.getLocalizedMessage());
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                throw new IOException("关闭输入输出流时出错:" + e.getLocalizedMessage());
            }
        }

        return bytesWritten;
    }

    /**
     * 打印 lease的 磁盘信息
     * @param info
     */
    private void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info)
    {
        logger.info("Lease Timeout: " + info.getLeaseTimeout());
        logger.info("Total Disk capacity: "  + info.getTotalDiskCapacityInKB());    // 很大概率会大于实质容量
        HttpNfcLeaseDeviceUrl[] deviceUrlArr = info.getDeviceUrl();
        if (deviceUrlArr != null)
        {
            int deviceUrlCount = 1;
            for (HttpNfcLeaseDeviceUrl durl : deviceUrlArr)
            {
                logger.info("HttpNfcLeaseDeviceUrl : "
                        + deviceUrlCount++);
                logger.info("  Device URL Key: " + durl.getKey());
                logger.info("  Device URL : " + durl.getUrl());
            }
        }
        else
        {
            logger.info("No Device URLS Found");
        }
    }

    /**
     * 根据 磁盘url 来建立https链接. 然后上传vmdk磁盘文件到相应的位置.
     * @param put
     * @param diskFilePath
     * @param urlStr
     * @param bytesAlreadyWritten
     * @param totalBytes
     * @param leaseProgressUpdater
     * @throws IOException
     */
    private void uploadVmdkFile(boolean put, String diskFilePath, String urlStr,
                                long bytesAlreadyWritten, long totalBytes,
                                LeaseProgressUpdater leaseProgressUpdater) throws IOException
    {
        HttpsURLConnection conn = null;
        try {
            conn = VcopseAndVcenterUtil.createTrustAllCertsHttpsConnection(urlStr);
        } catch (IOException e) {
            throw new IOException("建立https链接时出错:" + e.getLocalizedMessage());
        }

        conn.setChunkedStreamingMode(CHUCK_LEN);
        conn.setRequestMethod(put? "PUT" : "POST"); // Use a post method to write the file.
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type",  "application/x-vnd.vmware-streamVmdk");
        conn.setRequestProperty("Content-Length", Long.toString(new File(diskFilePath).length()));

        BufferedOutputStream bos = null;
        BufferedInputStream diskis = null;
        try{
            bos = new BufferedOutputStream(conn.getOutputStream());
            diskis = new BufferedInputStream(new FileInputStream(diskFilePath));

            int bytesAvailable = diskis.available();
            int bufferSize = Math.min(bytesAvailable, CHUCK_LEN);
            byte[] buffer = new byte[bufferSize];
            int preProgressPercent = 0;

            long totalBytesWritten = 0;
            while (true)
            {
                int bytesRead = diskis.read(buffer, 0, bufferSize);
                if (bytesRead == -1)
                {
                    break;
                }

                totalBytesWritten += bytesRead;
                bos.write(buffer, 0, bufferSize);
                bos.flush();
                int progressPercent = (int) (((bytesAlreadyWritten + totalBytesWritten) * 100) / totalBytes);

                // 反馈进度.
                if(progressPercent != preProgressPercent) {
                    logger.info("导入ovf文件中上传文件进度为:" + progressPercent);
                }

                leaseProgressUpdater.setPercent(progressPercent);
                preProgressPercent = progressPercent;
            }
        } catch (IOException e) {
            throw new IOException("导入ovf文件:上传磁盘文件出错." + e.getLocalizedMessage());
        } finally {
            try {
                diskis.close();
                bos.flush();
                bos.close();
            } catch (IOException e) {
                throw new IOException("导入ovf文件:关闭读写流出错." + e.getLocalizedMessage());
            }
            conn.disconnect();
        }
    }

    /**
     * 计算实际 ovf 磁盘 总容量.并打印磁盘信息
     * @param ovfImportResult
     * @return
     */
    private long addTotalBytes(OvfCreateImportSpecResult ovfImportResult)
    {
        OvfFileItem[] fileItemArr = ovfImportResult.getFileItem();

        long totalBytes = 0;
        if (fileItemArr != null)
        {
            for (OvfFileItem fi : fileItemArr)
            {
                printOvfFileItem(fi);
                totalBytes += fi.getSize();
            }
        }
        return totalBytes;
    }

    /**
     * 打印导入ovf磁盘文件信息
     * @param fi
     */
    private void printOvfFileItem(OvfFileItem fi)
    {
        logger.info("================ OvfFileItem ================");
        logger.info("chunkSize: " + fi.getChunkSize());
        logger.info("create: " + fi.isCreate());
        logger.info("deviceId: " + fi.getDeviceId());
        logger.info("path: " + fi.getPath());
        logger.info("size: " + fi.getSize());
        logger.info("==============================================");
    }
}
