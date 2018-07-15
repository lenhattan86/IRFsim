addpath('matlab_func');
common_settings;
% is_printed = 1;
%%
queue_num = 3;
figureSize = figSizeOneCol;

plots  = [true, true];

methods = { 'once', 'periodic', 'dem'};


%%
if plots(1)     
   x = [10 100 150 200 300 400 500 700 800 1000 2000];
   jobCompleted = [1518 1956 1953 2107 2051 2068 2048 2089 2045 1959 2063];
   baseline = 2118;
   figIdx=figIdx +1;         figures{figIdx} =figure;
   scrsz = get(groot,'ScreenSize');   
   yVals = jobCompleted;
   plot(x,jobCompleted,'LineWidth', LineWidth);
   hold on;
   plot(x,ones(size(x))*baseline,'LineWidth', LineWidth);
   legend('avg subset of jobs', 'avg all jobs');
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='number jobs averaged';
    yLabel='jobs completed';
%     legendStr={'ES', 'DRF', 'MaxMin', 'SpeedUp', 'Pricing'};  
  ylim ([0 max(yVals*1.1)]);
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
   

    fileNames{figIdx} = 'demand_change';
end

if plots(2)     
   
   jobCompleted = [2118 1729 0]
   figIdx=figIdx +1;         figures{figIdx} =figure;
   scrsz = get(groot,'ScreenSize');   
   yVals = jobCompleted./(jobCompleted(1));
   bar(yVals,barSize);
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel=strMethods;
    yLabel='progress';
%     legendStr={'ES', 'DRF', 'MaxMin', 'SpeedUp', 'Pricing'};
  xlim([0.5 3.5]);
  ylim ([0 max(yVals*1.1)]);
    xLabels=methods;
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   

    fileNames{figIdx} = 'demand_change';
end

return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);
    
    pdfFile = [ fig_path fileName '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end