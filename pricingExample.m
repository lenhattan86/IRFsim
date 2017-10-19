close all; clc; clear all;
addpath('matlab_func');


cpugpu = [0.7 0.4 0.3]'; 
beta = [10 0.1 1]'; 
memory= [0.03 0.2 0.4]'; 
% N=3; 
% cpugpu = 1 + 99.*rand(N,1);
% beta = 0.1+4.9.*rand(N,1);
% memory= 5+50.*rand(N,1);

report = [cpugpu beta memory]
[finalalloc,price]= pricing([cpugpu beta memory]);
finalalloc
price