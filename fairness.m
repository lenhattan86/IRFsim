%% compute fairness index scores
n = 3;
betas =[10, 0.1, 1];
%bad
if(false)
    % good
  r=[0.03/0.7 0.2/0.4 0.4/0.3];
  allocES=[100/3, 100/3, 100/3;
              100/3, 100/3, 100/3;
              100/3, 100/3, 100/3];
  allocMaxMin=[10.117700540869643, 76.76600716779824, 33.333333095093664;
              65.96747774079802, 6.991884493200389, 33.33333309505904;
              15.253280184636342, 9.74671963663658, 33.33333309503057];
            
  allocSpeedUp = [15.458923709972169, 52.87105458122879, 23.321548693811142;
              52.93587262566947, 14.810692411126917, 27.208470933391066; 
              18.306561674814912, 18.79588933378212, 49.46993467812938];

else
  r=[0.1/0.9 0.05/0.7 0.1/0.3];
  allocES=[100/3, 100/3, 100/3;
              100/3, 100/3, 100/3;
              100/3, 100/3, 100/3];
  allocMaxMin=[3.4929618347659877E-6, 6.879802121135074, 7.644224967145844;
              99.99998909061699, 70.18756176968692, 7.644196090541834;
              3.969453772634528E-6, 22.93260991963177, 7.644204629695185];
  allocSpeedUp = [2.3852809037173096E-5, 44.99998529157868, 49.99998630762173;
    54.999977749881346, 2.385174515858294E-5, 3.9285700096468488;
    44.999976929883296, 54.9999693900465, 33.333315439976616];
end
%

%alloc=allocES;
% alloc=allocMaxMin;
alloc=allocSpeedUp;

U = zeros(n,n);
A = zeros(2,n);
A_JFI = zeros(2,n);
JFI=ones(1,n);
for i=1:n
  for j=1:n
    A(1,i)= (alloc(j,1)+alloc(j,2)*betas(i))*r(i);
    A(2,i)= alloc(j,3);
    U(i,j)= min(A(1,i),A(2,i));    
  end
  A_JFI(1,i)= alloc(i,1)+alloc(i,2)*betas(i);
  A_JFI(2,i) = alloc(i,3);
end
A_JFI(1,:) = A_JFI(1,:)./sum(A_JFI(1,:));
A_JFI(2,:) = A_JFI(2,:)./sum(A_JFI(2,:));
JFI= min(A_JFI);
%JFI= A_JFI(2,:);

fair_scores=zeros(n,n);
for i=1:n
  for j=1:n
    fair_scores(i,j)=U(i,j)/U(i,i);
  end
end
%fair_scores
fair_score=max(max(fair_scores));

% Jain's fairness index
% consider as 2 resources?
JainFairnessIndex = (sum(JFI))^2/(n*sum(JFI.^2))
%JainFairnessIndex2 = (sum(max(alloc)))^2/(n*sum(max(alloc).^2))