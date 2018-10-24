clear all; close all; clc;

numOfJobs = 1000;

minVal = -1;
maxVal = 1;
stdev = 0.1;
meanVal = 0;

%%
pd = makedist('Normal', 'mu', meanVal, 'sigma', stdev);
t = truncate(pd,-0.99,0.99); % don't use too small speedupRates that favors DRF*
cpuErrs = random(t,[1 numOfJobs]);
gpuErrs = random(t,[1 numOfJobs]);    

fid=fopen('err.txt','w');
cpuErrors = 'public double[] cpuErrs = {';
gpuErrors = 'public double[] gpuErrs = {';
for i=1:numOfJobs 
  cpuErrors = [cpuErrors num2str(cpuErrs(i)) ',']; 
  gpuErrors =  [gpuErrors num2str(gpuErrs(i)) ',']; 
  if (mod(i,100)==0)
      cpuErrors = [cpuErrors '\n']; 
      gpuErrors =  [gpuErrors '\n'];
  end
end
cpuErrors = [cpuErrors '};'];
fprintf(fid, '}');
gpuErrors = [gpuErrors '};'];


fid=fopen('err.txt','wt');
fprintf(fid, cpuErrors);
fprintf(fid, '\n');
fprintf(fid, gpuErrors);

fclose(fid);
