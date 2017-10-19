% report: n*3 matrix with columns as [C/G,beta,Memory]
% k:      optimal speedup ratio
% alloc:  optimal allocation

% an example with randomized 100 jobs
% cpugpu = 1 + 99.*rand(100,1);
% beta = 0.1+4.9.*rand(100,1);
% memory= 5+50.*rand(100,1);
% [k,alloc,envy]= speedup([cpugpu beta memory]);

function [k,alloc,envy] = speedup(report)

[n,~]=size(report);
bneck = zeros(n,1);
%calculate the bottleneck value (transformed to C/G)
for i=1:n
    bneck(i)=min(1/n+report(i,2)*1/n,1/n*report(i,1)/report(i,3));
end
bneck=bneck'; % n*1 matrix

%generating equation matrix eqcoeff: k*bneck-x-beta y = 0
% row: job i
% column: [bneck, x1,x2,...xn,y1,y2,...yn]
objcoeff = [-1 zeros(1,2*n)];
eqcoeff = zeros(n,2*n+1);
eqcoeff(:,1)=bneck;
for i =1:n
    eqcoeff(i,i+1)=-1;
    eqcoeff(i,n+i+1)= -report(i,2);
end
eqrhs = zeros(1,n);


%memory constraint

memcoeff=0;
for i=1:n
    memcoeff = memcoeff + bneck(i)*report(i,3)/(report(i,1));
end
%inequality constraint
ineqcoeff = [memcoeff zeros(1,2*n);0 ones(1,n) zeros(1,n); zeros(1,n+1) ones(1,n)];
ineqrhs= ones(1,3);

lb = zeros(1,2*n+1);

x = linprog(objcoeff,ineqcoeff,ineqrhs,eqcoeff,eqrhs,lb);

k = x(1);
alloc=zeros(n,3);
%allocation matrix is a n*3 matrix
alloc(:,1)=x(2:n+1);
alloc(:,2)=x(n+2:2*n+1);
for i=1:n
    alloc(i,3)= k*bneck(i)*report(i,3)/report(i,1);
end
if abs(memcoeff*k -1)<10^(-4)
    disp('Memory is the bottleneck')
end

envy=zeros(n,n);
%check envy-freeness about jobs
for i=1:n
    for j=1:n  %give j's allocation to i
        if min(alloc(j,1)+report(i,2)*alloc(j,2),alloc(j,3)*report(i,1)/report(i,3)) > k*bneck(i)+10^(-4)
            envy(i,j)=1;
        end
    end
end





end