addpath('matlab_func');
common_settings;

%%
queue_num = 3;

cluster_size = 10;
%%
methods = {strES, strDRF, strEDRF, strMP, strMSR, strPricing};
  files = {['ES-output_' num2str(queue_num) '_' num2str(cluster_size) '.csv'];
                ['DRF-output_' num2str(queue_num) '_' num2str(cluster_size) '.csv'];
                ['EDRF-output_' num2str(queue_num) '_' num2str(cluster_size) '.csv'];
                ['MaxMinMem-output_' num2str(queue_num) '_' num2str(cluster_size) '.csv'];
                ['SpeedUp-output_' num2str(queue_num) '_' num2str(cluster_size) '.csv'];
                ['Pricing-output_' num2str(queue_num) '_' num2str(cluster_size) '.csv']
                 };

num_batch_queues = 1;
num_interactive_queue = 3;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 800;

extra = '_2';
is_printed = true;
cluster_size = 1000;




barColors = colorb1i3;

%%
result_folder = '';
output_folder = [result_folder 'output/'];

figIdx = 0;

% fig_path = 'figs\';
%%
% global batchJobRange
% batchJobRange = [1:10]


plots  = [true, true];

%%
if plots(1) 
    QUEUES = 'queue';
   
   [ avg_compl_time ] = obtain_compl_time( output_folder, files, QUEUES);

   figure;
   scrsz = get(groot,'ScreenSize');   
   bar(avg_compl_time', 'group');
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='Methods';
    yLabel='average compl. time (seconds)';
%     legendStr={'ES', 'DRF', 'MaxMin', 'SpeedUp', 'Pricing'};

    xLabels=methods;
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'avg_compl_time_2';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end  
end


%%


if plots(2) 
    QUEUES = {'queue0', 'queue1', 'queue2'};
    
    jobCompleted = zeros(length(QUEUES), length(methods));
    for i=1:length(QUEUES)
      [ jobCompleted(i,:) ] = obtain_job_completed( output_folder, files, QUEUES{i});
    end

   figure;
   scrsz = get(groot,'ScreenSize');   
   bar(jobCompleted', 'group');
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='Methods';
    yLabel='job completed';
    legendStr={'user1', 'user2', 'user3'};

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
if plots(2) 
    QUEUES = 'queue';
   
   [ jobCompleted ] = obtain_job_completed( output_folder, files, QUEUES);

   figure;
   scrsz = get(groot,'ScreenSize');   
   bar(jobCompleted', 'group');
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='Methods';
    yLabel='job completed';
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
    pdfFile = [ fig_path fileName extra '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end