clear all; close all; clc;

numOfJobs = 100; numUsers= 30; % number of users
scenario = 5;
alpha = 2;
%% large beta & compuation bound
if scenario==1
  betaMean = 1.5;  betaMax = 3; betaMin= 0.1; % beta = 10^beta
  betaStd = 0.75;
  
  ratioMean = 2*ones(1, numUsers);  % -ratioMax; % ratio= 2^ratio
  ratioMax=3; ratioMin =0; %ratioMax = 3; ratioMin = -3;
  ratioStd = 0.75;
elseif scenario==2
%% large beta & mix bound
  betaMean = 1.5;  betaMax = 3; betaMin= 0.1; % beta = 10^beta
  betaStd = 0.75;
  
  ratioMean = 0*ones(1, numUsers);  % -ratioMax; % ratio= 2^ratio
  ratioMax = 3; ratioMin = -3;
  ratioStd = 1.5;
elseif scenario==3
%% small beta & computation bound
  betaMean = -0.5;  betaMax = -0.05; betaMin= -1; % beta = 10^beta
  betaStd = 0.25;
  
  ratioMean = 2*ones(1, numUsers);  % -ratioMax; % ratio= 2^ratio
  ratioMax=3; ratioMin =0; %ratioMax = 3; ratioMin = -3;
  ratioStd = 0.75;
%% small beta & mix bound
elseif scenario==4  
  betaMean = -0.5;  betaMax = 0; betaMin= -1; % beta = 10^beta
  betaStd = 0.25;
  
  ratioMean = 0*ones(1, numUsers);  % -ratioMax; % ratio= 2^ratio
  ratioMax = 3; ratioMin = -3;
  ratioStd = 1.5;
%% mix beta & compuation bound
elseif scenario==5
  betaMean = 1;  betaMax = 3; betaMin = -1; % beta = 10^beta
  betaStd = 1;
  
  ratioMean = 2*ones(1, numUsers);  % -ratioMax; % ratio= 2^ratio
  ratioMax=3; ratioMin =0; %ratioMax = 3; ratioMin = -3;
  ratioStd = 0.75;
%% mix beta & mix bound
elseif scenario==6
  betaMean = 1;  betaMax = 3; betaMin = -1; % beta = 10^beta
  betaStd = 1;
  
  ratioMean = 0*ones(1, numUsers);  % -ratioMax; % ratio= 2^ratio
  ratioMax = 3; ratioMin = -3;
  ratioStd = 1.5;

elseif scenario==7
  betaMean = 1;  betaMax = 3; betaMin = -1; % beta = 10^beta
  betaStd = 1;
  
  ratioMean = 0*ones(1, numUsers);  % -ratioMax; % ratio= 2^ratio
  ratioMax = 3; ratioMin = -3;
  ratioStd = 1.5;
end
interArrivalMean = 10; interArrivalMin = 0; interArrivalMax = 20; % these are not used.
arrivalStd = 2;

% numIteraionMean = 10; numIteraionMin = 10; numIteraionMax = 10;
numIteraionMean = 5; numIteraionMin = 5; numIteraionMax = 5;
numIteraionStd = 2; 

numTasksMean = 10; numTasksMin = 10; numTasksMax = 10;
numTasksStd = 2; 

%% beta
betaVals = betaStd.*randn(numUsers, 1) + betaMean;
betaVals = max(betaVals,betaMin);
betaVals = min(betaVals,betaMax);
betaVals = 10.^(betaVals);
betas = '{';
betaVals = sort(betaVals,'descend');
for i=1:numUsers  
    betas = [betas  num2str(betaVals(i)) ','];    
end
betas = [betas '};'];
fid=fopen('betas.txt','w');
fprintf(fid, betas);
fprintf(fid, '\n');
fclose(fid);
betas


%% inter Arrival Time
y = arrivalStd.*randn(numUsers, numOfJobs) + interArrivalMean;
y = max(y,interArrivalMin);
y = min(y,interArrivalMax);
interArrivals = '{';
for i=1:numUsers
  interArrivals = [interArrivals '{'];
  for j =1:numOfJobs
    interArrivals = [interArrivals  num2str(y(i,j)) ','];    
  end
  interArrivals = [interArrivals '},'];
end
interArrivals = [interArrivals '};'];
fid=fopen('arrivals.txt','w');
fprintf(fid, interArrivals);
fprintf(fid, '\n');
fclose(fid);

%% Number of Iterations 
y = numIteraionStd.*randn(numUsers, numOfJobs) + numIteraionMean;
y = max(y,numIteraionMin);
y = min(y,numIteraionMax);
y = round(y);
numIterations = '{';
for i=1:numUsers
  numIterations = [numIterations '{'];
  for j =1:numOfJobs
    numIterations = [numIterations  num2str(y(i,j)) ','];    
  end
  numIterations = [numIterations '},'];
end
numIterations = [numIterations '};'];
fid=fopen('iterations.txt','w');
fprintf(fid, numIterations);
fprintf(fid, '\n');
fclose(fid);
numIterations

%% Number of Tasks
y = numTasksStd.*randn(numUsers, numOfJobs) + numTasksMean;
y = max(y,numTasksMin);
y = min(y,numTasksMax);
numTasks = '{';
for i=1:numUsers
  numTasks = [numTasks '{'];
  for j =1:numOfJobs
    numTasks = [numTasks  num2str(y(i,j)) ','];    
  end
  numTasks = [numTasks '},'];
end
numTasks = [numTasks '};'];
fid=fopen('tasks.txt','w');
fprintf(fid, numTasks);
fprintf(fid, '\n');
fclose(fid);
numTasks

%% Number of Tasks
y = 10.*ones(numUsers, 1);
jobSizes = '{';
for i=1:numUsers
  jobSizes = [jobSizes  num2str(y(i)) ','];  
%   jobSizes = [jobSizes '{'];
%   for j =1:numOfJobs
%     jobSizes = [jobSizes  num2str(y(i,j)) ','];    
%   end
%   jobSizes = [jobSizes '},'];
end
jobSizes = [jobSizes '};'];
fid=fopen('jobSizes.txt','w');
fprintf(fid, jobSizes);
fprintf(fid, '\n');
fclose(fid);
jobSizes

%% cpu to mem ratios
cpustomemratios = '{';
for i=1:numUsers
  y = ratioStd.*randn(1, numOfJobs) + ratioMean(i);
  y = max(y,ratioMin);
  y = min(y,ratioMax);  
  cpustomemratios = [cpustomemratios '{'];
  for j =1:numOfJobs
    cpustomemratios = [cpustomemratios  num2str(2^y(j)) ','];    
  end
  cpustomemratios = [cpustomemratios '},'];
end
cpustomemratios = [cpustomemratios '};'];
fid=fopen('cpustomemratios.txt','w');
fprintf(fid, cpustomemratios);
fprintf(fid, '\n');
fclose(fid);
cpustomemratios

%% deep learning jobs: cpu to mem ratios
largecputomem = '{';
for i=1:numUsers
  y = betaVals(i); 
  largecputomem = [largecputomem '{'];
  for j =1:numOfJobs
    largecputomem = [largecputomem  num2str(y) ','];    
  end
  largecputomem = [largecputomem '},'];
end
largecputomem = [largecputomem '};'];
fid=fopen('largecputomem.txt','w');
fprintf(fid, largecputomem);
fprintf(fid, '\n');
fclose(fid);
largecputomem

%% CPU completion time errors
cpuCmpltStd = 50;
cpuCmpltMean = 0;
cpuVals = cpuCmpltStd.*randn(numOfJobs, 1) + cpuCmpltMean;
cpuMin = -149;
cpuMax =  149;
cpuVals = max(cpuVals, cpuMin);
% cpuVals = min(cpuVals, cpuMax);
cpuCmpltErrors = '{';
for j =1:numOfJobs
  y = cpuVals(j);
  cpuCmpltErrors = [cpuCmpltErrors  num2str(y) ','];    
end
cpuCmpltErrors = [cpuCmpltErrors '};'];
fid=fopen('cpuCmpltErrors.txt','w');
fprintf(fid, cpuCmpltErrors);
fprintf(fid, '\n');
fclose(fid);
cpuCmpltErrors
