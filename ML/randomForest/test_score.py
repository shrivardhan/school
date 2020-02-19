"""Test Error Calculator"""
import numpy as np

# true values loaded (ASSUME THIS IS HIDDEN TO YOU)
true_values = np.genfromtxt('true_values.csv', delimiter=',')
true_values = np.expand_dims(true_values, axis=1)
print(type(true_values))
print(true_values.shape)

# sample predicted values for TA testing
# sample_preds = np.genfromtxt('sample.csv', delimiter=',')
# sample_preds = np.expand_dims(sample_preds, axis=1)
# print(sample_preds)
# print(sample_preds.shape)


def score(pred_vals):
    """Function returning the error of model
    ASSUME THIS IS HIDDEN TO YOU"""
    num_preds = len(pred_vals)
    num_true_vals = len(true_values)
    val = np.sqrt(np.sum(np.square(np.log2((true_values+1)/(pred_vals+1))))/num_true_vals)
    return round(val, ndigits=5)


# sample predicted values for TA testing
# print(score(sample_preds))
