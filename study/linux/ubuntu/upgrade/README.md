# something special of upgrade
This just happened to me after the 14.04 update, and it seems to be because my virtual environments have old copies of /usr/bin/python2.7<br>
I fixed each virtual environment by activating it and running:<br>
    $ cp /usr/bin/python2.7 $(which python2.7)<br>
Such as, overwrite python2.7 in /opt/stack/requirements/.venv/bin when run stack.sh of devstack after ubuntu upgraded to 16.04.
