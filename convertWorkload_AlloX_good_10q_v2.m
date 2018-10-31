clear; close all; clc;

OUTPUT = 'job_google.txt';
speedupRate = [33 40  32*2 32*3 32*3 32*5 32*6 32*7 32*8 32*15]/32;  % 32 cores per node vs 1 GPU
std_coeff = [1 1 1 1]*1.2;
% NJobs = 1000;
nSmallJobUser= 10;
nLargeJobUser=0;
nSmallJobs = 1000;
nLargeJobs = 100;

timeScale = 60;

strInfo = ['% timeScale=' num2str(timeScale) ' nSmallJobs=' num2str(nSmallJobs) '\n'];

nMaxJobs = max(nSmallJobs, nLargeJobs);
NJobs = [nSmallJobs * ones(1,nSmallJobUser) nLargeJobs *ones(1,nLargeJobUser)];
nUsers = nSmallJobUser + nLargeJobUser;
numOfJobs = sum(NJobs);

google_path = '/ssd/projects/ClusterData2011_2/';
JOB_FILE  = [google_path 'jobInfo.mat'];
load(JOB_FILE);
jobIds = JobInfos(:,1);
scheduleClass =JobInfos(:,4);
TIME_SCALE = 1;
second = 10^6;

QUEUED_UP_TIME = 100;

complTimes = (JobInfos(:,3) - JobInfos(:,5))/(timeScale*second); % microsecond
temp = [jobIds JobInfos(:,2) JobInfos(:,3) scheduleClass JobInfos(:,5) JobInfos(:,7)];

jobComplInfos = temp(find(complTimes > 0),:);
jobComplInfos = jobComplInfos(find(jobComplInfos(:,2) >= 0),:);
% complTimes = complTimes(find(complTimes>0));

%
%load(JOB_USAGE); 
jobUsage = JobInfos;

temp1 = ismember(jobUsage(:,1), jobComplInfos(:,1)); 
temp2 = ismember(jobComplInfos(:,1), jobUsage(temp1,1)); 
mJobUsage = [jobUsage(temp1,:) jobComplInfos(temp2,2:6)];
ARRIVAL_TIME_IGNORE = 2.4*24*3600 + 750;
jobSet = mJobUsage;
arrival_set = zeros(1,numOfJobs);
jobIdx = 1;
for iJob=1:length(jobSet(:,1))
    arrivalTime = jobSet(iJob,5)/10^6;
    if (arrivalTime < ARRIVAL_TIME_IGNORE) %|| (complTime > LONG_JOB_IGNORE)
      continue;
    end   
    arrivalTime = arrivalTime - ARRIVAL_TIME_IGNORE;
    arrivalTime = arrivalTime/timeScale;
    arrivalTime = floor(arrivalTime*30);
    cpuReq = jobSet(iJob,2);    % cpu cores
    memReq = jobSet(iJob,3);    % GB
    % ignore the very small jobs.
%     if (cpuReq==0)
%         continue
%     else
    arrival_set(jobIdx) = arrivalTime;
    jobIdx = jobIdx +1;
%     end 
    if jobIdx > numOfJobs        
        break;
    end     
end
if jobIdx <= numOfJobs        
    error('out of jobs');
end   


USE_TRACE = true;
if USE_TRACE
    arrival_order = sort(arrival_set);
%     arrival_order = arrival_order - min(arrival_order);
    arrival_order = arrival_order - min(arrival_order);
    arrival_order = max(0, arrival_order - QUEUED_UP_TIME);
else
    pd_normal = makedist('Normal','mu',5000,'sigma',4000);
    arrival_dist = truncate(pd_normal,0,8000);
    arrival_order = random(arrival_dist,1, numOfJobs);
    arrival_order = sort(arrival_order);
end

Folder = 'input/';

outputFile = [Folder OUTPUT];
%%%%% Job input
fileID = fopen(outputFile,'w');
jobIdx= 0;
jobSet = mJobUsage;
numJobIgnored = 0;
cpuReqs = [];
cpuCmplts = [];
gpuCmplts = [];
% p = randperm(NJobs,nUsers);

fprintf(fileID, strInfo);

jobLengthMin = 40;
jobLengthMax = 450;
minVal = 80;
gap = 30;
% minVal = 150;
% gap = 1;
jobLengths = minVal:gap:(minVal+(nUsers-1)*gap);
% jobLengths(1) = jobLengths(nUsers);

for i = 1:nUsers
    pd_normal = makedist('Normal','mu',jobLengths(i),'sigma',40);
    jobLengDist(i) = truncate(pd_normal, jobLengthMin, jobLengthMax);
end

pd_exp = makedist('Exponential','mu',80);
small_job = truncate(pd_exp,40,180);


% std: 1/5 speedupRate and speedupRate;
% speedupRate: [0.4 1.2 3 8 16]
cpu_time_array = zeros(nUsers,nMaxJobs);
gpu_time_array = zeros(nUsers,nMaxJobs);
large_cpu_time = zeros(1,numOfJobs);
large_gpu_time = zeros(1,numOfJobs);
speedupRate_mat = zeros(nUsers,nMaxJobs);

for i = 1: nUsers
    speedupRate_mean = speedupRate(mod(i-1,length(speedupRate))+1);
    speedupRate_std = speedupRate_mean*std_coeff(mod(i-1,length(std_coeff))+1);
    pd = makedist('Normal', 'mu', speedupRate_mean, 'sigma', speedupRate_std);
    t = truncate(pd,0.7,60); % don't use too small speedupRates that favors DRF*
    
    if i<= nSmallJobUser
        speedupRate_mat(i,1:nSmallJobs) = random(t,[1 nSmallJobs]);
    else
        speedupRate_mat(i,1:nLargeJobs) = random(t,[1 nLargeJobs]);
    end
end

speedup_rates = [];
cpu_times = [];
gpu_times = [];
for jobIdx = 1 : numOfJobs        
    
    if (jobIdx <= (nSmallJobUser+nLargeJobUser)*nLargeJobs)
        queue_id = 1 + mod(jobIdx-1, nUsers);
        jobIdInAUser = mod(jobIdx-1, nSmallJobs)+1;
    else
        temp = jobIdx - (nSmallJobUser+nLargeJobUser)*nLargeJobs;
        queue_id = 1 + mod(temp - 1, nSmallJobUser);
        jobIdInAUser = mod(temp-1, nSmallJobs)+1 ;        
    end    

    speedupRate_job = speedupRate_mat(queue_id, jobIdInAUser);    
    
    arrivalTime = arrival_order(jobIdx);    
    
    %% Make user 1 consistenly long. 
    if(queue_id==1)        
        cpu_time = max(jobLengths);                 
    else
        cpu_time = random(jobLengDist(queue_id));        
    end 
    gpu_time = max(1,min(400,cpu_time/speedupRate_job)); 
     
    cpu_time_array(queue_id,fix((jobIdx-1)/nUsers)+1) = cpu_time;
    gpu_time_array(queue_id,fix((jobIdx-1)/nUsers)+1) = gpu_time;
    large_cpu_time(jobIdx) = cpu_time;
    large_gpu_time(jobIdx) = gpu_time;

  
   speedup_rates(jobIdx) = cpu_time/gpu_time;

    strQueue = ['queue' num2str(queue_id-1) ];
    strJob = sprintf('# %d\n', jobIdx-1); 
    
    
    arrivalTimes(jobIdx) = arrivalTime;
    strJob = [strJob sprintf('1 %d 1 %0.0f %s\n', jobIdx-1, arrivalTime, strQueue)];
    cpuReq = 32;
    memReq = randi(16);
    gpuReq = 1;
    gpuMem = 2;
    
     strTemp = sprintf('stage -1.0 %0.1f %0.0f %0.0f %0.0f %0.0f %0.0f 1\n', ...
         cpuReq, ...
        memReq, cpu_time, gpuReq, gpuMem, gpu_time);

    strJob = [strJob strTemp];
    strJob = [strJob '0\n'];
    fprintf(fileID, strJob);
end
fclose(fileID);

outputFile
close all;
% hist(arrival_order,100)
hist(arrivalTimes,500);
title('arrivals');
figure;
hist(speedup_rates,50)
title('speedup rates');
cpuJobsRate = sum(speedup_rates<1)/length(speedup_rates)
return;