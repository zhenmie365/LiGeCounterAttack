# configuration of portgroup
there are two types of portgroup: portgroup used by standard vswitch or vmkernel, and portgroup used by distributed virtual switch. So provide two examples above.<br>
You can get details from vmware sdk html according to comment in examples above.<br>
In my opinion, you can look DVUpLinkPortgroup in DVS as pNic in standardVSwitch used for connecting host, and DVPortgroup as 
standardPortgroup or vmkernel used for connecting vm.
