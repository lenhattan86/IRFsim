addpath('matlab_func');
common_settings;
% is_printed = 1;
%%
barWidth = 0.5;
queue_num = 25;
cluster_size=100;
figureSize = figSizeOneCol .* [1 1 2/3 2/3];
plots  = [true false];
methods = {strDRF, strES,  strAlloX};
% files = {'DRF', 'ES',  'AlloX'};
% speedups = [0.1, 0.5, 1.0]
errs = 0:0.1:0.5;
files = {'DRF', 'ES',  'AlloX'};

DRFId = 1; ESId = 2; AlloXId = 3;
methodColors = {colorES; colorDRF; colorProposed};

%% load data
resVals = ones(length(methods),length(errs)); 
for i=1:length(methods)
    for j=1:length(errs)
        extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_e' sprintf('%1.1f',errs(j))];
        outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
        [JobIds, startTimes, endTimes, durations, queueNames] = import_compl_time(outputFile);
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

    plot(errs, resVals(AlloXId,1)./resVals(AlloXId,:) , 'LineWidth', lineWidth);

    xLabel='std of errors';
    yLabel=strFactorImprove;
    legendStr=methods;    
    ylim([0 1.1]);
%     ylim([0 0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
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
    pdfFile = [ fig_path fileName '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end