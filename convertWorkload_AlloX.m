%% reset 
clear; close all; clc;
% return;
%% load data

JOB_FILE  = 'jobInfo.mat';
JOB_USAGE = 'jobResUsageWReschedule.mat';
mkdir('figs');

cpuCapacity = 6600;
memCapcity  = 5900;
cpuMemRate  = cpuCapacity/memCapcity;

second = 10^6;

timeScale = 60*5;
%scaleDownArrivalTime = 10;
scaleDownArrivalTime = 10;

load(JOB_FILE);
jobIds = JobInfos(:,1);
scheduleClass =JobInfos(:,4);
complTimes = (JobInfos(:,3) - JobInfos(:,5))/(timeScale*second); % microsecond
temp = [jobIds JobInfos(:,2) JobInfos(:,3) scheduleClass JobInfos(:,5) JobInfos(:,7)];

jobComplInfos = temp(find(complTimes > 0),:);
jobComplInfos = jobComplInfos(find(jobComplInfos(:,2) >= 0),:);
% complTimes = complTimes(find(complTimes>0));

%
load(JOB_USAGE); 
jobUsage = JobInfos;

% 
% for i=1:4
%   class = find(jobComplInfos(:,4) == (i-1));
%   jobs = jobComplInfos(class,1);
%   
%   temp1 = ismember(jobUsage(:,1),jobs); 
%   usageTemp = jobUsage(temp1,:);
%   temp2 = ismember(jobComplInfos(:,1), usageTemp(:,1)); 
%   mJobUsage{i} = [usageTemp jobComplInfos(temp2,2:6)];
% end

temp1 = ismember(jobUsage(:,1), jobComplInfos(:,1)); 
temp2 = ismember(jobComplInfos(:,1), jobUsage(temp1,1)); 
mJobUsage = [jobUsage(temp1,:) jobComplInfos(temp2,2:6)];

%%
END_TIME = 3600;
MeanBeta = 32;

%% Experiment case studies

% simple
if false
    betas = [5 7 12];
    reportBetas = [5 7 12];
    numOfJobs=133*ones(size(betas));
elseif true
% more users
    nUsers = 25;
    pd = makedist('Normal','mu',MeanBeta,'sigma',MeanBeta);
    t = truncate(pd,0,inf);
    betas = random(t, nUsers,1);
    reportBetas = betas;    
    betas = betas / mean(betas) * MeanBeta;    
    
    NJobs = 1000;
    numOfJobs=nUsers*NJobs;
    BETAS = zeros(nUsers, NJobs);
    for iUser = 1:nUsers
        pd = makedist('Normal','mu',betas(iUser),'sigma',betas(iUser));
        t = truncate(pd,0,inf);
        temp = random(t, NJobs,1);          
        BETAS(iUser,:) = temp / mean(temp) * betas(iUser); 
    end
end

%% Get the metadata for users

load('plotJobDemandUser.mat');
Folder = '/home/tanle/projects/IRFsim/input/';
% Folder = './';

% outputFile = [Folder 'jobs_input_' num2str(length(betas)) '_Google.txt'];
% queueFile = [Folder 'queue_input_' num2str(length(betas)) '_Google.txt'];
outputFile = [Folder 'job_google.txt'];
queueFile = [Folder 'queue_google.txt'];

ARRIVAL_TIME_IGNORE = 2.4*24*3600 + 750;    
%% Create Queue Info

fileID = fopen(queueFile,'w');
queueID = 0;
numCoresACPU = 16;
for iC = 1:length(reportBetas)
    %     # 0
    %     queue0 0.0 
    %     1.0
    %     80.0
    %     80.0
    strQueue = ['queue' num2str(queueID)];    
    type=0;  

    queue = ['# ' num2str(queueID) '\n'];        
    queue = [queue strQueue ' ' '0.0 \n'];
    queue = [queue '1.0' ' \n'];
    queue = [queue num2str(betas(iC)*numCoresACPU) ' \n'];
    queue = [queue num2str(reportBetas(iC)*numCoresACPU) ' \n'];

    fprintf(fileID, queue);
    queueID = queueID+1;
end
fclose(fileID);

%% Job input
fileID = fopen(outputFile,'w');
jobIdx= 0;
jobSet = mJobUsage;
numJobIgnored = 0;
cpuReqs = [];
cpuCmplts = [];
gpuCmplts = [];
for iJob=1:length(jobSet(:,1))    
    %1 0 100 bursty0
    complTime = jobSet(iJob,6) - jobSet(iJob,8);
    complTime = complTime/10^6/timeScale;
    arrivalTime = jobSet(iJob,5)/10^6;

    if (arrivalTime < ARRIVAL_TIME_IGNORE) %|| (complTime > LONG_JOB_IGNORE)
      continue;
    end
    
    arrivalTime = arrivalTime - ARRIVAL_TIME_IGNORE;
    arrivalTime = arrivalTime/timeScale;

    arrivalTime = arrivalTime/scaleDownArrivalTime;

    % # 2
    % 1 2 1 7 queue2
    % stage 1.0 16.0 6.0 150.0 1.0 2.0 12.5 12.0 1
    % 0

    %# 0
    %     userID = mJobUsage{i}(iJob,9);

    strQueue = ['queue' num2str( mod(jobIdx,length(betas)) ) ];
    strJob = sprintf('# %d\n', jobIdx); 
    strJob = [strJob sprintf('1 %d 1 %0.0f %s\n', jobIdx, arrivalTime, strQueue)];

    cpuReq = jobSet(iJob,2);    % cpu cores
    memReq = jobSet(iJob,3);    % GB
    
    taskPeriod = complTime;
    if (cpuReq==0)
        continue
    end
    
    if (cpuReq <= 2)
        memReq = 2/cpuReq*memReq;
        cpuReq = 2;        
    end
    if (cpuReq >= 64)
        memReq = 64/cpuReq*memReq;
        cpuReq = 64;        
    end
    

    taskPeriod = ceil(taskPeriod);
    if taskPeriod > 200
       taskPeriod = 200 - rand(1)*100;
    end
    %     numTasks = numTasks*scale;    
    %     % stage 1.0 16.0 6.0 150.0 1.0 2.0 12.5 12.0 1
    gpuMem= 2;
    gpuReq = 1;
%     beta=betas(mod(jobIdx,length(betas))+1);
    beta = BETAS(ceil((jobIdx+1)/NJobs), mod(jobIdx,NJobs)+1);
    gpuCmplt = taskPeriod/beta * (cpuReq/gpuReq);
    
    if gpuCmplt > 200
       gpuCmplt = 200 - rand(1)*100;
    end
    
    strTemp = sprintf('stage -1.0 %0.1f %0.0f %0.0f %0.0f %0.0f %0.0f 1\n', ...
         cpuReq, ...
        memReq, taskPeriod, gpuReq, gpuMem, gpuCmplt );

    strJob = [strJob strTemp];
    strJob = [strJob '0\n'];
    

    %     if (cpuReq > 1 && gpuCmplt> 1 && taskPeriod < 200)
%     if (cpuReq >= 0.1 && gpuCmplt> 1 && gpuCmplt < 200)    
%     if (gpuCmplt> 1 && gpuCmplt < 200)    
%     if (gpuCmplt> 1)
    if (cpuReq >= 0.1 && gpuCmplt>= 1)
        jobIdx = jobIdx + 1;
        fprintf(fileID, strJob);
        cpuReqs = [cpuReqs cpuReq];
        cpuCmplts = [cpuCmplts taskPeriod];
        gpuCmplts = [gpuCmplts gpuCmplt];
    else
        numJobIgnored = numJobIgnored + 1;
    end
    if jobIdx >= sum(numOfJobs)
      break;
    end    
end
fclose(fileID);
outputFile
return;
%% Analyze the trace
close all;
disp('Number of jobs are ignored');
numJobIgnored
arrivalTime
fprintf('arrivalTime= %d\n', arrivalTime);

% figure for betas
fprintf('mean(betas)= %d\n', mean(betas));
figure; hist(betas);

% figure 
fprintf('mean(cpuCmplts)= %d\n', mean(cpuCmplts* (timeScale/60)));
figure; hist(cpuCmplts * (timeScale/60), 100);
ylabel('jobs');xlabel('mins');
% figure 
fprintf('mean(gpuCmplts)= %d\n', mean(gpuCmplts* (timeScale/60)));
figure; hist(gpuCmplts * (timeScale/60), 100);
ylabel('jobs');xlabel('mins');
