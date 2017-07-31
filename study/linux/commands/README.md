# infos of linux commands
<ul>
  <li>
  find <br>
    use -xdev option to avoid GVFS-FUSE privilege problems.
  </li>
  <li>
  du <br>
    -h human-readable <br>
    --max-depth defind hierarchy of folder want to show <br>
    such as : du -h --max-depth=1 /usr <br>
  </li>
  <li>
  tty <br>
      some solution : <a href="http://blog.sina.com.cn/s/blog_704836f401010osn.html">Sharing a screen session with another administrator on a Linux system</a> <br>
  </li>
  <li>
  nfs<br>
    In server, install command is : $sudo apt-get install nfs-kernel-server; (include nfs-common module)<br>
    In client, install command is : $sudo apt-get install nfs-common<br>
    edit /etc/exports file to add nfs file:<br>
    /home/USER/nfs *(rw,sync,no_root_squash,no_subtree_check)<br>
    then restart rpc and nfs:<br>
    $sudo service rpcbind restart restart <br>
    $sudo service restartnfs-kernel-server restart <br>
    check if nfs file is ready:<br>
    $showmount -e <br>
    <br>
    In client, mount nfs file :<br>
    $sudo mount -t nfs ***.***.***.***:/home/USER/nfs /nfs-clinet/; while ***.***... is nfs-server ip.<br>
    
  </li>
</ul>
