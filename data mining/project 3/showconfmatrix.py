import csv
import sys

predictionsfile = str(sys.argv[1])

with open(predictionsfile, 'rb') as csvfile:
    predictionReader = csv.reader(csvfile, delimiter=',')
    trueLabels = []
    predictedLabels = []
    for row in predictionReader:
        trueLabels.append(row[0])
        predictedLabels.append(row[1])

    labels = sorted(set(trueLabels))

    confusionMatrix = [[0 for x in range(len(labels))] for y in range(len(labels))]

    for i in range(len(trueLabels)):
        confusionMatrix[int(trueLabels[i])][int(predictedLabels[i])] += 1

    print "Confusion Matrix:"
    for x in confusionMatrix:
        print x

    accuracy = float(sum(confusionMatrix[i][i] for i in range(len(labels))))/float(sum(sum(confusionMatrix[i]) for i in range(len(labels))))

    print "\nAccuracy: ",accuracy*100,"%"


    # startTime = int(round(time.time() * 1000))
    # endTime = int(round(time.time() * 1000))
    # print "total time taken = "+ str((endTime-startTime)/1000)+" seconds"
