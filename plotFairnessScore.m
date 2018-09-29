clear; close all;
addpath('matlab_func');
common_settings;
figureSize = figSizeThreeFourth;
is_printed = true;

num_batch_queues = 15;
num_interactive_queue = 0;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 2000;  STEP_TIME = 1;
cluster_size = 10;

enableSeparateLegend = false;
scale_down_mem = 1;
USER_SET = 1:3;
USER_ID = 3;

% fig_path='../IRF/figs/';

%%
result_folder= '';
output_sufix = ''; 
%%
% EC
plots=[0, 0, 1]; 
logfolder = [result_folder 'log/'];

start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*num_queues+1;
endIdx = max_time_step*num_queues;
num_time_steps = max_time_step-start_time_step;
linewidth= 2;
barwidth = 1.0;
timeScale = 1;
timeInSeconds = START_TIME+STEP_TIME:STEP_TIME:END_TIME;
timeInSeconds = timeInSeconds * timeScale;

lengendStr = cell(1, num_queues);
for i=1:num_interactive_queue
%     lengendStr{i} = ['LQ-' int2str(num_interactive_queue-i)];
    lengendStr{i} = ['LQ-' int2str(i-1)];
end
for i=1:num_batch_queues
    lengendStr{i+num_interactive_queue} = ['User ' int2str(i-1)];
end
MAX_SCORE = 1;
%%
% extraStr = '';
extraStr = ['_' int2str(num_batch_queues) '_' int2str(cluster_size)];

%%
% prefixes = {'DRF', 'ES', 'DRFExt', 'AlloX', 'SJF'};
% prefixes = {'DRFFIFO','DRF','ES', 'DRFExt', 'FS', 'SRPT'};
% methods = {'DRFF','DRF','ES', 'DRFExt', 'AlloX', 'SRPT'};
prefixes = {'DRFFIFO','SRPT', 'FS'};
methods = {'DRFF', 'SRPT','AlloX'};
fairscoreLineStyles = {lineDRF, lineSRPT, lineAlloX};
JFIs = zeros(1,length(prefixes));
FAIR_SCORES ={};
for iFile=1:length(prefixes)
  if plots(1)   
     logFile = [ logfolder prefixes{iFile} '-output' extraStr  '.csv'];
     [queueNames, res1, res2, res3, fairScores, flag] = importResUsageLog(logFile);
     if (flag)
        figure;        
        resAll = zeros(1,num_queues*num_time_steps);
        temp = fairScores(startIdx:length(fairScores));
        if(length(resAll)>length(temp))
           resAll(1:length(temp)) = temp;
        else
           resAll = temp(1:num_queues*num_time_steps);
        end
        fairScoreUsers = reshape(resAll, num_queues, num_time_steps);        
        FAIR_SCORES{iFile} = fairScoreUsers;
        meanFairScore = mean(fairScoreUsers');
        JFIs(iFile) = sum(meanFairScore)^2 / (num_queues*sum(meanFairScore.^2));
        % hBar = bar(timeInSeconds,fairScoreUsers',barwidth,'stacked','EdgeColor','none');      
        hBar = plot(timeInSeconds,fairScoreUsers(USER_SET, :)');  
        ylabel(strFairScore);
        xlabel(strTime);
        xlim([0 max(timeInSeconds)]);
        ylim([0 MAX_SCORE]);
        %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
%         title(prefixes{iFile},'fontsize',fontLegend);
        
        set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
        if is_printed         
          figIdx=figIdx +1;
          fileNames{figIdx} = ['q' int2str(num_batch_queues) '_res_usage_' prefixes{iFile}];         
          epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
          print ('-depsc', epsFile);
        end
     end
  end
end
%%
% plot Jain's fairness index
 if plots(2)   
    
    figure;        
    hold on
    for iFile=1:length(prefixes)        
        hBar = bar(iFile,JFIs(iFile), barWidth);             
    end
    hold off;
    ylabel("Jain's Fairness Index");
    xlabel(strMethods);  
    xlim([0.6 length(prefixes)+0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    if is_printed         
      figIdx=figIdx +1;
      fileNames{figIdx} = ['jain_fairness_index'];         
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
      print ('-depsc', epsFile);
    end
 end
 %%
 if plots(3)   
     figure;   
 hold on;  

    for iFile=1:length(prefixes)
  
     logFile = [ logfolder prefixes{iFile} '-output' extraStr  '.csv'];
     [queueNames, res1, res2, res3, fairScores, flag] = importResUsageLog(logFile);
     if (flag)             
        resAll = zeros(1,num_queues*num_time_steps);
        temp = fairScores(startIdx:length(fairScores));
        if(length(resAll)>length(temp))
           resAll(1:length(temp)) = temp;
        else
           resAll = temp(1:num_queues*num_time_steps);
        end
        fairScoreUsers = reshape(resAll, num_queues, num_time_steps);
        if (iFile == length(prefixes))
            w = lineWidth;
        else
            w = lineWidth*0.7;
        end
        hBar = plot(timeInSeconds, fairScoreUsers(USER_ID, :)', fairscoreLineStyles{iFile},'LineWidth',w);
     end
    end
  hold off;
    ylabel(strFairScore);
    xlabel(strTime);
    xlim([0 max(timeInSeconds)]);
    ylim([0 MAX_SCORE]);
    legendStr = methods;
    legend(legendStr,'Location','best','FontSize',fontLegend,'Orientation','vertical');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    if is_printed         
      figIdx=figIdx +1;
      fileNames{figIdx} = ['fairness_score'];         
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
      print ('-depsc', epsFile);
    end
end
%%

%% create dummy graph with legends
if enableSeparateLegend
  figure
  hBar = bar(timeInSeconds,fairScoreUsers',barwidth,'stacked','EdgeColor','none');
%   set(hBar,{'FaceColor'},barColors);
  legend(lengendStr,'Location','southoutside','FontSize',fontLegend,'Orientation','horizontal');
  set(gca,'FontSize',fontSize);
  axis([20000,20001,20000,20001]) %move dummy points out of view
  axis off %hide axis  
  set(gca,'YColor','none');
  set (gcf, 'Units', 'Inches', 'Position', legendSize, 'PaperUnits', 'inches', 'PaperPosition', legendSize);    

  if is_printed   
      figIdx=figIdx +1;
    fileNames{figIdx} = ['q' int2str(num_batch_queues)  '_res_usage_legend'];        
    epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
    print ('-depsc', epsFile);
  end
end

% if is_printed
%    pause(30);
%    close all;
% end

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