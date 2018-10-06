clear; close all; clc;
nUsers = 10;
% beta= [0.4 0.8 1.2 4 16];
beta = [12 40 40 96 512]/32;  % 32 cores per node vs 1 GPU 
std_coeff = [1/6 1/3 1 1.5];
NJobs = 50;
numOfJobs = nUsers*NJobs;


google_path = '/home/tanle/projects/ClusterData2011_2/';
JOB_FILE  = [google_path 'jobInfo.mat'];
load(JOB_FILE);
jobIds = JobInfos(:,1);
scheduleClass =JobInfos(:,4);
TIME_SCALE = 1;
second = 10^6;
% timeScale = 60*5;
timeScale = 60;

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

arrival_order = sort(arrival_set);

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


% std: 1/5 beta and beta;
% beta: [0.4 1.2 3 8 16]
cpu_time_array = zeros(nUsers,NJobs);
gpu_time_array = zeros(nUsers,NJobs);
large_cpu_time = zeros(1,numOfJobs);
large_gpu_time = zeros(1,numOfJobs);
beta_mat = zeros(nUsers,NJobs);
for i = 1: nUsers
    beta_mean = beta(ceil(i/4));
    beta_std = beta_mean*std_coeff(mod(i-1,4)+1);
    pd = makedist('Normal','mu',beta_mean,'sigma',beta_std);
    t = truncate(pd,0.1,60);  
    beta_mat(i,:) = random(t,[1 NJobs]);
end



for jobIdx = 1 : numOfJobs    
    queue_id = 1 + mod(jobIdx-1,nUsers);  %will -1 later
    
    if queue_id <11
        cpu_time = random(small_job);
    else
        cpu_time = random(large_job);   
    end
%         beta_mean = beta(mod(queue_id-1,5)+1);
%     
%         if mod(queue_id,10)>0 && mod(queue_id,10)<6
%             beta_std = std_coeff(1)*beta_mean;
%         else
%              beta_std = std_coeff(2)*beta_mean;
%         end
% 
%         pd = makedist('Normal','mu',beta_mean,'sigma',beta_std);
%         t = truncate(pd,0.1,60);  
%         beta_job = random(t);  
%         gpu_time = min(400,cpu_time/beta_job);
    beta_job = beta_mat(ceil(jobIdx/NJobs),mod(jobIdx-1,NJobs)+1);
     gpu_time = max(1,min(400,cpu_time/beta_job));       

    cpu_time_array(queue_id,fix((jobIdx-1)/nUsers)+1) = cpu_time;
    gpu_time_array(queue_id,fix((jobIdx-1)/nUsers)+1) = gpu_time;
    large_cpu_time(jobIdx) = cpu_time;
    large_gpu_time(jobIdx) = gpu_time;

    % scaling wrt time
%      cpu_time = (1.5-jobIdx/numOfJobs)*cpu_time;
%      gpu_time = (1.5-jobIdx/numOfJobs)*gpu_time;
   % seedd = randperm(numOfJobs);

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
return;
