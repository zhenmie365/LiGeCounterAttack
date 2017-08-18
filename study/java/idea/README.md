# Some operations of Idea

# Lombok in idea
<ul>
 <li>
  It can get information of using Lombok in Idea from <a href="https://stackoverflow.com/questions/24006937/lombok-annotations-do-not-compile-under-intellij-idea">this</a> <br>
  Such as, I can solve the error of not found getXXX or setXXX after adding "-javaagent:lombok.jar" in "Shared build process VM options".<br>
 </li>
 <li>
  It must use lombok-maven-plugin when deploy project with Lombok.<br>
  When something wrong go with error "The parameters 'encoding' for goal org.projectlombok", you must add coding "utf-8" to tag "propoties" in pom.xml.<br>
  But it would come error "not getXXX method" when using mvn compile project, unless you remove all entities which uses @Getter into a isolated source folder named "lombok" which is at the same level of source folder "java".<br> 
  get details in <a href="http://www.jianshu.com/p/b03d66af04d4">this</a> and <a href="http://blog.csdn.net/rickyit/article/details/51315733">this</a> <br>
 </li>
 <li>
  Of course, it's good of using lombok with Gradle in eclipse.<br>
 </li>
 <li>
  Chinese:<br>
  对于怎么将lombok插入到idea使用,请参照以下步骤:<br>
1.首先idea要安装lombok插件,file -> Settings -> plugins -> 搜索"lombok",安装插件,然后重启idea<br>
<br>
2.可能还是出现"getXXX"或"setXXX"找不到的问题,要如下处理:<br>
   a.file -> settings -> build,Execution,Deployment -> Compiler -> Annotation Processors -> 勾选"Enable annotation processing"<br>
<br>
   b.file -> settings -> build,Execution,Deployment -> Compiler -> "Shared build process VM options" 填入 "-javaagent:lombok.jar",后面的jar包为本地lombok.jar地址.<br>
<br>
3.上面是编译遇到的问题,下面是mvn deploy时遇到的问题,请参照:<br>
  <a href="http://www.jianshu.com/p/b03d66af04d4"> this</a><br> 
 </li>
</ul>
