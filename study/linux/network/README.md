# some technique of network in linux
<ul>
 <li>
  namespace:<br>
   用于不同命名空间的网络栈的完全隔离.<br>
   #ip netns list/add nsName;
 </li>
 <li>
  veth:<br>
   为了在不同的网络命名空间之间进行通信,利用它可以直接将两个网络命名空间连接起来.<br>
   &nbsp;#ip link add veth1 type veth peer name veth2;  &nbsp;//创建veth <br>
   &nbsp;#ip link set veth1 netns nsName;	&nbsp;// 将veth1分配给nsName namespace. <br>
   &nbsp;#ip netns exec nsName ip link show;	&nbsp;// 在namespace nsName里查看网络 <br>
   &nbsp;#ip netns exec nsName ip addr add 10.1.1.1/24 dev veth1;&nbsp;	// 在namespace nsName里分配ip给veth1; <br>
   &nbsp;#ip addr add 10.1.1.2/24 dev veth2;	&nbsp;// 在现有的namespace上分配ip给veth2; <br>
   &nbsp;#ip netns exec nsName ip link set dev veth1 up;&nbsp;	// 在namespace nsName里启动veth1. <br>
   &nbsp;#ip link set dev veth2 up;	&nbsp;// 在现有的namespace里启动veth2 <br>
   &nbsp;#ping 10.1.1.1	&nbsp;// 可以在namespace里ping通nsName里的veth1; <br>
   &nbsp;#ip netns exec nsName ping 10.1.1.2;	&nbsp;// 在nsName namespace里ping通现有namespace; <br>
   &nbsp;#ethtool -S veth1;&nbsp;// 获取另一端的接口设备的序列号 X <br>
   &nbsp;#ip netns exec nsName ip link | grep X; &nbsp;//就可以知道另一端的设备名称.<br>
 </li>
</ul>
