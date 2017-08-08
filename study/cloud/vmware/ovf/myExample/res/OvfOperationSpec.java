package com.sunment.cloud.res.ovf;

import java.util.List;

/**
 * ovf 操作相关属性的spec
 * Created by zhenmie on 17年8月8日.
 */
public class OvfOperationSpec {
    private String entityUuid;                  // 实体(虚拟机或vApp)的uuid
    private String entityName;                  // 实体名称
    private String entityType;                  // 实体类型:虚拟机(VirtualMachine) 或 vApp(VirtualApp)

    private String sourceHostName;              // 实体所属主机的名称,很可能和主机的ip一样
    private String sourceHostIp;                // 实体所属主机的ip,
    private String exportTargetDir;             // 导出ovf待放置的目录:"/home/Document/ovf/".注意,一定要以目录分隔符结尾,不然导出的ovf不能如常导入

    private String importTargetEntityNewName;   // 导入的虚拟机的名字
    private String importHostUuid;              // 放置虚拟机的主机uuid
    private String importOvfFilePath;           // 导入ovf的文件路径
    private String importResourcePoolUuid;      // not required,如果提供,资源池需要属于指定主机
    private String importDatastoreUuid;         // not required,如果提供,数据存储需要属于指定主机
    private List<String> importNetworkUuids;    // not required,如果提供,网络需要属于指定主机
    private String importFolderUuid;            // not required,
    private String importDiskType;              // not required,虚拟机磁盘的类型,如:think或thin等....

    public OvfOperationSpec() {
    }

    /**
     * 导出ovf 的spec构造函数
     * @param entityUuid
     * @param entityName
     * @param entityType
     * @param sourceHostName
     * @param sourceHostIp
     * @param exportTargetDir
     */
    public OvfOperationSpec(String entityUuid, String entityName, String entityType,
                            String sourceHostName, String sourceHostIp, String exportTargetDir) {
        this.entityUuid = entityUuid;
        this.entityName = entityName;
        this.entityType = entityType;
        this.sourceHostName = sourceHostName;
        this.sourceHostIp = sourceHostIp;
        this.exportTargetDir = exportTargetDir;
    }

    /**
     * 导入ovf 的spec构造函数
     * @param importTargetEntityNewName
     * @param importHostUuid
     * @param importOvfFilePath
     * @param importResourcePoolUuid
     * @param importNetworkUuids
     * @param importDatastoreUuid
     * @param importFolderUuid
     * @param importDiskType
     */
    public OvfOperationSpec(String importTargetEntityNewName, String importHostUuid, String importOvfFilePath,
                            String importResourcePoolUuid, List<String> importNetworkUuids,
                            String importDatastoreUuid, String importFolderUuid, String importDiskType) {
        this.importTargetEntityNewName = importTargetEntityNewName;
        this.importHostUuid = importHostUuid;
        this.importOvfFilePath = importOvfFilePath;
        this.importResourcePoolUuid = importResourcePoolUuid;
        this.importNetworkUuids = importNetworkUuids;
        this.importDatastoreUuid = importDatastoreUuid;
        this.importFolderUuid = importFolderUuid;
        this.importDiskType = importDiskType;
    }

    /**
     * 导出ovf 的spec 构建类
     * @param entityUuid
     * @param entityName
     * @param entityType
     * @param sourceHostName
     * @param sourceHostIp
     * @param exportTargetDir
     * @return
     */
    public static OvfOperationSpec createExportOvfSpecInst(String entityUuid, String entityName, String entityType,
                                                           String sourceHostName, String sourceHostIp, String exportTargetDir) {
        OvfOperationSpec spec = new OvfOperationSpec(entityUuid, entityName, entityType, sourceHostName,sourceHostIp, exportTargetDir);
        return spec;
    }

    /**
     * 导入ovf 的spec 构建类
     * @param importTargetEntityNewName
     * @param importHostUuid
     * @param importOvfFilePath
     * @param importResourcePoolUuid
     * @param importNetworkUuids
     * @param importDatastoreUuid
     * @param importFolderUuid
     * @param importDiskType
     * @return
     */
    public static OvfOperationSpec createImportOvfSpecInst(String importTargetEntityNewName, String importHostUuid, String importOvfFilePath,
                                                           String importResourcePoolUuid, List<String> importNetworkUuids,
                                                           String importDatastoreUuid, String importFolderUuid, String importDiskType) {
        OvfOperationSpec spce = new OvfOperationSpec(importTargetEntityNewName, importHostUuid,importOvfFilePath,
                importResourcePoolUuid,importNetworkUuids,importDatastoreUuid,importFolderUuid,importDiskType);
        return spce;
    }

    public String getImportDiskType() {
        return importDiskType;
    }

    public void setImportDiskType(String importDiskType) {
        this.importDiskType = importDiskType;
    }

    public String getImportFolderUuid() {
        return importFolderUuid;
    }

    public void setImportFolderUuid(String importFolderUuid) {
        this.importFolderUuid = importFolderUuid;
    }

    public String getImportTargetEntityNewName() {
        return importTargetEntityNewName;
    }

    public void setImportTargetEntityNewName(String importTargetEntityNewName) {
        this.importTargetEntityNewName = importTargetEntityNewName;
    }

    public String getImportResourcePoolUuid() {
        return importResourcePoolUuid;
    }

    public void setImportResourcePoolUuid(String importResourcePoolUuid) {
        this.importResourcePoolUuid = importResourcePoolUuid;
    }

    public String getImportDatastoreUuid() {
        return importDatastoreUuid;
    }

    public void setImportDatastoreUuid(String importDatastoreUuid) {
        this.importDatastoreUuid = importDatastoreUuid;
    }

    public List<String> getImportNetworkUuids() {
        return importNetworkUuids;
    }

    public void setImportNetworkUuids(List<String> importNetworkUuids) {
        this.importNetworkUuids = importNetworkUuids;
    }

    public String getExportTargetDir() {
        return exportTargetDir;
    }

    public void setExportTargetDir(String exportTargetDir) {
        this.exportTargetDir = exportTargetDir;
    }

    public String getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(String entityUuid) {
        this.entityUuid = entityUuid;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getSourceHostName() {
        return sourceHostName;
    }

    public void setSourceHostName(String sourceHostName) {
        this.sourceHostName = sourceHostName;
    }

    public String getImportHostUuid() {
        return importHostUuid;
    }

    public void setImportHostUuid(String importHostUuid) {
        this.importHostUuid = importHostUuid;
    }

    public String getSourceHostIp() {
        return sourceHostIp;
    }

    public void setSourceHostIp(String sourceHostIp) {
        this.sourceHostIp = sourceHostIp;
    }

    public String getImportOvfFilePath() {
        return importOvfFilePath;
    }

    public void setImportOvfFilePath(String importOvfFilePath) {
        this.importOvfFilePath = importOvfFilePath;
    }
}
