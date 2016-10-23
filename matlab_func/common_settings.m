clear; close all; clc;

fontAxis = 8;
fontTitle = 8;
fontLegend = 8;
LineWidth = 2;
FontSize = 8;
is_printed = true;

LOCAL_FIG = '/home/tanle/projects/SpeedFairSim/figs/';

PS_CMD_FORMAT='ps2pdf -dEmbedAllFonts#true -dSubsetFonts#true -dEPSCrop#true -dPDFSETTINGS#/prepress %s %s';

if true
    drf_compl_files = {'DRF-output_1_1.csv';
                      'DRF-output_1_2.csv';
                      'DRF-output_1_4.csv';
                      'DRF-output_1_8.csv';
                      'DRF-output_1_16.csv';
                      'DRF-output_1_32.csv'};

    drfw_compl_files = {'DRF-W-output_1_1.csv';
                      'DRF-W-output_1_2.csv';
                      'DRF-W-output_1_4.csv';
                      'DRF-W-output_1_8.csv';
                      'DRF-W-output_1_16.csv';
                      'DRF-W-output_1_32.csv'};  

    speedfair_compl_files = {'SpeedFair-output_1_1.csv';
                      'SpeedFair-output_1_2.csv';
                      'SpeedFair-output_1_4.csv';
                      'SpeedFair-output_1_8.csv';
                      'SpeedFair-output_1_16.csv';
                      'SpeedFair-output_1_32.csv'};  

    strict_priority_compl_files = {'Strict-output_1_1.csv';
                      'Strict-output_1_2.csv';
                      'Strict-output_1_4.csv';
                      'Strict-output_1_8.csv';
                      'Strict-output_1_16.csv';
                      'Strict-output_1_32.csv'};  
else       

    drf_compl_files = {'DRF-output_1_1.csv';
                      'DRF-output_2_1.csv';
                      'DRF-output_3_1.csv';
                      'DRF-output_4_1.csv'};

    drfw_compl_files = {'DRF-w-output_1_1.csv';
                      'DRF-w-output_2_1.csv';
                      'DRF-w-output_3_1.csv';
                      'DRF-w-output_4_1.csv'};  

    speedfair_compl_files = {'SpeedFair-output_1_1.csv';
                      'SpeedFair-output_2_1.csv';
                      'SpeedFair-output_3_1.csv';
                      'SpeedFair-output_4_1.csv'};  

    strict_priority_compl_files = {'Strict-output_1_1.csv';
                      'Strict-output_2_1.csv';
                      'Strict-output_3_1.csv';
                      'Strict-output_4_1.csv'};  
end

