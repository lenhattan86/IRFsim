clear; close all; clc;
nUsers = 10;
% speedupRate = [33 40 42 44 32*3 32*5 32*6 32*7 32*8 32*15]/32;  % 32 cores per node vs 1 GPU 
speedupRate = [33 40  32*2 32*3 32*3 32*5 32*6 32*7 32*8 32*15]/32;  % 32 cores per node vs 1 GPU 
% speedupRate = [33 40 32*2 32*8 32*15]/32;  % 32 cores per node vs 1 GPU 
% speedupRate = [33 40 42 32*2 32*3 32*5 32*6 32*7 32*8 32*10]/32;  % 32 cores per node vs 1 GPU 
% speedupRate = [33 40 32*3.3 32*3 32*3 32*5 32*6 32*7 32*8 32*15]/32;  % 32 cores per node vs 1 GPU 
% std_coeff = [1/6 1/3 1 1.5];
std_coeff = [1 1 1 1]*1.2;
NJobs = 1000;
numOfJobs = nUsers*NJobs;

google_path = '/home/tanle/projects/ClusterData2011_2/';
JOB_FILE  = [google_path 'jobInfo.mat'];
load(JOB_FILE);
jobIds = JobInfos(:,1);
scheduleClass =JobInfos(:,4);
TIME_SCALE = 1;
second = 10^6;
% timeScale = 60*5;
% timeScale = 50; % 50: 27% - 91%
% timeScale = 55; % 50: 27% - 91%
timeScale = 55;
% timeScale = 75;

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
    if (cpuReq==0)
        continue
    else
        arrival_set(jobIdx) = arrivalTime;
        jobIdx = jobIdx +1;
    end 
    if jobIdx > sum(numOfJobs)
        break;
    end     
end

USE_TRACE = true;
if USE_TRACE
    arrival_order = sort(arrival_set);
else
    pd_normal = makedist('Normal','mu',5000,'sigma',4000);
    arrival_dist = truncate(pd_normal,0,8000);
    arrival_order = random(arrival_dist,1, numOfJobs);
    arrival_order = sort(arrival_order);
end

Folder = 'input/';
outputFile = [Folder 'job_google.txt'];
%%%%% Job input
fileID = fopen(outputFile,'w');
jobIdx= 0;
jobSet = mJobUsage;
numJobIgnored = 0;
cpuReqs = [];
cpuCmplts = [];
gpuCmplts = [];

p = randperm(NJobs,nUsers);

% small jobs: exponential with mean 100;

pd_exp = makedist('Exponential','mu',80);
small_job = truncate(pd_exp,40,180);

% large jobs: normal distribution with mean 200, std 60, truncated at 300;
pd_normal = makedist('Normal','mu',200,'sigma',20);
large_job = truncate(pd_normal,80,300);

% this also works @ 25%
% small_job = truncate(pd_exp,80,250);
% large_job = truncate(pd_exp,80,250);


% std: 1/5 speedupRate and speedupRate;
% speedupRate: [0.4 1.2 3 8 16]
cpu_time_array = zeros(nUsers,NJobs);
gpu_time_array = zeros(nUsers,NJobs);
large_cpu_time = zeros(1,numOfJobs);
large_gpu_time = zeros(1,numOfJobs);
speedupRate_mat = zeros(nUsers,NJobs);
for i = 1: nUsers
    speedupRate_mean = speedupRate(mod(i-1,length(speedupRate))+1);
    speedupRate_std = speedupRate_mean*std_coeff(mod(i-1,length(std_coeff))+1);
    pd = makedist('Normal', 'mu', speedupRate_mean, 'sigma', speedupRate_std);
    t = truncate(pd,0.7,60); % don't use too small speedupRates that favors DRF*
    speedupRate_mat(i,:) = random(t,[1 NJobs]);
end
% for i = 1: nUsers
%     speedupRate_mean_log = log(speedupRate(mod(i-1,length(speedupRate))+1));
%     speedupRate_std = speedupRate_mean_log*std_coeff(mod(i-1,length(std_coeff))+1);
%     pd = makedist('Normal', 'mu', speedupRate_mean_log, 'sigma', speedupRate_std);
%     t = truncate(pd,log(0.7),log(60)); % don't use too small speedupRates that favors DRF*
% 
%     speedupRate_mat(i,:) = 2.^(random(t,[1 NJobs]));
% end

speedup_rates = [];
cpu_times = [];
gpu_times = [];
for jobIdx = 1 : numOfJobs    
    queue_id = 1 + mod(jobIdx-1,nUsers);  %will -1 later
    
%     if queue_id <=nUsers/2
%         cpu_time = random(small_job);
%     else
%         cpu_time = random(large_job);   
%     end

%     speedupRate_job = speedupRate_mat(ceil(jobIdx/NJobs),mod(jobIdx-1,NJobs)+1);    
%     speedupRate_job = speedupRate_mat(ceil(jobIdx/NJobs), ceil(jobIdx/nUsers));    
    speedupRate_job = speedupRate_mat(mod(jobIdx-1,nUsers)+1, mod(jobIdx-1,NJobs)+1);    
        
    if speedupRate_job <=2
        cpu_time = random(small_job);
    else
        cpu_time = random(large_job);   
    end
    
     gpu_time = max(1,min(400,cpu_time/speedupRate_job));    
     
%      if (speedupRate_job < 1) % this makes ES perform worse
%          gpu_time = gpu_time / 2;
%          cpu_time = cpu_time / 2;
%      end

    cpu_time_array(queue_id,fix((jobIdx-1)/nUsers)+1) = cpu_time;
    gpu_time_array(queue_id,fix((jobIdx-1)/nUsers)+1) = gpu_time;
    large_cpu_time(jobIdx) = cpu_time;
    large_gpu_time(jobIdx) = gpu_time;

    % scaling wrt time
%      cpu_time = (1.5-jobIdx/numOfJobs)*cpu_time;
%      gpu_time = (1.5-jobIdx/numOfJobs)*gpu_time;
   % seedd = randperm(numOfJobs);
   
   speedup_rates(jobIdx) = cpu_time/gpu_time;

    strQueue = ['queue' num2str(queue_id-1) ];
    strJob = sprintf('# %d\n', jobIdx-1); 
    strJob = [strJob sprintf('1 %d 1 %0.0f %s\n', jobIdx-1, arrival_order(jobIdx), strQueue)];
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
hist(arrival_order,50)
title('arrivals');
figure;
hist(speedup_rates,50)
title('speedup rates');
cpuJobsRate = sum(speedup_rates<1)/length(speedup_rates)
return;