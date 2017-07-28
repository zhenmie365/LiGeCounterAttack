import com.vmware.vim25.*;
import com.vmware.vim25.mo.DistributedVirtualPortgroup;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VmwareDistributedVirtualSwitch;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * detail reconfiguration of DvsPortgroup.
 * example : java uploadOrDownloadDsFile <vcIp> <username> <password> <dvsPortgroupName>
 * and create DVsPortgroup example: java uploadOrDownloadDsFile <vcIp> <username> <password> <dVSwtichName>
 * Created by zhenmie on 17年7月25日.
 */
public class reconfigureDVSPortgroupTest {
    public static void main(String[] args) {
        if(args.length!=4)
        {
            System.out.println("Usage: java uploadOrDownloadDsFile <vcIp> " +
                    "<username> <password> <dvsPortgroupName> ");
            System.exit(0);
        }

        testDVSPortgroup(args);
        //createDvsPortgroup(args);
    }

    private static void testDVSPortgroup(String[] args) {
        String DvsPortName = args[3];//"dvPortGroup";

        ServiceInstance si = getInitSi(args);

        DistributedVirtualPortgroup dvPortGroup = null;

        try {
            dvPortGroup = (DistributedVirtualPortgroup) new InventoryNavigator(si.getRootFolder()).
                    searchManagedEntity("DistributedVirtualPortgroup", DvsPortName);

            DVPortgroupConfigSpec dvPortgroupConfigSpec = createDvPortgroupSpec();
            dvPortGroup.reconfigureDVPortgroup_Task(dvPortgroupConfigSpec);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static DVPortgroupConfigSpec createDvPortgroupSpec() {
        DVPortgroupConfigSpec dvPortgroupConfigSpec = new DVPortgroupConfigSpec();

        //The flag is currently supported only for static portgroups,看下面的type
        dvPortgroupConfigSpec.setAutoExpand(false);     //若true,将无视端口的上限
        dvPortgroupConfigSpec.setConfigVersion("0");     // reconfig方法要和DVPortgroupConfigInfo的configVersion一致

        BoolPolicy boolPolicy = new BoolPolicy();

        VMwareDVSPortSetting vMwareDVSPortSetting = new VMwareDVSPortSetting();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        vMwareDVSPortSetting.setIpfixEnabled(boolPolicy);   // 要switch设置了VMwareDVSConfigInfo的ipAddress和ipfixConfig

        // 弃用(Deprecated),在端口层次没有作用
        //vMwareDVSPortSetting.setLacpPolicy(new VMwareUplinkLacpPolicy());

        // 弃用(Deprecated)
        //vMwareDVSPortSetting.setQosTag(new IntPolicy());

        setDvsSecurityPolicy(vMwareDVSPortSetting);

        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        vMwareDVSPortSetting.setTxUplink(boolPolicy);

        setUplinkTeamingPolicy(vMwareDVSPortSetting);

        // 还有两个类型VmwareDistributedVirtualSwitchPvlanSpec, VmwareDistributedVirtualSwitchTrunkVlanSpec
        VmwareDistributedVirtualSwitchVlanIdSpec vmwareDistributedVirtualSwitchVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        vmwareDistributedVirtualSwitchVlanIdSpec.setInherited(false);
        vmwareDistributedVirtualSwitchVlanIdSpec.setVlanId(36); // 0表示不用vlan,A value from 1 to 4094
        vMwareDVSPortSetting.setVlan(vmwareDistributedVirtualSwitchVlanIdSpec);

        // Extends DVPortSetting
        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        vMwareDVSPortSetting.setBlocked(boolPolicy);  //Indicates whether this port is blocked. If a port is blocked, packet forwarding is stopped.

        setTrafficShapingPolicy(vMwareDVSPortSetting);

        // 这些属性都可以忽略不设
        //setOtherPropotis(vMwareDVSPortSetting);


        dvPortgroupConfigSpec.setDefaultPortConfig(vMwareDVSPortSetting);

        dvPortgroupConfigSpec.setDescription("description");
        dvPortgroupConfigSpec.setName("createDvsPortgroupTest");    // 会重命名这个portgroup
        dvPortgroupConfigSpec.setNumPorts(36);  // 会随着这个数字来增删端口,情况不对会抛错.portgroups of type ephemeral this property is ignored

        setDvPortgroupPolicy(dvPortgroupConfigSpec);

        // 定义端口的命名格式(DistributedVirtualPortgroupMetaTagName), 例如 "redNetwork-<portIndex>" and "<dvsName>-pnic<portIndex>"
        dvPortgroupConfigSpec.setPortNameFormat("sunmnet-<dvsName>-<portIndex>");

        // Eligible entities that can connect to the portgroup
        //dvPortgroupConfigSpec.setScope(new ManagedObjectReference[]{});

        // 看DistributedVirtualPortgroupPortgroupType, 有earlyBinding, ephemeral, lateBinding(Deprecated)
        // Early binding specifies a static set of ports that are created when you create the distributed virtual portgroup. An ephemeral portgroup uses dynamic ports that are created when you power on a virtual machine
        dvPortgroupConfigSpec.setType("earlyBinding");

        // Indicates whether the portgroup is an uplink portroup.Since vSphere API 6.5
        //dvPortgroupConfigSpec.isUpLink(true);

        // Opaque binary blob that stores vendor specific configuration.
        //dvPortgroupConfigSpec.setVendorSpecificConfig(new DistributedVirtualSwitchKeyedOpaqueBlob[]{});

        // The key of virtual NIC network resource pool to be associated with a portgroup. Setting this property to "-1", would mean that this portgroup is not associated with any virtual NIC network resource pool.
        dvPortgroupConfigSpec.setVmVnicNetworkResourcePoolKey("-1");

        return dvPortgroupConfigSpec;
    }

    private static void setDvPortgroupPolicy(DVPortgroupConfigSpec dvPortgroupConfigSpec) {
        // vMwareDVSPortgroupPolicy继承于DVPortgroupPolicy
        VMwareDVSPortgroupPolicy vMwareDVSPortgroupPolicy = new VMwareDVSPortgroupPolicy();
        // 这里有几个是客户端没有展示的
        // 这里替代的意思是指,个别端口的策略覆盖portgroup的策略(defaultPortConfig)
        vMwareDVSPortgroupPolicy.setIpfixOverrideAllowed(true);             // 是否替代IpFix        0
        vMwareDVSPortgroupPolicy.setSecurityPolicyOverrideAllowed(true);    // 是否替代安全策略     0
        vMwareDVSPortgroupPolicy.setUplinkTeamingOverrideAllowed(true);     // 允许替代上行链路成组   0
        vMwareDVSPortgroupPolicy.setVlanOverrideAllowed(true);              // 是否允许替代VLan   0
        vMwareDVSPortgroupPolicy.setBlockOverrideAllowed(false);             // 是否允许替代阻止    0
        vMwareDVSPortgroupPolicy.setLivePortMovingAllowed(true);            // 是否允许活动端口转移   0
        vMwareDVSPortgroupPolicy.setNetworkResourcePoolOverrideAllowed(true);   // 是否允许资源分配     0
        vMwareDVSPortgroupPolicy.setPortConfigResetAtDisconnect(false);      // 是否允许在端口链接的情况重设端口信息  0
        vMwareDVSPortgroupPolicy.setShapingOverrideAllowed(true);           // 是否允许替代shaping    0
        vMwareDVSPortgroupPolicy.setTrafficFilterOverrideAllowed(true);     // 是否允许替代traffic 过滤策略   0
        vMwareDVSPortgroupPolicy.setVendorConfigOverrideAllowed(true);      // 是否允许替代供应商配置  0

        dvPortgroupConfigSpec.setPolicy(vMwareDVSPortgroupPolicy);
    }

    // 这些属性可以忽略不设
    private static void setOtherPropotis(VMwareDVSPortSetting vMwareDVSPortSetting) {
        /**
         * 网络过滤策略,会继承上层实体(DVS),具体策略看DvsFilterConfig,要修改就看DvsTrafficFilterConfigSpec.
         * DvsFilterConfig qualifier有SingleIp, IpRange, SingleMac, MacRange,等
         * Actions有DvsDropNetworkRuleAction, DvsAcceptNetworkRuleAction, DvsPuntNetworkRuleAction,等
         */
        DvsFilterPolicy dvsFilterPolicy = new DvsFilterPolicy();
        dvsFilterPolicy.setFilterConfig(new DvsFilterConfig[]{});
        vMwareDVSPortSetting.setFilterPolicy(dvsFilterPolicy);

        // 唔知咩黎(Deprecated)(Deprecated),看DVPortgroupConfigInfo的DVPortgroupConfigInfo
        vMwareDVSPortSetting.setNetworkResourcePoolKey(new StringPolicy());

        // 供应商信息
        vMwareDVSPortSetting.setVendorSpecificConfig(new DVSVendorSpecificConfig());

        // 设置VMDirectPath Gen 2是否得到支持.是在 DVSFeatureCapability,HostCapability,PhysicalNic,和VirtualEthernetCardOption层面上设置.
        vMwareDVSPortSetting.setVmDirectPathGen2Allowed(new BoolPolicy());
    }

    private static void setTrafficShapingPolicy(VMwareDVSPortSetting vMwareDVSPortSetting) {
        BoolPolicy boolPolicy= new BoolPolicy();
        // 入
        DVSTrafficShapingPolicy dvsTrafficShapingPolic = new DVSTrafficShapingPolicy();
        dvsTrafficShapingPolic.setInherited(false);
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        dvsTrafficShapingPolic.setEnabled(boolPolicy);

        LongPolicy longPolicy = new LongPolicy();
        longPolicy.setInherited(false);
        longPolicy.setValue(3000l);
        dvsTrafficShapingPolic.setAverageBandwidth(longPolicy);

        longPolicy = new LongPolicy();
        longPolicy.setInherited(false);
        longPolicy.setValue(7000l);
        dvsTrafficShapingPolic.setBurstSize(longPolicy);

        longPolicy = new LongPolicy();
        longPolicy.setInherited(false);
        longPolicy.setValue(36000l);
        dvsTrafficShapingPolic.setPeakBandwidth(longPolicy);
        vMwareDVSPortSetting.setInShapingPolicy(dvsTrafficShapingPolic);

        // 出
        dvsTrafficShapingPolic = new DVSTrafficShapingPolicy();
        dvsTrafficShapingPolic.setInherited(false);
        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        dvsTrafficShapingPolic.setEnabled(boolPolicy);

        longPolicy = new LongPolicy();
        longPolicy.setInherited(false);
        longPolicy.setValue(6000l);
        dvsTrafficShapingPolic.setAverageBandwidth(longPolicy);

        longPolicy = new LongPolicy();
        longPolicy.setInherited(false);
        longPolicy.setValue(14000l);
        dvsTrafficShapingPolic.setBurstSize(longPolicy);

        longPolicy = new LongPolicy();
        longPolicy.setInherited(false);
        longPolicy.setValue(336000l);
        dvsTrafficShapingPolic.setPeakBandwidth(longPolicy);
        vMwareDVSPortSetting.setOutShapingPolicy(dvsTrafficShapingPolic);
    }

    private static void setUplinkTeamingPolicy(VMwareDVSPortSetting vMwareDVSPortSetting) {
        BoolPolicy boolPolicy = new BoolPolicy();
        VmwareUplinkPortTeamingPolicy vmwareUplinkPortTeamingPolicy = new VmwareUplinkPortTeamingPolicy();
        vmwareUplinkPortTeamingPolicy.setInherited(false);  // 是否继承DVS
        DVSFailureCriteria dvsFailureCriteria = new DVSFailureCriteria();
        dvsFailureCriteria.setInherited(false);
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        dvsFailureCriteria.setCheckBeacon(boolPolicy);
        vmwareUplinkPortTeamingPolicy.setFailureCriteria(dvsFailureCriteria);   // 现在只支持Beacon

        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(false);
        vmwareUplinkPortTeamingPolicy.setNotifySwitches(boolPolicy);    // 是否失败后通知vswitch

        StringPolicy stringPolicy = new StringPolicy();
        stringPolicy.setInherited(false);
        stringPolicy.setValue("loadbalance_loadbased");  // 要看DVSFeatureCapability属性nicTeamingPolicy提供了哪几种,类型看DistributedVirtualSwitchNicTeamingPolicyMode
        vmwareUplinkPortTeamingPolicy.setPolicy(stringPolicy);

        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(false);
        vmwareUplinkPortTeamingPolicy.setReversePolicy(boolPolicy); // 是否回滚

        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        vmwareUplinkPortTeamingPolicy.setRollingOrder(boolPolicy);  // 是否失败后重启port,然后重排序

        VMwareUplinkPortOrderPolicy vMwareUplinkPortOrderPolicy = new VMwareUplinkPortOrderPolicy();
        vMwareUplinkPortOrderPolicy.setInherited(false);
        vMwareUplinkPortOrderPolicy.setActiveUplinkPort(new String[]{"dvUplink2", "dvUplink3"});  // 待输入 upLink port
        vMwareUplinkPortOrderPolicy.setStandbyUplinkPort(new String[]{"dvUplink1", "dvUplink4"}); // 待输入 upLink port
        vmwareUplinkPortTeamingPolicy.setUplinkPortOrder(vMwareUplinkPortOrderPolicy);
        vMwareDVSPortSetting.setUplinkTeamingPolicy(vmwareUplinkPortTeamingPolicy);
    }

    private static void setDvsSecurityPolicy(VMwareDVSPortSetting vMwareDVSPortSetting) {
        BoolPolicy boolPolicy = new BoolPolicy();
        DVSSecurityPolicy dvsSecurityPolicy = new DVSSecurityPolicy();
        dvsSecurityPolicy.setInherited(false);  // 是否继承DVS

        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        dvsSecurityPolicy.setAllowPromiscuous(boolPolicy);  // 混杂模式

        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        dvsSecurityPolicy.setForgedTransmits(boolPolicy);   // 伪传输

        boolPolicy = new BoolPolicy();
        boolPolicy.setInherited(false);
        boolPolicy.setValue(true);
        dvsSecurityPolicy.setMacChanges(boolPolicy);        // MAC地址更改
        vMwareDVSPortSetting.setSecurityPolicy(dvsSecurityPolicy);
    }

    // get ServiceInstance of vc
    public static ServiceInstance getInitSi(String[] args){
        ServiceInstance si  = null;
        try {
            si = new ServiceInstance(new URL("https://" + args[0] + "/sdk"), args[1], args[2], true);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return si;
    }

    /**
     * example of create DvsPortgroup.
     * @param args
     */
    public static void createDvsPortgroup(String[] args) {
        ServiceInstance si = getInitSi(args);
        try {
            String dvswitchName = args[3];

            VmwareDistributedVirtualSwitch dvs = (VmwareDistributedVirtualSwitch) new InventoryNavigator(si.getRootFolder()).
                    searchManagedEntity("VmwareDistributedVirtualSwitch", dvswitchName);

            DVPortgroupConfigSpec dvPortgroupConfigSpec = createDvPortgroupSpec();
            dvs.createDVPortgroup_Task(dvPortgroupConfigSpec);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
