import pickle as pk
name = './data/APIFeatures.GLAPI'
with open(name, 'rb') as f:
    data = pk.load(f)
print(data[0])

name = './data/APIIndexs.GLAPI'
with open(name, 'rb') as f:
    data = pk.load(f)
print(data)