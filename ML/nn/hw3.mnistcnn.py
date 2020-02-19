#!/usr/bin/env python
# coding: utf-8

# In[145]:


import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import torch
import torchvision
import torchvision.transforms as transforms
import torch.optim as optim
from torch.autograd import Variable
import torch.nn as nn
import torch.nn.functional as F
import datetime


# In[146]:
batch_runTime_list = []
batch_size_list = [32,64,96,128]
optimizerFlag = 1 # 1 for SGD(default), 2 for Adam, 3 for Adagrad
for batchSize in batch_size_list:
    # In[147]:

    transform = transforms.Compose([transforms.ToTensor(),
      transforms.Normalize((0.1307,), (0.3081,))
    ])
    trainset = torchvision.datasets.MNIST(root = './', train = True, download = True,transform = transform)
    testset = torchvision.datasets.MNIST(root = './', train = False, download = True,transform = transform)


    # In[148]:


    train_loader = torch.utils.data.DataLoader(trainset, batch_size = batchSize, shuffle = True)
    test_loader = torch.utils.data.DataLoader(testset, batch_size = batchSize, shuffle = True)


    # In[149]:


    class Net(nn.Module):
        def __init__(self):
            super(Net, self).__init__()
            self.conv1 = nn.Conv2d(1, 20, 3, stride=1)
            self.fc1 = nn.Linear(20 * 13 * 13, 128)
            self.fc2 = nn.Linear(128,10)

        def forward(self, x):
            x = self.conv1(x)
            x = F.max_pool2d(x, (2, 2))
            x = F.relu(x)
            x = x.view(x.size(0), -1)
            x = self.fc1(x)
            x = F.relu(x)
            x = self.fc2(x)
            return F.log_softmax(x)


    # In[150]:


    # init network
    net = Net()
    #init optimizer
    optimizer = optim.SGD(net.parameters(), lr = 0.01, momentum = 0.9)
    if optimizerFlag == 2:
        optimizer = torch.optim.Adam(model.parameters(), lr = 0.01)
    elif optimizerFlag == 3:
        optimizer = torch.optim.Adagrad(model.parameters(), lr = 0.01)
    # init loss function
    criterion = nn.CrossEntropyLoss()
    losslist = []
    traing_accuracy_list = []
    # for run time
    startTime = datetime.datetime.now()
    new_loss = 0
    while True:
        total_loss = 0
        training_accuracy = 0
        prev_loss = new_loss
        for batchId, (data, target) in enumerate(train_loader):
            # all cells need gradient
            images = data.requires_grad_()
            # gradient is set to 0
            optimizer.zero_grad()
            # passing through the network
            net_out = net(images)
            # calculaing cross entropy loss
            loss = criterion(net_out, target)
            # propogating loss at current step
            loss.backward()
            # updating parameters (in steps) after gradient descent
            optimizer.step()
            total_loss += loss
            pred = net_out.data.max(1)[1]
            training_accuracy += pred.eq(target.data).sum()
        losslist.append(total_loss.item()/len(train_loader.dataset))
        traing_accuracy_list.append(training_accuracy.item()/len(train_loader.dataset))
        print('current epoch loss: {}'.format(total_loss/len(train_loader.dataset)))
        new_loss = total_loss.item()/len(train_loader.dataset)
        if new_loss - prev_loss < 0.0001:
            break
    # for Runtime
    endTime = datetime.datetime.now()
    # getting runTime
    batch_runTime_list.append((endTime-startTime).seconds)


    # In[ ]:


    torch.save(net, "mnist-cnn")
    model = Net()
    model = torch.load( "mnist-cnn")


    # In[ ]:


    test_loss = 0
    correct = 0
    for data, target in test_loader:
        images = data.requires_grad_()
        # pass through the network
        net_out = model(images)
        # calculate total loss
        test_loss += criterion(net_out, target)
        # predictions
        pred = net_out.data.max(1)[1]
        # accuracy
        correct += pred.eq(target.data).sum()

    test_loss /= len(test_loader.dataset)
    print('Average loss:{}, Accuracy:{}'.format(test_loss, 100. * correct / len(test_loader.dataset)))


    # In[ ]:

    if batchSize == 32:
        plt.plot(losslist)
        plt.title("loss vs epochs (SGD)")
        plt.ylabel("loss%")
        plt.xlabel("epochs")
        plt.savefig(fname='lossSGD32.png',format='png')


        # In[ ]:


        plt.plot(traing_accuracy_list)
        plt.title("Accuracy vs epochs (SGD)")
        plt.ylabel("Accuracy %")
        plt.xlabel("epochs")
        plt.savefig(fname='accuracySGD32.png',format='png')

    if optimizerFlag > 1:
        break


# In[115]:


# have to run it for different batch sizes before executing this
plt.plot(batch_size_list,batch_runTime_list)
plt.title("Time vs Batch size")
plt.xlabel("Batch Size")
plt.ylabel("Time(in sec)")
plt.savefig(fname='batchSizeTime.png',format='png')


# In[ ]:
