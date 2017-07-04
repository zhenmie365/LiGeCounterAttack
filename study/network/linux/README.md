# some network infos

# iptables
<a href="http://www.linuxso.com/linuxpeixun/10332.html"> netfilter/iptables全攻略 </a> , <a href="http://blog.chinaunix.net/uid-26495963-id-3279216.html"> iptables详解 </a>, <a href="http://blog.csdn.net/qustdjx/article/details/7875748"> ubuntu开启iptables </a>
<br>
such as,setting icmp forbidden,command : <br>
    iptables -A OUTPUT -p icmp --icmp-type 8 -j DROP;<br>
  It will ping command output "sendmsg: Operation not permitted" <br>
  
command: iptables -A INPUT -p icmp --icmp-type 0 -j DROP; <br>
  It will cause host can't ping outside ip, but outside can ping host ip.<br>
command: iptables -A INPUT -p icmp --icmp-type 8 -s 0/0 -j DROP;<br>
  iptables -A INPUT -p icmp --icmp-type 0 -s 0/0 -j ACCEPT;<br>
    It will cause host can ping outside but outside can't ping host ip.<br>
