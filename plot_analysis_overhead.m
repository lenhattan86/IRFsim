addpath('matlab_func');
common_settings;
% is_printed = 1;
EXTRA = '';
%%
barWidth = 0.5;
queue_num = 10;
cluster_size=20;
figureSize = figSizeThreeFourth;
plots  = [true true];

overheads = 0.0:0.01:0.05;
files = {'DRFFIFO','DRF', 'ES','DRFExt',  'AlloX','SRPT'};
methods = {'DRFF', strDRFSJF, strES, strDRFExt,  strAlloX, strSRPT};
lines = {lineDRFFIFO, lineDRFSJF,  lineES, lineDRFExt,lineAlloX, lineSRPT};
colors = {colorDRFFIFO, colorDRFSJF,  colorES, colorDRFExt,colorAlloX, colorSRPT};
DRFFIFOId = 1; ESId = 3; AlloXId = 5;
methodColors = {colorES; colorDRF; colorProposed};

%% load data
resVals = ones(length(files),length(overheads)); 
for i=1:length(files)
    for j=1:length(overheads)
        extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_o' sprintf('%1.2f',overheads(j))];
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
    
%    plot(errs, resVals(DRFFIFOId,1)./resVals(AlloXId,:) , 'LineWidth', lineWidth);
%    hold on;
    yValues = (resVals(DRFFIFOId,:)-resVals(AlloXId,:))./resVals(DRFFIFOId,:) * 100;
    plot(overheads*100*(3), yValues , 'LineWidth', lineWidth);
    
   xLabel='profiling overheads %';
   yLabel='improvement vs. DRFF (%)';
   legendStr=methods;    
   ylim([0 max(yValues)]);
    xlim([0 15]);
    ylim([0 100]);
   set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    box off;
   ylabel(yLabel,'FontSize', fontAxis);    
   xlabel(xLabel,'FontSize', fontAxis);   
   fileNames{figIdx} = 'analysis_overhead';
end

%%
if plots(2)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');       
    
    yValues = resVals';
    hold on;    
    for iMethod = 1:length(yValues)
        plot(overheads*100*(3), yValues(:,iMethod) , lines{iMethod}, 'Color', colors{iMethod}, 'LineWidth', lineWidth);
    end
    hold off;
    
   xLabel='profiling overheads %';
   yLabel=strAvgCmplt;
   legendStr=methods;    
%    ylim([0 max(yValues)]);
   xlim([0 15]);
   box off;
   legend(methods, 'Location','northeastoutside','FontSize', fontLegend);
   set (gcf, 'Units', 'Inches', 'Position', figureSize.*[1 1 1.2 1], 'PaperUnits', 'inches', 'PaperPosition', figureSize.*[1 1 1.2 1]);
%     xlabel(xLabel,'FontSize',fontAxis);
   ylabel(yLabel,'FontSize', fontAxis);    
   xlabel(xLabel,'FontSize', fontAxis);   
   fileNames{figIdx} = 'analysis_overhead_ext';
end


%%
if~is_printed
    return;
else
   pause(1)  
end
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