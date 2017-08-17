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
</ul>
