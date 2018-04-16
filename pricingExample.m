close all; clc; clear all;
addpath('matlab_func');

%%
% N=3; 
% cpugpu = 1 + 99.*rand(N,1);
% beta = 0.1+4.9.*rand(N,1);
% memory= 5+50.*rand(N,1);

%%
% memory= [0.005 0.01 0.01]'; 
% cpugpu = [0.1 0.1 0.1]'; 
% beta = [10 1 0.1]';
% report = [cpugpu beta memory];
% [finalalloc,price]= pricing([cpugpu beta memory]);
% finalalloc
% finalalloc = finalalloc .* (capacity * ones(size(capacity')));
% price

%%
GI = 1024*1024*1024;
memory= [3 3]' /240; 
cpugpu = [22000 22000]'/ 88000; 
beta = [2.588 1.258]';
capacity = [88 4 240];

report = [cpugpu beta memory];
[finalalloc,price]= pricing([cpugpu beta memory]);
finalalloc
finalalloc = finalalloc .* (ones(2,1)*capacity);
finalalloc
% price

