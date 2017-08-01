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
   #ip link add veth1 type veth peer name veth2;  //创建veth <br>
   #ip link set veth1 netns nsName;	// 将veth1分配给nsName namespace. <br>
   #ip netns exec nsName ip link show;	// 在namespace nsName里查看网络 <br>
   #ip netns exec nsName ip addr add 10.1.1.1/24 dev veth1;	// 在namespace nsName里分配ip给veth1; <br>
   #ip addr add 10.1.1.2/24 dev veth2;	// 在现有的namespace上分配ip给veth2; <br>
   #ip netns exec nsName ip link set dev veth1 up;	// 在namespace nsName里启动veth1. <br>
   #ip link set dev veth2 up;	// 在现有的namespace里启动veth2 <br>
   #ping 10.1.1.1	// 可以在namespace里ping通nsName里的veth1; <br>
   #ip netns exec nsName ping 10.1.1.2;	// 在nsName namespace里ping通现有namespace; <br>
 </li>
</ul>
