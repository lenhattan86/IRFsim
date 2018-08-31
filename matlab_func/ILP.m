% Input: 
% cpu n*3;
% gpu n*3;
% cap 1*3;
% used 1*3;

function [schedule] = ILP(cpu,gpu,cap,used)
tic
[n,~] = size(cpu);
m = max(max(cpu(:,3)),max(gpu(:,3)));

f = [cpu(:,3); gpu(:,3); m*ones(n,1)];

capLHS = [cpu(:,1)' zeros(1,2*n); zeros(1,n) gpu(:,1)' zeros(1,n);  cpu(:,2)' gpu(:,2)' zeros(1,n)];
occLHS = zeros(n,3*n);
for j = 1:n
    occLHS(j,j) = 1;
    occLHS(j,j+n) = 1;
end
A = [capLHS; occLHS];
b = [cap(1)-used(1)  cap(2)-used(2)  cap(3)-used(3)  ones(1,n)];
Aeq = zeros(n,3*n);
for j = 1:n
    Aeq(j,j) = 1;
   Aeq(j,j+n)= 1; 
   Aeq(j,j+2*n) = 1;
end
beq = ones(1,n);
intcon = [1:2*n];
ub = [ones(2*n,1);Inf*ones(n,1)];
lb = zeros(3*n,1);
sol = intlinprog(f,intcon,A,b,Aeq,beq,lb,ub);
schedule = [sol(1:n)'; sol(n+1:2*n)'; sol(2*n+1:3*n)'];
toc
end