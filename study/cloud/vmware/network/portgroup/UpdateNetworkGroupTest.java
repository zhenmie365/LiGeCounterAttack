package com.sunment.cloud.vmware.network;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.HostNetworkSystem;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * 设置虚拟网络的具体属性
 * 标准交换机和vmKernel可以通过updatePortGroup()方法来设置portgroup,而相对应的虚拟交换机的属性就会改变.
 * 以上方法会覆盖继承自vSwitch的相关属性.
 * Created by zhenmie on 17年7月25日.
 */
public class UpdateNetworkGroupTest {
    public static void main(String[] args) {
        if(args.length!=6)
        {
            System.out.println("Usage: java uploadOrDownloadDsFile <vcIp> " +
                    "<username> <password> <hostName> <portgroupName> <vSwitchName> ");
            System.exit(0);
        }

        testStandardPortGroup(args);
    }

    private static void testStandardPortGroup(String[] args) {
        // host in which portgroup is
        String hostName = args[3];

        // portgroup name
        String hostPortGroupName = args[4];

        // vSwitch to which portgroup connects
        String vSwitchName = args[5];

        ServiceInstance si = getInitSi(args);

        HostSystem host = null;
        try {
            host = (HostSystem) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("HostSystem", hostName);
            HostNetworkSystem hostNetworkSystem = host.getHostNetworkSystem();

            HostNetworkPolicy hostNetworkPolicy = createHostNetworkPolicy();

            int vlanId = 365;

            HostPortGroupSpec hostPortGroupSpec = new HostPortGroupSpec();
            hostPortGroupSpec.setName(hostPortGroupName);
            hostPortGroupSpec.setPolicy(hostNetworkPolicy);
            hostPortGroupSpec.setVlanId(vlanId);
            hostPortGroupSpec.setVswitchName(vSwitchName);

            hostNetworkSystem.updatePortGroup(hostPortGroupName,hostPortGroupSpec);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // create custom HostNetworkPolicy
    private static HostNetworkPolicy createHostNetworkPolicy() {
        HostNetworkPolicy hostNetworkPolicy = new HostNetworkPolicy();

        // 有些属性要看HostNetCapabilities是否提供支持
        HostNicTeamingPolicy hostNicTeamingPolicy  = new HostNicTeamingPolicy();

        /**
         * 需要是BondBridge
         * 需要虚拟交换机或pg要开启Beacon,要看HostVirtualSwitchBondBridge中的beacon(HostVirtualSwitchBeaconConfig)是否设置.
         * 如果没有vs或pg的beacon 没有设置,这里就设置这个就会报错.
         * 其他设置项已经不用(Deprecated).
         **/
        HostNicFailureCriteria hostNicFailureCriteria = new HostNicFailureCriteria();
        hostNicFailureCriteria.setCheckBeacon(true);        // 信标探测
        hostNicTeamingPolicy.setFailureCriteria(hostNicFailureCriteria);

        // 需要是BondBridge, 设置网络适配器的顺序策略
        HostNicOrderPolicy hostNicOrderPolicy = new HostNicOrderPolicy();
        hostNicOrderPolicy.setActiveNic(new String[]{"vmnic1"});
        hostNicOrderPolicy.setStandbyNic(new String[]{"vmnic0"});
        hostNicTeamingPolicy.setNicOrder(hostNicOrderPolicy);

        // 失败是否通知物理交换机
        hostNicTeamingPolicy.setNotifySwitches(false);

        /**
         * 要看HostNetCapabilities的nicTeamingPolicy能提供的策略.
         * loadbalance_ip: route based on ip hash.                          IP哈希
         * loadbalance_srcmac: route based on source MAC hash.              MAC地址哈希
         * loadbalance_srcid: route based on the source of the port ID.     端口 ID
         * failover_explicit: use explicit failover order.                  仅故障切换
         */
        hostNicTeamingPolicy.setPolicy("loadbalance_srcmac");

        // 弃用(Deprecated)
        hostNicTeamingPolicy.setReversePolicy(false);

        // 设置重置顺序.若true,失败的pv就不会再在顺序前;如果是false,失败的pv会重启,并排在前列.
        hostNicTeamingPolicy.setRollingOrder(true);

        // 需要是BondBridge
        hostNetworkPolicy.setNicTeaming(hostNicTeamingPolicy);

        // 弃用(Deprecated)
        HostNetOffloadCapabilities hostNetOffloadCapabilities = new HostNetOffloadCapabilities();
        hostNetOffloadCapabilities.setCsumOffload(false);
        hostNetOffloadCapabilities.setTcpSegmentation(false);
        hostNetOffloadCapabilities.setZeroCopyXmit(false);
        hostNetworkPolicy.setOffloadPolicy(hostNetOffloadCapabilities);

        HostNetworkSecurityPolicy hostNetworkSecurityPolicy = new HostNetworkSecurityPolicy();
        hostNetworkSecurityPolicy.setAllowPromiscuous(false);   // 混杂模式
        hostNetworkSecurityPolicy.setForgedTransmits(false);    // 伪传输
        hostNetworkSecurityPolicy.setMacChanges(false);         // MAC地址更改
        hostNetworkPolicy.setSecurity(hostNetworkSecurityPolicy);

        HostNetworkTrafficShapingPolicy hostNetworkTrafficShapingPolicy = new HostNetworkTrafficShapingPolicy();
        hostNetworkTrafficShapingPolicy.setEnabled(true);
        hostNetworkTrafficShapingPolicy.setAverageBandwidth(15000l);   // 平均宽带 Kbps,所以1000以上,界面才有现实1Kbps
        hostNetworkTrafficShapingPolicy.setBurstSize(3000l);           // 突发大小 KB,所以1024以上,界面才有现实1KB
        hostNetworkTrafficShapingPolicy.setPeakBandwidth(136000l);      // 宽带峰值 Kbps,所以1000以上,界面才有现实1Kbps
        hostNetworkPolicy.setShapingPolicy(hostNetworkTrafficShapingPolicy);

        return hostNetworkPolicy;
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
}
