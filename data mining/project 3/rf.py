import csv
import sys
from collections import Counter
import math
import copy
import random

trainFile = str(sys.argv[1])
minFreq = int(sys.argv[2])

allData = []
root = None

rootNodeID = 0

# f = open(modelFile,"wb")
f = 0

class CountMatrixNode:
    NUMLABELS = 10

    def __init__(self,val,attribute):
        if(val == None):
            return
        self.splitVal = val
        self.lessThan = [0 for x in range(CountMatrixNode.NUMLABELS)]
        self.greaterThan = [0 for x in range(CountMatrixNode.NUMLABELS)]
        self.giniIndex = 0.0
        self.lessThanDataList = []
        self.greaterThanDataList = []
        self.index = attribute

    def getSplitVal(self):
        return self.splitVal

    def getIndex(self):
        return self.index

    def setPointers(self,data):
        for x in data:
            if(float(x.getValues()[self.index]) > float(self.splitVal)):
                self.greaterThanDataList.append(x)
            else:
                self.lessThanDataList.append(x)

    def setLists(self, dataLabelCount, labelCount):
        self.lessThan = [i for i in labelCount]
        self.greaterThan = [(dataLabelCount[i]-labelCount[i]) for i in range(len(dataLabelCount))]

    def incrementLessThan(self,label,dataPoint):
        self.lessThan[label] = self.lessThan[label]+1
        self.lessThanDataList.append(dataPoint)

    def incrementGreaterThan(self,label,dataPoint):
        self.greaterThan[label] = self.greaterThan[label]+1
        self.greaterThanDataList.append(dataPoint)

    def computeGiniIndex(self,parentLen):
        sumLessThan = sum(self.lessThan)
        sumGreaterThan = sum(self.greaterThan)
        proportionlessThan = [(float(self.lessThan[i])/sumLessThan)**2 for i in range(CountMatrixNode.NUMLABELS) if sumLessThan > 0]
        proportionGreaterThan = [(float(self.greaterThan[i])/sumGreaterThan)**2 for i in range(CountMatrixNode.NUMLABELS) if sumGreaterThan > 0]
        self.giniIndex = (1-sum(proportionlessThan))*(float(sumLessThan)/parentLen) + (1-sum(proportionGreaterThan))*(float(sumGreaterThan)/parentLen)
        # print self.giniIndex
        return self.giniIndex

    def getGiniIndex(self):
        return self.giniIndex

    def getLessThanList(self):
        return self.lessThanDataList

    def getGreaterThanList(self):
        return self.greaterThanDataList

    def isPure(self):
        checkLabel = -1
        if len(self.lessThanDataList) > 0:
            checkLabel = self.lessThanDataList[0].getLabel()
        elif len(self.greaterThanDataList) > 0:
            checkLabel = self.greaterThanDataList[0].getLabel()
        else:
            return -1

        for x in self.lessThanDataList:
            if(checkLabel != x.getLabel()):
                return -1
        for x in self.greaterThanDataList:
            if(checkLabel != x.getLabel()):
                return -1
        return checkLabel

    def hasMinThreshold(self):
        if((len(self.lessThanDataList) + len(self.greaterThanDataList)) > minFreq):
            return True
        else:
            return False

    def getMajorityLabel(self):
        LabelList = [x.getLabel() for x in self.lessThanDataList] + [x.getLabel() for x in self.greaterThanDataList]
        LabelsCount = Counter(LabelList)
        return (LabelsCount.most_common(1))[0][0]

    def setNumLabels(self,nl):
        CountMatrixNode.NUMLABELS = nl


class Node:
    ROOT = None
    DIMENSIONS = -1

    def __init__(self, v, l):
        self.left = None
        self.right = None
        self.label = -1
        self.splitAttributes = [i for i in l]
        global rootNodeID
        self.nodeID = rootNodeID
        rootNodeID = rootNodeID + 1
        self.rightID = -1
        self.leftID = -1
        if(v == None):
            Node.ROOT = self
            return
        self.value = v
        self.splitAttributes.append(v.getIndex())
        self.label = v.isPure()
        if( self.label != -1):
            return
        if( (not self.value.hasMinThreshold()) | self.hasAllAttributesOpened(self.splitAttributes)):
            self.label = self.value.getMajorityLabel()

    def getRoot(self):
        return Node.ROOT

    def getValue(self):
        return self.value

    def getLabel(self):
        return self.label

    def getLeft(self):
        return self.left

    def getRight(self):
        return self.right

    def getSplitAttributes(self):
        return self.splitAttributes

    def getID(self):
        return self.nodeID

    def insert(self, node, o):
        if(o==0):
            self.value = node
            return self
        dataPoint = Node(node,self.splitAttributes)
        if o == 1:
            self.left = dataPoint
            self.leftID = dataPoint.getID()
        elif o == 2:
            self.right = dataPoint
            self.rightID = dataPoint.getID()
        return dataPoint

    def setDimensions(self,d):
        Node.DIMENSIONS = d

    def getDimensions(self):
        return Node.DIMENSIONS

    def hasAllAttributesOpened(self, traversedList):
        if(len(traversedList) == Node.DIMENSIONS):
            return True
        else:
            return False

    def printTreeToFile(self):
        if self.left:
            self.left.printTreeToFile()
        f.write(str(self.nodeID)+","+str(self.value.getSplitVal())+","+str(self.value.getIndex())+","+str(self.label)+","+str(self.rightID)+","+str(self.leftID)+"\n")
        if self.right:
            self.right.printTreeToFile()

class digits:
    def __init__(self, l , v):
        self.label = l
        self.values = v.split(',')[1:]

    def getLabel(self):
        return self.label

    def getValues(self):
        return self.values

def splitAttribute(data,attribute,dataLabelCount):
    data.sort(key=lambda x:float(x.getValues()[attribute]))
    dataLen = len(data)
    attributeVals = [float(x.getValues()[attribute]) for x in data]
    # print attributeVals[1:10]
    uniqueElems = sorted(set(attributeVals))
    splitVals = [int((uniqueElems[i]+uniqueElems[i+1])/2) for i in range(len(uniqueElems)-1)]
    splitVals = sorted(set(splitVals))
    if len(uniqueElems) == 1:
        splitVals.append(uniqueElems[0])
    splitNode = None
    lowestGini = 0
    tempIndex = 0
    # print splitVals
    # print len(splitVals)
    labelCount = [0 for x in range(10)]
    for x in splitVals:
        temp = CountMatrixNode(x,attribute)
        for i in range(tempIndex,dataLen):
            if(float(data[i].getValues()[attribute])>x):
                tempIndex = i
                break

            labelCount[int(data[i].getLabel())] += 1
            # print labelCount
        # print labelCount
        # print dataLabelCount
        temp.setLists(dataLabelCount,labelCount)
        # print x,"::",temp.computeGiniIndex(len(data)),"::",tempIndex
        if((splitNode == None) | (temp.computeGiniIndex(len(data))<lowestGini)):
            splitNode = temp
            lowestGini = temp.getGiniIndex()
            # print "lowest gini"
            # print lowestGini
    return splitNode

def splitAttributePoint(data,splitAttributes):
    splitPoint = None
    splitDimension = -1
    dataLabelCount = [0 for x in range(10)]
    for x in data:
        dataLabelCount[int(x.getLabel())] += 1

    for i in range(root.getDimensions()):
        if(i in splitAttributes):
            continue
        splitNode = splitAttribute(data,i,dataLabelCount)

        if(splitPoint == None):
            splitDimension = i
            splitPoint = splitNode
            continue
        if(splitNode.getGiniIndex() < splitPoint.getGiniIndex()):
            splitDimension = i
            splitPoint = splitNode
    if(splitDimension == -1):
        return

    splitPoint.setPointers(data)
    # print splitPoint.getGiniIndex()

    if(len(splitAttributes)==0):
        splitAttributes.append(splitDimension)
    return splitPoint

def createTree(tree,data,o):
    if(len(data) == 0):
        return
    if(tree == None):
        return
    tempNode = splitAttributePoint(data,tree.getSplitAttributes())
    if(tempNode == None):
        return
    tree = tree.insert(tempNode,o)
    if(int(tree.getLabel())>-1):
        return
    createTree(tree,tree.getValue().getLessThanList(),1)
    createTree(tree,tree.getValue().getGreaterThanList(),2)

with open(trainFile, 'rb') as csvfile:
    trainreader = csv.reader(csvfile, delimiter=' ', quotechar='|')
    allLabels = []
    for row in trainreader:
        allData.append(digits(row[0][0],row[0][1:]))
        allLabels.append(int(row[0][0]))

sampleLen = int(0.4*len(allData))
for i in range(100):
    rootNodeID = 0
    modelFile="pred"+'{0:03}'.format(i+1)+".csv"
    sampleData = random.sample(allData,sampleLen)
    root = Node(None,[])
    root.setDimensions(len(sampleData[0].getValues()))
    createTree(root,sampleData,0)
    f = open(modelFile,"wb")
    root.printTreeToFile()
    f.close()
