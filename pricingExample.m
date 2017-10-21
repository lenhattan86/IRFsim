close all; clc; clear all;
addpath('matlab_func');

memory= [0.005 0.01 0.01]'; 
cpugpu = [0.1 0.1 0.1]'; 
beta = [10 1 0.1]'; 

% N=3; 
% cpugpu = 1 + 99.*rand(N,1);
% beta = 0.1+4.9.*rand(N,1);
% memory= 5+50.*rand(N,1);

report = [cpugpu beta memory]
[finalalloc,price]= pricing([cpugpu beta memory]);
finalalloc
price