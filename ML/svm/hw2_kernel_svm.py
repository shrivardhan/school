#!/usr/bin/env python
# coding: utf-8

# In[219]:


import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import cvxopt

path = './hw2/hw2data.csv'
kernelName = 'RBF-SVM' # choose kernel. RBF-SVM or linear
data = pd.read_csv(path, header = None)
index = [i for i in range(data.shape[0])]
np.random.shuffle(index)
data = data.set_index([index]).sort_index()
t = int(0.8*data.shape[0])
train = data[:t]
test = data[t:]
y = train[[2]]
X = train.drop([2],axis=1)
y_test = test[[2]]
X_test = test.drop([2],axis=1)
k = 10
# X -= X.mean(axis=0, skipna = True)
X_shuffled = {}
y_shuffled = {}
for i in range(0,k):
    X_shuffled[i] = X[i::k]
    y_shuffled[i] = y[i::k]


# In[220]:


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


# In[221]:


def kernel(X1,Y1,name):
    if name == 'RBF-SVM':
        gamma = 10
        K = np.exp(-gamma * np.linalg.norm(X1 - Y1) ** 2)
        return K
    elif name == 'linear':
        K = np.dot(X1.T, Y1)
        return K


# In[222]:


def rbf_svm_train(X_train, y_train, C):
    total_samples, total_features = X_train.shape
    P = cvxopt.matrix(np.outer(y_train,y_train) * kernel(X_train.to_numpy(),X_train.to_numpy(), kernelName))
    q = cvxopt.matrix(np.ones(total_samples) * -1)
    A = cvxopt.matrix(y_train.to_numpy(), (1,total_samples))
    b = cvxopt.matrix(0.0)
    G = cvxopt.matrix(np.vstack((np.identity(total_samples)*-1, np.identity(total_samples))))
    h = cvxopt.matrix(np.vstack((cvxopt.matrix(np.zeros(total_samples)), cvxopt.matrix(np.ones(total_samples) * C))))
    alphas = np.ravel(cvxopt.solvers.qp(P, q, G, h, A, b)['x'])
    threshold_vals = alphas > 1e-5
    alpha = alphas[threshold_vals]
    vectors = X_train[threshold_vals]
    labels = y_train[threshold_vals]
    w = [alpha[i]*labels.to_numpy()[i] for i in range(len(alpha))]
    b = labels.to_numpy()[0] - np.sum(w[i]*kernel(vectors.to_numpy()[i],vectors.to_numpy()[0],kernelName) for i in range(len(alpha)))
    return w,b,vectors


# In[223]:


def rbf_svm_predict(X_valid, w, b, vectors):
    pred = []
    for x in X_valid.to_numpy():
        temp = np.sum([w[i] * kernel(x, vectors.iloc[i], kernelName) for i in range(len(w))])
        pred.append(np.sign(temp+b)[0])
    return pred


# In[224]:


def error(predicted, real):
    real = real.to_numpy()
    errors = abs(real - predicted)
    res = np.count_nonzero(errors > 0)
    return res/errors.shape[0]


# In[225]:


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
        w,b,vectors = rbf_svm_train(X_train, y_train, c)
        y_pred = rbf_svm_predict(X_valid, w, b, vectors)
        x_pred = rbf_svm_predict(X_train, w, b, vectors)
        pred = rbf_svm_predict(X_test, w, b, vectors)
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


# In[226]:


plt.plot(C,test_set,label="Test error")
plt.plot(C,train_set,label="Train error")
plt.plot(C,cross_set,label="Cross validation error")
plt.title("RMSE")
plt.xlabel("C")
plt.ylabel("RMSE")
plt.legend()


# In[ ]:




