clear all; close all; clc;

numOfJobs = 150;
numDimemsion = 6;

minVal = -1;
maxVal = 1;
stdev = 0.1;
meanVal = 0;

y = stdev.*randn(numOfJobs, numDimemsion) + meanVal;
% y = stdev.*trandn(minVal*ones(numOfJobs, numDimemsion), maxVal*ones(numOfJobs, numDimemsion));

y = max(y,minVal);
y = min(y,maxVal);

errors = '{';
for i=1:numOfJobs
  jobDemand = '{';
  for j=1:numDimemsion
    jobDemand = [jobDemand  num2str(y(i,j)) ','];    
  end
  jobDemand = [jobDemand '}'];
  errors = [errors jobDemand ',']; 
end
errors = [errors '}'];

errors