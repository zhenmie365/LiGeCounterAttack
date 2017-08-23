# git commands
simple talking of git commands

# commit git commands
<ul>
<li>1.git pull <br>
    update local repository
</li>
<li>1.git status</li>
<li>2.git add -A</li>
<li>3.git commit -a <br>
  commont description of the operation, then ctrl+x to save overwrite and Enter to exit
  </li>
<li>4.git push <br>
  push with username and pwd<br>
</li>
<li>
    5.git checkout -b newBranchName<br>
    equal to two commands: #:git branch newBranchName; git checkout newBranchName.<br>
    this command is to create new branch in local repo, and what is modified after this command is commit/push on this branch.<br>
    If there isn't this branch in remote repo, a "fatal' hint will come out to tell you set the remote as upstream,use <br>
    # git push --set-upstream origin testBranch <br>
    <a href="https://git-scm.com/book/zh/v2/Git-%E5%88%86%E6%94%AF-%E5%88%86%E6%94%AF%E7%9A%84%E6%96%B0%E5%BB%BA%E4%B8%8E%E5%90%88%E5%B9%B6">details of branch operation</a>
</li>
</ul>
