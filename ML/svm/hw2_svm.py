#!/usr/bin/env python
# coding: utf-8

# In[506]:


import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import cvxopt

path = './hw2/hw2data.csv'

data = pd.read_csv(path, header = None)
index = [i for i in range(data.shape[0])]
np.random.shuffle(index)
data = data.set_index([index]).sort_index()
t = int(0.8*data.shape[0])
train = data[:t]
test = data[t:]
y = train[[2]]
X = train.drop([2], axis=1)
y_test = test[[2]]
X_test = test.drop([2], axis=1)
# data_temp = pd.DataFrame(np.random.randn(len(data), 2))
# data_msk = np.random.rand(len(data_temp)) < 0.8
# data_train = data_temp[data_msk]
# data_test = data_temp[~data_msk]
# y = data_train.iloc[:,-1].to_frame()
# X = data_train.iloc[:,:-1]
# y_test = data_test.iloc[:,-1].to_frame()
# X_test = data_test.iloc[:,:-1]
k = 10
# X -= X.mean(axis=0, skipna = True)
X_shuffled = {}
y_shuffled = {}
for i in range(0,k):
    X_shuffled[i] = X[i::k]
    y_shuffled[i] = y[i::k]


# In[507]:


def get_next_train_valid(X_shuffled, y_shuffled, itr):
    X_train = pd.DataFrame()
    y_train = pd.DataFrame()
    X_valid = pd.DataFrame()
    y_valid = pd.DataFrame()
    for i in range(0,k):
        if (i == itr):
            X_valid = X_shuffled.get(i)
            y_valid = y_shuffled.get(i)
        else:
            X_train = X_train.append(X_shuffled.get(i))
            y_train = pd.concat([y_train, y_shuffled.get(i)])
    return X_train, y_train, X_valid, y_valid


# In[508]:


def svmfit(X_train, y_train, C):
    total_samples, total_features = X_train.shape
    P = cvxopt.matrix(np.outer(y_train,y_train) * np.dot(X_train,X_train.T))
    q = cvxopt.matrix(np.ones(total_samples) * -1)
    A = cvxopt.matrix(y_train.to_numpy(), (1,total_samples))
    b = cvxopt.matrix(0.0)
    G = cvxopt.matrix(np.vstack((np.identity(total_samples)*-1, np.identity(total_samples))))
    h = cvxopt.matrix(np.hstack((np.zeros(total_samples), np.ones(total_samples) * C)))
    alphas = np.ravel(cvxopt.solvers.qp(P, q, G, h, A, b)['x'])
    w = np.sum(np.mat(y_train.to_numpy()*alphas)*np.mat(X_train.to_numpy()), axis = 0)
    threshold_vals = alphas > 1e-5
    w = np.array(w).flatten()
    b = y_train[threshold_vals].to_numpy() - np.dot(X_train[threshold_vals].to_numpy(), w)[0]
    b = np.asarray(b).flatten()
    b = np.mean(b)
    print(b)
    return w,b
    


# In[509]:


def predict(X_valid, w, b):
    return np.sign(np.dot(X_valid.to_numpy(), w) + b)
    


# In[510]:


def error(predicted, real):
    real = real.to_numpy()
    predicted = predicted.reshape(-1,1)
#     correct = np.sum(real == predicted)
#     print(correct)
    errors = abs(real - predicted)
    res = np.count_nonzero(errors > 0)
    return res/errors.shape[0]


# In[511]:


train_set = []
test_set = []
cross_set = []
C = [0.0001, 0.001, 0.01, 0.1, 1, 10, 100, 1000]
for c in C:
    train_e = 0
    test_e = 0
    cross_e = 0
    for i in range(0,k):
        X_train, y_train, X_valid, y_valid = get_next_train_valid(X_shuffled, y_shuffled, i)
        w,b = svmfit(X_train, y_train, c)
        y_pred = predict(X_valid, w, b)
        x_pred = predict(X_train, w, b)
        pred = predict(X_test, w, b)
#         print(type(y_pred))
#         print(y_pred.shape)
        train_e += error(x_pred,y_train)
        test_e += error(pred, y_test)
        cross_e += error(y_pred, y_valid)
    train_e /= k
    test_e /= k
    cross_e /= k
    train_set.append(train_e)
    test_set.append(test_e)
    cross_set.append(cross_e)
print(train_set)
print(test_set)
print(cross_set)


# In[515]:


plt.plot(C,test_set,label="Test error")
plt.plot(C,train_set,label="Train error")
plt.plot(C,cross_set,label="Cross validation error")
plt.title("RMSE")
plt.xlabel("C")
plt.ylabel("RMSE")
plt.legend()
plt.savefig(fname='errors.png',format='png')


# In[ ]:





# In[ ]:





# In[ ]:




