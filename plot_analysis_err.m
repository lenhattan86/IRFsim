addpath('matlab_func');
common_settings;
is_printed = 0;
EXTRA='';
%%
barWidth = 0.5;
queue_num = 10;
cluster_size=20;
figureSize = figSizeThreeFourth;
plots  = [false true];

% files = {'DRF', 'ES',  'AlloX'};
% speedups = [0.1, 0.5, 1.0]
errs = 0:0.1:0.6;
files = {'DRFFIFO','DRF', 'ES', 'DRFExt', 'AlloX', 'SRPT'};
methods = {'DRFF', strDRFSJF, strES, strDRFExt,  strAlloX, strSRPT};
lines = {lineDRFFIFO, lineDRFSJF,  lineES, lineDRFExt,lineAlloX, lineSRPT};
colors = {colorDRFFIFO, colorDRFSJF,  colorES, colorDRFExt,colorAlloX, colorSRPT};
DRFFIFOId = 1; DRFId=2; ESId = 3; DRFExtId = 4; AlloXId = 5; SRPTId=6;
% vsMethods = [DRFFIFOId, DRFId, ESId, DRFExtId, SRPTId];
vsMethods = [ESId, AlloXId, SRPTId];

%% load data
resVals = ones(length(files),length(errs)); 
for i=1:length(files)
    for j=1:length(errs)
        extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_e' sprintf('%1.1f',errs(j))];
        outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
        [JobIds, startTimes, endTimes, durations, queueNames] = import_compl_time_real_job(outputFile);
        if(~isnan(durations))
            resVals(i,j) = mean(durations);                        
        end
    end
end

%%
if plots(1)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');       
   
   hold on;  
   legendStr={};    
   
   for i=1:length(vsMethods)
       idx = vsMethods(i);
       yValues = (resVals(idx,:)-resVals(AlloXId,:))./resVals(idx,:) * 100;
        plot(errs*100, yValues , 'LineWidth', lineWidth);
        legendStr{i}=['vs.' methods(idx)];
   end
   
   hold off; 
    
    
   xLabel=strErrorStd;
   yLabel='improvement (%)';
   
%    ylim([min(min(min(yValues)),0) max(max(yValues))]);
%     ylim([0 0.4]);
   set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
   ylabel(yLabel,'FontSize', fontAxis);    
   xlabel(xLabel,'FontSize', fontAxis);   
   fileNames{figIdx} = 'analysis_err_im';
end

%%
if plots(2)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');       
   
   hold on;
   
   strLegend = {};
   for i=1:length(vsMethods)  
       iMethod = vsMethods(i);
       plot(errs*100, resVals(iMethod,:),lines{iMethod}, 'Color', colors{iMethod}, 'LineWidth', lineWidth);
       strLegend{i} = methods{iMethod};
   end
   hold off;
   
   legend(strLegend, 'Location','west','FontSize', fontLegend);
   
   xLabel=strErrorStd;
   yLabel=strAvgCmplt;
   box off;
%    ylim([min(min(min(yValues)),0) max(max(yValues))]);
%     ylim([0 0.4]);
   set (gcf, 'Units', 'Inches', 'Position', figureSize .*[1 1 1 1], 'PaperUnits', 'inches', 'PaperPosition', figureSize .*[1 1 1 1]);
%     xlabel(xLabel,'FontSize',fontAxis);
   ylabel(yLabel,'FontSize', fontAxis);    
   xlabel(xLabel,'FontSize', fontAxis);   
   fileNames{figIdx} = 'analysis_err';
end

%%
return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName EXTRA '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end