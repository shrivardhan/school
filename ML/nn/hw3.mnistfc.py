#!/usr/bin/env python
# coding: utf-8

# In[260]:


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


# In[261]:


transform = transforms.Compose([transforms.ToTensor(),
  transforms.Normalize((0.1307,), (0.3081,))
])
trainset = torchvision.datasets.MNIST(root = './', train = True, download = True,transform = transform)
testset = torchvision.datasets.MNIST(root = './', train = False, download = True,transform = transform)
# print(type(trainset))
# print(trainset)
# print(len(trainset))
# print(testset)


# In[262]:


train_loader = torch.utils.data.DataLoader(trainset, batch_size = 32, shuffle = True)
test_loader = torch.utils.data.DataLoader(testset, batch_size = 32, shuffle = True)
# print(type(train_loader))
# print(len(train_loader))
# print(len(train_loader.dataset))
# print(train_loader)
# print(len(test_loader))
# print(len(test_loader.dataset))


# In[263]:


class Net(nn.Module):
    def __init__(self):
        super(Net, self).__init__()
        self.fc1 = nn.Linear(28 * 28, 128)
        self.fc2 = nn.Linear(128,10)

    def forward(self, x):
        x = F.relu(self.fc1(x))
        x = self.fc2(x)
        return F.log_softmax(x)


# In[264]:


# init network
net = Net()
print(net)


# In[265]:


#init optimizer
optimizer = optim.SGD(net.parameters(), lr = 0.01, momentum = 0.9)
# init loss function
criterion = nn.CrossEntropyLoss()
print(criterion)
print(optimizer)


# In[266]:


epochs = 10
losslist = []
traing_accuracy_list = []
for epoch in range(0,epochs):
    total_loss = 0
    training_accuracy = 0
    for batchId, (data, target) in enumerate(train_loader):
        data, target = Variable(data), Variable(target)
        data = data.view(-1, 28*28)
        # gradient is set to 0
        optimizer.zero_grad()
        # passing through the network
        net_out = net(data)
        # calculaing cross entropy loss
        loss = criterion(net_out, target)
        # propogating loss at current step
        loss.backward()
        # updating parameters (in steps) after gradient descent
        optimizer.step()
        # calculate total loss
        total_loss += loss
        # predictions
        pred = net_out.data.max(1)[1]
        # accuracy
        training_accuracy += pred.eq(target.data).sum()
    losslist.append(total_loss.item()/len(train_loader.dataset))
    traing_accuracy_list.append(training_accuracy.item()/len(train_loader.dataset))
    print('loss at epoch: {} = {}'.format(epoch,total_loss/len(train_loader.dataset)))



# In[267]:


torch.save(net, "mnist-fc")
model = Net()
model = torch.load( "mnist-fc")


# In[268]:


test_loss = 0
correct = 0
for data, target in test_loader:
    data, target = Variable(data, volatile=True), Variable(target)
    data = data.view(-1, 28 * 28)
    # pass through the network
    net_out = model(data)
    # calculate total loss
    test_loss += criterion(net_out, target)
    # predictions
    pred = net_out.data.max(1)[1]
    # accuracy
    correct += pred.eq(target.data).sum()

test_loss /= len(test_loader.dataset)
print('Average loss:{}, Accuracy:{}'.format(test_loss, 100. * correct / len(test_loader.dataset)))


# In[269]:


plt.plot(losslist)
plt.savefig(fname='losslistFC.png',format='png')

# In[270]:


plt.plot(traing_accuracy_list)
plt.savefig(fname='accuracylistFC.png',format='png')

# In[ ]:
