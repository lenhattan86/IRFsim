% report = [cpu/mem, beta, gpu/mem] absolute value;
% report = [16/12 80 1/12; 16/12 112 1/12; 16/12 192 1/12; 16/12 288 1/12];
% cluster_size = [384 12 1152];
% [final_alloc,final_price] = AllocX(report,cluster_size);

%%
report = [16/4 80 1/2; 16/8 112 1/2; 16/6 192 1/2];
cluster_size = 600*[64 1 96];
[final_alloc,final_price] = AllocX(report,cluster_size);
final_alloc
final_price

final_alloc(:,1)*final_price(1)/cluster_size(1) + final_alloc(:,2)*final_price(2)/cluster_size(2)