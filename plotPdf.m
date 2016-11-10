clear; close all; clc;
addpath('matlab_func');
common_settings;
is_printed = true;

MAX_DUR = 350;

inputFile = 'pdf/queries_bb_FB_distr.csv'; workload='BB';
% inputFile = 'pdf/queries_tpch_FB_distr.csv'; workload='TPC-H';
% inputFile = 'pdf/queries_tpcds_FB_distr_new.csv'; workload='TPC-DS';
[durations,num_tasks] = importAllTaskInfo(inputFile);

fig_path = ['/home/tanle/projects/EuroSys17/fig/' workload '-'];

allDurations = zeros(sum(num_tasks),1);

idx = 0;
for i=1:length(num_tasks)
    for j=1:num_tasks(i)
        idx=idx+1;
        allDurations(idx) = durations(i);
    end
end

allDurations = allDurations(allDurations<MAX_DUR);
hist(allDurations, 100);

xLabel='task duration (secs)';
yLabel='Number of tasks';


figSize = [0.0 0 5.0 3.0];
set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);
xlabel(xLabel,'FontSize',fontAxis);
ylabel(yLabel,'FontSize',fontAxis);
xlim([0 MAX_DUR]);
set(gca,'FontSize',fontAxis);

if is_printed
   figIdx=figIdx +1;
   fileNames{figIdx} = 'hist';
   epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
   print ('-depsc', epsFile);
end

return;
%% convert to pdf

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
