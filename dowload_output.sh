defaulHostname="ctl.simu.yarnrm-pg0.wisc.cloudlab.us"

defaultWorkload="BB"

if [ -z "$1" ]
then
	hostname=$defaulHostname
	echo "usage download_output [hostname] [newFolder]"
	exit;
else
	hostname="$1"
fi

if [ -z "$2" ]
then
	newFolder=$defaultFolder
	echo "usage download_output [hostname] [newFolder]"
	exit;
else
	newFolder="$2"
fi

resultPath="result"
defaultFolder="download"
method=""
echo "[INFO] download the files from $hostname"


subfolder1="users/tanle/SWIM/scriptsTest/workGenLogs"

downloadOuput () {
	echo "[INFO] download $2 ................"
	ssh tanle@$hostname "cd $2; tar zcvf $1.tar $1"
	scp $hostname:$2/$1.tar $resultPath/$newFolder/$hostname
	tar -xvzf $resultPath/$newFolder/$hostname/$1.tar -C $resultPath/$newFolder/$hostname
	ssh tanle@$hostname "rm -rf $2/$1.tar"
	rm -rf $resultPath/$newFolder/$hostname/$1.tar;
}

#prompt

rm -rf $resultPath/$newFolder/$hostname;
mkdir $resultPath/$newFolder/
mkdir $resultPath/$newFolder/$hostname

outputTar="output"; outputFolder="~/SpeedFairSim"; 
downloadOuput $outputTar $outputFolder 
outputTar="log"; outputFolder="~/SpeedFairSim"; 
downloadOuput $outputTar $outputFolder 

echo "[INFO] $hostname $workload"
echo "[INFO] Finished at: $(date) "

exit;

echo "
host BB 
  Hostname 128.110.152.31 
  Port 22 
  User tanle 
  IdentityFile ~/Dropbox/Papers/System/Flink/cloudlab/cloudlab.pem 

host TPCDS
  Hostname 128.110.152.32
  Port 22 
  User tanle 
  IdentityFile ~/Dropbox/Papers/System/Flink/cloudlab/cloudlab.pem 

host TPCH
  Hostname 128.110.152.28 
  Port 22 
  User tanle 
  IdentityFile ~/Dropbox/Papers/System/Flink/cloudlab/cloudlab.pem 

rm -rf log/*; rm -rf output/*; rm -rf input_gen/*
java -Xmx16384m -classpath bin/ cluster.simulator.Main BB AvgTaskDuration > execution.log
java -Xmx16384m -classpath bin/ cluster.simulator.Main BB EstimationErrors > execution.log

rm -rf bin; cp -a ../SpeedFairSim/bin ./

git reset --hard origin/master
git pull

git config --global credential.helper cache
git config --global credential.helper 'cache --timeout=2592000'
"
