addpath('matlab_func');
common_settings;

%%
queue_num = 4;


plots  = [false, false, true, true];

isLarge = false;
largeStr = '';
if isLarge
  cluster_size = 600;
  queue_num = 30;
  END_TIME = 50;
%   largeStr='_lbeta_cpu';
%   largeStr='_lbeta_mix';
%   largeStr='_sbeta_cpu';
%   largeStr='_sbeta_mix';
%   largeStr='_mbeta_cpu';
%   largeStr='_mbeta_mix';
  largeStr='_mbeta_lcpu';
%   largeStr='';
  plots(1:2:3)=false;
else
  cluster_size = 100;
  END_TIME = 10;
  largeStr='_case2';
  queue_num=4;
end


outputExtra = '';
colorUsers = {colorUser1; colorUser2; colorUser3; colorUser4};
%%
% methods = {strES, strDRF, strFDRF, strMP, strMSR, strPricing};
methods = {strES, strDRF, strMP, strFDRF,  strPricing};
  files = {['ES-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
                ['DRF-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
                ['MaxMinMem-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
                ['FDRF-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];                
%                 ['SpeedUp-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
                ['Pricing-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv']
                 };

num_batch_queues = 1;
num_interactive_queue = 3;
num_queues = num_batch_queues + num_interactive_queue;



%%
result_folder = '';
output_folder = [result_folder 'output/'];

figIdx = 0;
% fig_path = 'figs\';
%%x
% global batchJobRange
% batchJobRange = [1:10]




%%
QUEUES = {'queue0', 'queue1', 'queue2', 'queue3'};
if plots(1) 
    
    
    avgComplTime = zeros(length(QUEUES), length(methods));
    for i=1:length(QUEUES)
      [ avgComplTime(i,:) ] = obtain_compl_time( output_folder, files, QUEUES{i});
    end

   figure;
   scrsz = get(groot,'ScreenSize');   
   hBar = bar(avgComplTime', 'group');
   set(hBar,{'FaceColor'}, colorUsers);   
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel=strMethods;
    yLabel=strAvgComplTime;
    legendStr={strUser1, strUser2, strUser3, strUser4};

    xLabels=methods;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'avg_compl_time_each';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end  
end

%%
if plots(2) 
    QUEUES = 'queue';
   
   [ avg_compl_time ] = obtain_compl_time( output_folder, files, QUEUES);

   figure;
   scrsz = get(groot,'ScreenSize');   
   hBar = bar(avg_compl_time', barSize);
%    set(hBar,{'FaceColor'}, colorUsers);   
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel=strMethods;
    yLabel=strAvgComplTime;
%     legendStr={'ES', 'DRF', 'MaxMin', 'SpeedUp', 'Pricing'};

    xLabels=methods;
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'avg_compl_time_all';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end  
end




%%
scaleUP=50/END_TIME;
if plots(3) 
    
    jobCompleted = zeros(length(QUEUES), length(methods));
    for i=1:length(QUEUES)
      [ jobCompleted(i,:) ] = obtain_job_completed( output_folder, files, QUEUES{i});
    end
    (jobCompleted(1,:)-jobCompleted(1,1))/jobCompleted(1,1)*100
    (jobCompleted(1,:)-jobCompleted(1,2))/jobCompleted(1,2)*100
   figure;
   scrsz = get(groot,'ScreenSize');   
   hBar = bar(jobCompleted'*scaleUP, 'group');
   set(hBar,{'FaceColor'}, colorUsers);   
%    set(gca,'yscale','log')
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel=strMethods;
    yLabel=strJobCompleted;
    legendStr={strUser1, strUser2, strUser3, strUser4};

    xLabels=methods;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'job_completed_each';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end  
end

%%
if plots(4) 
    QUEUES = 'queue';
   
   [ jobCompleted ] = obtain_job_completed( output_folder, files, QUEUES);

   figure;
   scrsz = get(groot,'ScreenSize');   
   bar(jobCompleted'*scaleUP,barSize);
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel=strMethods;
    yLabel=strJobCompleted;
%     legendStr={'ES', 'DRF', 'MaxMin', 'SpeedUp', 'Pricing'};

    xLabels=methods;
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'job_completed_all';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end  
end

fileNames
return;
%%

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName largeStr '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end