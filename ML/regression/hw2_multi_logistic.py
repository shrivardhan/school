#!/usr/bin/env python
# coding: utf-8

# In[238]:


import pandas as pd
import numpy as np
import matplotlib.pyplot as plt


# In[239]:


train = pd.read_csv('./hw2/mnist_train.csv', header = None)
test = pd.read_csv('./hw2/mnist_test.csv', header = None)

y = train[[0]]
X = train.drop([0], axis=1)
y_test = test[[0]].to_numpy()
X_test = test.drop([0], axis=1).to_numpy()
X = (X - X.mean()).to_numpy()
X = X/X.std()
features = X.shape[1]
labels = y[0].unique()
y = y.to_numpy()
X_test = X_test - X_test.mean()
X_test /= X_test.std()

w = np.random.normal(size = (len(labels), features), loc=0.2, scale=0.05)
# print(w)
# print(w.shape)
b = np.random.normal(size = len(labels), loc=0.2, scale=0.05)
# print(b)
# print(b.shape)


# In[240]:


def softmax(x):
    return np.exp(x) / np.sum(np.exp(x), axis=0)


# In[241]:


def cross_entropy(t, label):
#     return -np.sum(label * np.log(t))
    return -np.log(t[label])


# In[242]:


def gradient(w, b, X_batch, y_batch):
    dw = 0
    db = 0
    batch_loss = 0
    for i in range(X_batch.shape[0]):
        one_hot = np.zeros((10,1))
#         print(y_batch[i])
        one_hot[y_batch[i]] = 1
#         print(w)
#         print(b)
#         print(x_batch.iloc[i].to_numpy())
#         print(temp)
        pred = softmax(np.dot(w, X_batch[i]) + b)
#         print(pred)
#         tmp = y_batch.iloc[i].to_numpy()
#         print(tmp)
#         tmp1 = np.zeros((tmp.size, tmp.max()+1))
#         tmp1[np.arange(1), tmp] = 1
#         print(tmp1)
#         tmp = np.zeros((10, 10))
#         tmp[np.arange(10), y_batch[i]] = 1
#         print(tmp)
#         print(type(pred))
#         print(pred.shape)
#         print(pred.reshape(-1,1).shape)
        b_temp = -(one_hot - pred.reshape(-1,1))
#         print(b_temp)
#         print(x_batch[i].reshape(1,-1).shape)
        w_temp = - np.multiply(one_hot - pred.reshape(-1,1), X_batch[i].reshape(1,-1))
        dw += w_temp
        db += b_temp
        loss = cross_entropy(pred, y_batch[i])
#         print(loss)
        batch_loss += loss

    dw /= X_batch.shape[0]
    db /= X_batch.shape[0]
    return dw, db, loss


# In[243]:


def multi_logistic_predict(w, b, X_data, y_data):
    loss_list = []
    temp = np.array([softmax(np.dot(w, X_data[i]) + b.mean()) for i in range(len(y_data))])
    pred = np.argmax(temp, axis=1)
    loss_list = [cross_entropy(temp[i], y_data[i]) for i in range(len(y_data))]
    return sum(loss_list), pred


# In[244]:


def error(predicted, real):
#     print(type(predicted))
#     print(type(real))
#     print(real.shape)
#     print(predicted.reshape(-1,1).shape)
    acc = sum(predicted.reshape(-1,1) == real)/float(len(real))
    return acc


# In[245]:


def  multi_logistic_train(alpha, w, b, X_train, y_train):
    batches = 10000
#     print(X_train.shape)
    batch_size = int(X_train.shape[0]/batches)
    loss_list = []
    for itr in range(batches):
            X_batch = X_train[batch_size*itr : batch_size*(itr+1)]
            y_batch = y_train[batch_size*itr : batch_size*(itr+1)]
            dw, db, loss = gradient(w, b, X_batch, y_batch)
#             print(db.shape)
#             print(b.shape)
            w -= alpha * dw
            b = b.reshape(10,1)
            b -= alpha * db
            b = b.reshape(10,)
#             print(loss)
            loss_list.append(loss)
    print('loss in batches',loss_list)
    return w, b


# In[247]:


epochs = 20
alpha = 0.01
test_loss_list = []
test_acc_list = []
train_loss_list = []
train_acc_list = []
predictions = []
pred = []
for e in range(epochs):
    print('epoch: ',e)
    w, b = multi_logistic_train(alpha, w, b, X, y)
    train_loss, pred = multi_logistic_predict(w, b, X, y)
    train_acc = error(pred, y)
#     print(train_loss)
#     print(train_acc)
    test_loss, predictions = multi_logistic_predict(w, b, X_test, y_test)
    test_acc = error(predictions, y_test)
#     print(test_loss)
#     print(test_acc)
    test_loss_list.append(test_loss)
    test_acc_list.append(test_acc)
    train_loss_list.append(train_loss)
    train_acc_list.append(train_acc)
    


# In[248]:


plt.plot(train_loss_list,label="Train")
plt.title("Loss-epochs ")
plt.xlabel("Epochs")
plt.ylabel("Loss")
plt.legend()
plt.savefig(fname='Train-loss.png',format='png')


# In[249]:


plt.plot(test_acc_list,label="Test")
plt.title("Accuracy-epochs ")
plt.xlabel("Epochs")
plt.ylabel("Accuracy")
plt.legend()
plt.savefig(fname='Test-accuracy.png',format='png')


# In[ ]:


print(test_acc_list)


# In[ ]:


# print(type(predictions))
# print(predictions.shape)
confusion_matrix = np.zeros((10,10))
for i in range(y_test.shape[0]):
#     print(i)
#     print(y_test[i])
#     print(predictions[i])
    confusion_matrix[y_test[i][0]][predictions[i]] =  confusion_matrix[y_test[i][0]][predictions[i]] + 1
print(confusion_matrix.astype(int))


# In[ ]:


np.save('weights.npy', w)


# In[ ]:




