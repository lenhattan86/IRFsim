clear; close all;
addpath('matlab_func');
common_settings;

workload='SIMPLE';

num_batch_queues = 3;
num_interactive_queue = 0;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 10;  STEP_TIME = 1;
is_printed = true;
cluster_size = 10;

enableSeparateLegend = true;

scale_down_mem = 1;

fig_path='../IRF/figs/';

figSize = figSizeOneCol;

extra = '';

%%
result_folder= '';

output_sufix = ''; 

%%
% EC
plots=[1]; 
logfolder = [result_folder 'log/'];

start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*num_queues+1;
endIdx = max_time_step*num_queues;
num_time_steps = max_time_step-start_time_step;
linewidth= 2;
barwidth = 1.0;
timeInSeconds = START_TIME+STEP_TIME:STEP_TIME:END_TIME;

lengendStr = cell(1, num_queues);
for i=1:num_interactive_queue
%     lengendStr{i} = ['LQ-' int2str(num_interactive_queue-i)];
    lengendStr{i} = ['User-' int2str(i-1)];
end

%%
% extraStr = '';
extraStr = ['_' int2str(num_queues) '_' int2str(cluster_size)];
extraStrReject = ['_' int2str(num_queues) '_' int2str(cluster_size)];

%%
prefixes = {'ES', 'EDRF', 'DRF', 'MaxMinMem', 'SpeedUp', 'Pricing'};
% prefixes = {'DRF','EDRF'};
for iFile=1:length(prefixes)
  if plots(1)   
     logFile = [ logfolder prefixes{iFile} '-output' extraStr  '.csv'];
     [queueNames, res1, res2, res3, flag] = importResUsageLog(logFile);   

     colorUsers = {colorUser1; colorUser2; colorUser3; colorWasted};
     stackLabels = {strUser1,strUser2, strUser3,'Unallocated'};
     groupLabels = { strCPU, strGPU, strMemory};  

     if (flag)
        figure;
        [ avgCPU ] = convertToAvgUsage( res1, num_queues, num_time_steps,startIdx);
        [ avgGPU ] = convertToAvgUsage( res2, num_queues, num_time_steps,startIdx);
        [ avgMemory ] = convertToAvgUsage( res3, num_queues, num_time_steps,startIdx);
        avgRes = [[avgCPU cluster_size-sum(avgCPU)]; [avgGPU cluster_size-sum(avgGPU)];[avgMemory cluster_size-sum(avgMemory)]]/cluster_size;      
        stackLabels = {strUser1,strUser2, strUser3,'Unallocated'};
        hBar = bar(1:3, avgRes, 0.5, 'stacked');
        ylabel('Normalized Capacity');
        set(hBar,{'FaceColor'}, colorUsers);      
        legend(stackLabels,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');  
        set(gca,'XTickLabel', groupLabels, 'FontSize', fontAxis);   
        title(prefixes{iFile});
        ylim([0 1]);
        set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);     
        if is_printed   
          figIdx=figIdx +1;
          fileNames{figIdx} = ['avgResourceUsage_' prefixes{iFile}  ];        
          epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
          print ('-depsc', epsFile);
        end   
     end
  end
end

return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i}
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName extra '.pdf']    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
%fileNames