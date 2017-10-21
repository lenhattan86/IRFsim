clear all; close all; clc;
numOfJobs = 100;
numUsers= 30; % number of users

interArrivalMean = 10;
arrivalStd = 2;
interArrivalMin = 0;
interArrivalMax = 20;

numIteraionMean = 10;
numIteraionStd = 2;
numIteraionMin = 0;
numIteraionMax = 20;

betaMean = 0; 
betaMax = 3;
betaMin = -3;
betaStd = 10;

%% beta
y = betaStd.*randn(numUsers, 1) + betaMean;
y = max(y,betaMin);
y = min(y,betaMax);
betas = '{';
for i=1:numUsers  
    betas = [betas  num2str(10^y(i)) ','];    
end
betas = [betas '};'];
fid=fopen('betas.txt','w');
fprintf(fid, betas);
fprintf(fid, '\n');


%% inter Arrival Time
y = arrivalStd.*randn(numUsers, numOfJobs) + interArrivalMean;
y = max(y,interArrivalMin);
y = min(y,interArrivalMax);
interArrivals = '{';
for i=1:numUsers
  for j =1:numOfJobs
    interArrivals = [interArrivals  num2str(y(i,j)) ','];    
  end
  interArrivals = [interArrivals ';'];
end
interArrivals = [interArrivals '};'];
fid=fopen('arrivals.txt','w');
fprintf(fid, interArrivals);
fprintf(fid, '\n');

%% Number of Iterations 
y = numIteraionStd.*randn(numUsers, numOfJobs) + numIteraionMean;
y = max(y,numIteraionMin);
y = min(y,numIteraionMax);
numIterations = '{';
for i=1:numUsers
  for j =1:numOfJobs
    numIterations = [numIterations  num2str(y(i,j)) ','];    
  end
  numIterations = [numIterations ';'];
end
numIterations = [numIterations '};'];
fid=fopen('arrivals.txt','w');
fprintf(fid, numIterations);
fprintf(fid, '\n');