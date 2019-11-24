import sys
import csv
from collections import Counter

testFile = str(sys.argv[1])
predictionFile = str(sys.argv[2])

testData = []
treeNodes = {}

class Node:
    def __init__(self,row):
        self.splitVal = row[1]
        self.attribute = row[2]
        self.label = row[3]
        self.rightNodeID = row[4]
        self.leftNodeID = row[5]
        self.rightNode = None
        self.leftNode = None

    def constructTree(self,data):
        if(self.rightNodeID):
            if(int(self.rightNodeID)>-1):
                self.rightNode = Node(data[self.rightNodeID])
                self.rightNode.constructTree(data)
        if(self.leftNodeID):
            if(int(self.leftNodeID)>-1):
                self.leftNode = Node(data[self.leftNodeID])
                self.leftNode.constructTree(data)

    def getClassification(self,dataPoint):
        if(int(self.label)>-1):
            return self.label
        if(float(dataPoint.getValues()[int(self.attribute)]) <= float(self.splitVal)):
            return self.leftNode.getClassification(dataPoint)
        else:
            return self.rightNode.getClassification(dataPoint)
        return -1

class digits:
    def __init__(self, l , v):
        self.label = l
        self.values = v.split(',')[1:]
        self.prediction = []

    def getLabel(self):
        return self.label

    def getValues(self):
        return self.values

    def getPrediction(self):
        return Counter(self.prediction).most_common(1)[0][0]

    def setPrediction(self,val):
        self.prediction.append(val)

with open(testFile, 'rb') as csvfile:
    testReader = csv.reader(csvfile, delimiter=' ', quotechar='|')
    for row in testReader:
        testData.append(digits(row[0][0],row[0][1:]))

for i in range(100):
    modelFile="pred"+'{0:03}'.format(i+1)+".csv"
    treeNodes.clear()
    with open(modelFile,'rb') as csvfile:
        modelReader = csv.reader(csvfile, delimiter=',')
        for row in modelReader:
            treeNodes[row[0]] = row

    root = Node(treeNodes["0"])
    root.constructTree(treeNodes)

    for x in testData:
        x.setPrediction(root.getClassification(x))

with open(predictionFile,'wb') as f:
	for x in testData:
		f.write(x.getLabel()+','+x.getPrediction()+'\n')
