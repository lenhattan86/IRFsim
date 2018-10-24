if (exist('is_printed'))
    if (is_printed)
        disp('==== Using the old setting ====');
        clear; close all; clc;
        is_printed = true;
    else
        clear; close all; clc;
        is_printed = false;
    end    
else
    clear; close all; clc;
    is_printed = false;
end

fontSize=10;
fontAxis = fontSize;
fontTitle = fontSize;
fontLegend = fontSize;
LineWidth = 1.5;
FontSize = fontSize;
axisWidth = 1.5;

%%

legendSize = [0.0 0 5 0.4];
figSizeOneCol = [0.0 0 5 3];
figSizeOneColHaflRow = [1 1 1 0.5].* figSizeOneCol;
figSizeTwoCol = 2*figSizeOneCol;
figSizeOneThirdCol = 1/3*figSizeOneCol;
figSizeHalfCol = 1/2*figSizeOneCol;
figSizeTwothirdCol = 2/3*figSizeOneCol;
figSizeThreeFourth = 3/4*figSizeOneCol;
figSizeFourFifthCol = 4/5*figSizeOneCol;

%%

barLineWidth=0;
groupBarSize = 0.9;
barSize = 0.5;
barWidth = 0.5;
lineWidth = 2;

%%

figIdx=0;

LOCAL_FIG = 'figs/';

%PS_CMD_FORMAT='ps2pdf -dEmbedAllFonts#true -dSubsetFonts#true -dEPSCrop#true -dPDFSETTINGS#/prepress %s %s';
PS_CMD_FORMAT='ps2pdf -dEmbedAllFonts#true -dSubsetFonts#true -dEPSCrop#false -dPDFSETTINGS#/prepress %s %s';

% fig_path = ['figs/'];
% fig_path = '/home/tanle/Dropbox/Papers/AlloX/figs/';
fig_path = '/ssd/projects/allox/figs/';

%%

strUser1 = 'User 1';
strUser2 = 'User 2';
strUser3 = 'User 3';
strUser4 = 'User 4';
strUnalloc = 'unallocated';

strES = 'ES';
strAlloX = 'AlloX';
strAlloXopt = 'AlloXopt';
strDRFFIFO = 'DRFF';
strDRFSJF = 'DRFS';
strSJF = 'SJF';
strDRFExt = 'DRFE';
strSRPT = 'SRPT';
strErrorStd = 'std. of err (%)';

strGPU = 'GPU';
strCPU = 'CPU';
strMemory= 'mem.';

strJobCompleted = 'completed jobs';
strAvgCmplt = 'avg. compl. time (mins)';
strMaxCmplt = 'max compl. (mins)';

strMakeSpan = 'makespan';
strImprovement = 'improvement (%)';
strPerfGap = 'perf. gap (%)';

strNormCapacity='norm. capacity';
strCdf = 'cdf';

strEstimationErr = 'std. of estimation errors (%)';
strFactorImprove = 'factor of improvement';
strAvgComplTime = 'avg. compl. (secs)';

strMethods='methods';

strTime = 'secs';

strSimTime = 'mins';

strBudget='norm. budget';

strFairScore = 'fair score';

%% line specs
lineAlloX = '-';
lineES = ':';
lineDRF = '--';
lineSRPT = '-.';

lineBB = '-';
lineTPCDS = '--';
lineTPCH = '-.';
workloadLineStyles = {lineBB, lineTPCDS, lineTPCH};
fairscoreLineStyles = {lineDRF, lineAlloX, lineSRPT};

%%

%http://colorbrewer2.org/#type=sequential&scheme=BuGn&n=3

% colorProposed = [0.8500    0.3250    0.0980];
% colorStrict = [0.4660    0.6740    0.1880];
% colorDRF = [0    0.4470    0.7410];
% colorDRFW = [0.9290    0.6940    0.1250];

colorProposed = [237    125    49]/255;
colorES = [165    165    165]/255;
colorDRF = [68    71   196]/255;
colorDRFW = [00    0.0    0.0];
%colorhard = [hex2dec('a8')    hex2dec('dd')    hex2dec('b5')]/255;%2c7fb8
colorhard = [hex2dec('2c')    hex2dec('7f')    hex2dec('b8')]/255;%2c7fb8

colorBarMinMax='k';
lineWidthBarMinMax=1.5;
colorUser1 = [hex2dec('1f')    hex2dec('49')    hex2dec('7d')]/255;
colorUser2 = [hex2dec('f7')    hex2dec('96')    hex2dec('46')]/255;

colorJob0 = [hex2dec('00')    hex2dec('72')    hex2dec('BD')]/255;
colorJob1 = [hex2dec('D9')    hex2dec('53')    hex2dec('19')]/255;
colorJob2 = [hex2dec('ED')    hex2dec('B1')    hex2dec('20')]/255;
colorJob3 = [hex2dec('7E')    hex2dec('2F')    hex2dec('8E')]/255;
colorJob4 = [hex2dec('77')    hex2dec('AC')    hex2dec('30')]/255;
colorJob5 = [hex2dec('4D')    hex2dec('BE')    hex2dec('EE')]/255;
colorJob6 = [hex2dec('A2')    hex2dec('14')    hex2dec('2F')]/255;


%% 
log_folder = 'log/';
output_folder = 'output/';

%%