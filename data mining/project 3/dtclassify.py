import sys
import csv


modelFile = str(sys.argv[1])
testFile = str(sys.argv[2])
predictionFile = str(sys.argv[3])

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
        self.prediction = -1

    def getLabel(self):
        return self.label

    def getValues(self):
        return self.values

    def getPrediction(self):
        return self.prediction

    def setPrediction(self,val):
        self.prediction = val

with open(modelFile,'rb') as csvfile:
    modelReader = csv.reader(csvfile, delimiter=',')
    for row in modelReader:
        treeNodes[row[0]] = row

root = Node(treeNodes["0"])
root.constructTree(treeNodes)

with open(testFile, 'rb') as csvfile:
    testReader = csv.reader(csvfile, delimiter=' ', quotechar='|')
    for row in testReader:
        testData.append(digits(row[0][0],row[0][1:]))

for x in testData:
    x.setPrediction(root.getClassification(x))

with open(predictionFile,'wb') as f:
	for x in testData:
		f.write(x.getLabel()+','+x.getPrediction()+'\n')
