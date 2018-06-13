clear; close all;
addpath('matlab_func');
common_settings;

workload='SIMPLE';

num_queues = 3;
START_TIME = 0; END_TIME = 10;  STEP_TIME = 1;
is_printed = false;

isLarge = false;
largeStr = '';
if isLarge
  cluster_size = 600;
  num_queues = 30;
  END_TIME = 4;
  largeStr='_mbeta_lcpu';
else
  cluster_size = 60; 
  END_TIME = 10;
  capacity = cluster_size * [64 1 96];
  largeStr='';
end
stackLabels = {strUser1,strUser2, strUser3, 'Unallocated'};
colorUsers = {colorUser1; colorUser2; colorUser3; colorWasted};
     
enableSeparateLegend = true;

scale_down_mem = 1;

% fig_path='../IRF/figs/';

figSize = figSizeHalfCol;


isTitle=false;

%%
result_folder= '';

output_sufix = ''; 

%%
% EC
plots=[1 0]; 
logfolder = [result_folder 'log/'];

start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*num_queues+1;
endIdx = max_time_step*num_queues;
num_time_steps = max_time_step-start_time_step;
linewidth= 2;
barwidth = 1.0;
timeInSeconds = START_TIME+STEP_TIME:STEP_TIME:END_TIME;

%%

extraStr = ['_' int2str(num_queues) '_' int2str(cluster_size) largeStr];
extraStrReject = ['_' int2str(num_queues) '_' int2str(cluster_size)];

%%
prefixes = {'ES', 'DRF', 'AlloX'};
methods = {'ES', 'DRF', 'AlloX'};
% prefixes = {'MaxMinMem'};
for iFile=1:length(prefixes)
  if plots(1)   
     logFile = [ logfolder prefixes{iFile} '-output' extraStr  '.csv'];
     [queueNames, res1, res2, res3, flag] = importResUsageLog(logFile);   

     groupLabels = { strCPU, strGPU, strMemory};  

     if (flag)
        figIdx=figIdx +1;
        figures{figIdx} = figure;
        [ avgCPU ] = convertToAvgUsage( res1, num_queues, num_time_steps,startIdx);
        [ avgGPU ] = convertToAvgUsage( res2, num_queues, num_time_steps,startIdx);
        [ avgMemory ] = convertToAvgUsage( res3, num_queues, num_time_steps,startIdx);
        avgRes = [[avgCPU capacity(1)-sum(avgCPU)]; [avgGPU capacity(2)-sum(avgGPU)];[avgMemory capacity(3)-sum(avgMemory)]]./(capacity'*ones(1,num_queues+1));
        hBar = bar(1:3, avgRes, 0.5, 'stacked');
        ylabel(strNormCapacity);
%         if ~isLarge
          set(hBar,{'FaceColor'}, colorUsers);      
%         end
%         legend(stackLabels,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');  
        set(gca,'XTickLabel', groupLabels, 'FontSize', fontAxis);
        if isTitle
          title(prefixes{iFile});
        end
        ylim([0 1]);
        xlim([0.5 3.5]);
        set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);     
        
        fileNames{figIdx} = ['avgResourceUsage_' prefixes{iFile}  ];                

     end
  end
end

%% create dummy graph with legends
legendSize = [0.0 0 6 0.4];

if ~isLarge
  if enableSeparateLegend
    figIdx=figIdx +1;
    figures{figIdx} = figure;
    hBar = bar(1:3, avgRes,barwidth,'stacked');

    set(hBar,{'FaceColor'}, colorUsers);      

    legend(stackLabels,'Location','southoutside','FontSize',fontLegend,'Orientation','horizontal');
    set(gca,'FontSize',fontSize);
    axis([20000,20001,20000,20001]) %move dummy points out of view
    axis off %hide axis  
    set(gca,'YColor','none');
    set (gcf, 'Units', 'Inches', 'Position', legendSize, 'PaperUnits', 'inches', 'PaperPosition', legendSize);    

%     if is_printed   
%         figIdx=figIdx +1;
      fileNames{figIdx} = ['q' int2str(num_queues)  'avg_res_usage_legend'];        
%       epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
%       print ('-depsc', epsFile);
%     end
  end
end

%% System utilization
for iFile=1:length(prefixes)
  if plots(2)   
     logFile = [ logfolder prefixes{iFile} '-output' extraStr  '.csv'];
     [queueNames, res1, res2, res3, flag] = importResUsageLog(logFile);   

     colorUsers = {colorUser1; colorWasted};
     stackLabels = {'allocated', 'unallocated'};
     groupLabels = { strCPU, strGPU, strMemory};  

     if (flag)
        figIdx=figIdx +1;
        figures{figIdx} =figure;
        [ avgCPU ] = convertToAvgUsage( res1, num_queues, num_time_steps,startIdx);
        [ avgGPU ] = convertToAvgUsage( res2, num_queues, num_time_steps,startIdx);
        [ avgMemory ] = convertToAvgUsage( res3, num_queues, num_time_steps,startIdx);
        avgRes = [[sum(avgCPU) cluster_size-sum(avgCPU)]; [sum(avgGPU) cluster_size-sum(avgGPU)];[sum(avgMemory) cluster_size-sum(avgMemory)]]/cluster_size;              
        hBar = bar(1:3, avgRes, 0.5, 'stacked_mbeta_lcpu');
        ylabel(strNormCapacity);        
          set(hBar,{'FaceColor'}, colorUsers);              
%         legend(stackLabels,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');  
        set(gca,'XTickLabel', groupLabels, 'FontSize', fontAxis);
        if isTitle
          title(prefixes{iFile});
        end
        ylim([0 1]);
        set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);     
         fileNames{figIdx} = ['res_util_' prefixes{iFile}  ];        
     end
  end
end
return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);
    
    pdfFile = [ fig_path fileName largeStr '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
%fileNames