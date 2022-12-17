import numpy as np
import networkx as nx
import pickle as pk
from tqdm import tqdm

from stellargraph import StellarGraph
from stellargraph.mapper import GraphSAGENodeGenerator,GraphSAGELinkGenerator
from stellargraph.data import EdgeSplitter
from stellargraph.layer import GraphSAGE, link_classification

from tensorflow import keras
import logging
import time
from collections import defaultdict
import os
import scipy.sparse as sp
import torch
from torch.nn import functional as F

datasetname = 'SH_S'#有数据集SH_S、SH_L、MV
threshold1 = 0.5
threshold2 = 0.5
batch_size = 128
epochs = 3
num_samples = [30, 20]
layer_sizes = [64, 64]

best_suc = [0]*21
best_pre = [0]*21
best_recall = [0]*21
pro_best_suc = [0]*21
pro_best_pre = [0]*21
pro_best_recall = [0]*21
test_config = 'C2.1'

now = time.strftime("%Y-%m-%d-%H_%M_%S",time.localtime(time.time()))

logging.basicConfig(format='%(asctime)s-%(levelname)s:%(message)s',
                    filename='./results/GLAPI-GraphSAGE_AB_'+datasetname+'_'+str(batch_size)+'_'+str(epochs)+'_'+now+'.log',
                    filemode='a', level=logging.INFO)
logger = logging.getLogger(__name__)


def getg(data, minsup=1):
    g = nx.Graph()
    n = len(data.item_method_id)
    # 顶点
    point = []
    for i in range(n):
        point.append(i)
    g.add_nodes_from(point)
    # 边权重
    edglist = []
    edges = set()
    for user, items in tqdm(data.invocation_mx.items()):
        for i in range(len(items)):
            for j in range(i + 1, len(items)):
                edges.add((items[i], items[j]))

    for edg in tqdm(edges):
        weight = float(data.adj[edg[0], edg[1]])
        if weight >= minsup:
            edglist.append((edg[0], edg[1], weight))
        # edglist.append((edg[0],edg[1]))

    g.add_weighted_edges_from(edglist)
    # g.add_edges_from(edglist)
    return g


def to_torch_sparse_tensor(sparse_mx):
    """Convert a scipy sparse matrix to a torch sparse tensor."""
    sparse_mx = sparse_mx.tocoo().astype(np.float32)
    indices = torch.from_numpy(np.vstack((sparse_mx.row, sparse_mx.col)).astype(np.int64))
    values = torch.from_numpy(sparse_mx.data)
    shape = torch.Size(sparse_mx.shape)
    return torch.sparse.FloatTensor(indices, values, shape)


def build_my_new_adj_matrix(data, train_dict):
    n = len(data.item_method_id)
    A = sp.dok_matrix((n, n), dtype=np.float32)
    FD = defaultdict(int)
    for user, items in tqdm(train_dict.items()):
        for i in range(len(items)):
            FD[items[i]] += 1
            for j in range(i + 1, len(items)):
                if A[items[i], items[j]] == 0:
                    A[items[i], items[j]] = A[items[j], items[i]] = len(data.invocation_mx.items())
                else:
                    A[items[i], items[j]] = A[items[j], items[i]] = A[items[j], items[i]] + len(
                        data.invocation_mx.items())
    print('build_my_new_adj_matrix finish')
    data.FD = FD
    data.adj = to_torch_sparse_tensor(A)


def load_mydata(dataset_name):
    name = './data/%s-mydata.pk' % dataset_name
    if not os.path.exists(name):
        print('no file.')
    with open(name, 'rb') as f:
        data = pk.load(f)
        print('load dataset from disk.')
    # data.adj = to_torch_sparse_tensor(data.adj)
    return data


def get_node_embeddings(data):
    g = getg(data)
    g_feature_attr = g.copy()
    for node_id, node_data in g_feature_attr.nodes(data=True):
        node_data["feature"] = data.item_pre_emb[node_id].numpy()

    G = StellarGraph.from_networkx(
        g_feature_attr, node_features="feature", node_type_default="API", edge_type_default="Attribute"
    )
    edge_splitter_test = EdgeSplitter(G)

    # Randomly sample a fraction p=0.1 of all positive links, and same number of negative links, from G, and obtain the
    # reduced graph G_test with the sampled links removed:
    G_test, edge_ids_test, edge_labels_test = edge_splitter_test.train_test_split(
        p=0.1, method="global", keep_connected=True
    )
    # Define an edge splitter on the reduced graph G_test:
    edge_splitter_train = EdgeSplitter(G_test)

    # Randomly sample a fraction p=0.1 of all positive links, and same number of negative links, from G_test, and obtain the
    # reduced graph G_train with the sampled links removed:
    G_train, edge_ids_train, edge_labels_train = edge_splitter_train.train_test_split(
        p=0.1, method="global", keep_connected=True
    )

    train_gen = GraphSAGELinkGenerator(G_train, batch_size, num_samples)
    train_flow = train_gen.flow(edge_ids_train, edge_labels_train, shuffle=True)
    test_gen = GraphSAGELinkGenerator(G_test, batch_size, num_samples)
    test_flow = test_gen.flow(edge_ids_test, edge_labels_test)
    graphsage = GraphSAGE(
        layer_sizes=layer_sizes, generator=train_gen, bias=True, dropout=0.3
    )
    x_inp, x_out = graphsage.in_out_tensors()
    prediction = link_classification(
        output_dim=1, output_act="relu", edge_embedding_method="ip"
    )(x_out)
    model = keras.Model(inputs=x_inp, outputs=prediction)

    model.compile(
        optimizer=keras.optimizers.Adam(lr=1e-3),
        loss=keras.losses.binary_crossentropy,
        metrics=[keras.metrics.binary_accuracy],
    )
    history = model.fit(train_flow, epochs=epochs, validation_data=test_flow, verbose=2)
    x_inp_src = x_inp[0::2]
    x_out_src = x_out[0]
    embedding_model = keras.Model(inputs=x_inp_src, outputs=x_out_src)
    node_ids = G_train.nodes()
    node_gen = GraphSAGENodeGenerator(G_train, batch_size, num_samples).flow(node_ids)
    node_embeddings = embedding_model.predict(node_gen, workers=4, verbose=1)
    return node_embeddings


# threshold1取值（0，1）表示考虑节点相似特征的阈值，值越大候选特征节点越少
# threshold2取值（0，1）表示节点属性特征的重要性，越小越不重要
def build_new_relation(ratings, threshold1=0.8, threshold2=0.001):
    dataset = load_mydata(datasetname)
    n = len(ratings)
    for i in tqdm(range(n)):
        for j in range(i + 1, n):
            simily = float(ratings[i][j])
            if simily >= threshold1:
                dataset.adj[i, j] = threshold2 * simily
    return dataset


def get_my_top_items(tensor):
    item_dict = {}
    for i in tqdm(range(len(tensor))):
        if tensor[i].item() != 0:
            item_dict[i] = tensor[i].item()
    # print('get_my_top_items==>item_dict',item_dict)
    top_items = [item[0] for item in sorted(item_dict.items(), key=lambda item: item[1], reverse=True)]
    # print('get_my_top_items==>top_items',top_items)
    return top_items[:21]


def get_my_top_items2(data, adj, Q, ratings, a, b):
    # 链接预测
    diag = torch.diag(ratings)  # 获取对角为一维向量
    diag_embed = torch.diag_embed(diag)  # 由diag恢复为对角矩阵
    link_embed = ratings - diag_embed
    rowsoftmax = F.softmax(link_embed, dim=1)
    link_q1 = torch.zeros(size=[len(rowsoftmax[0])])
    for q in Q:
        link_q1 = link_q1 + rowsoftmax[q]

    # 贝叶斯预测
    M = len(data.invocation_mx.items())
    D = set()
    FD = data.FD

    for q in Q:
        tensor = adj[q]
        for i in range(len(tensor)):
            if tensor[i].item() != 0:
                D.add(i)

    link_q2 = torch.zeros(size=[len(adj)])
    print(len(link_q2))
    for d in D:
        fd = FD[d]
        fdq = 1
        for q in Q:
            tensor = adj[q]
            fdq = fdq * (tensor[d].item() * 1.0 / fd)
        # 利用贝叶斯求得d被预测的概率
        p2 = fdq * fd * 1.0 / M
        link_q2[d] = p2

    link_q = link_q1 * a + link_q2 * b
    arr, top_items = torch.sort(link_q, descending=True)
    top_items = top_items[:21]
    # top_items = [item[0] for item in sorted(item_dict.items(), key=lambda item: item[1], reverse=True)]
    # print('get_my_top_items==>top_items',top_items)
    return top_items.numpy()


def myeval(dataset, ratings, a, b):
    test_set = dataset.test_dict
    logger.info('test start. test set size: %d' % len(test_set))
    t1 = time.time()
    users = np.asarray(list(test_set.keys()))  # 训练集的方法编号数组

    top_items = []
    used_items = []

    for userid in tqdm(users):
        used_items.append(set(dataset.train_dict[userid]))
        # print(dataset.train_dict[userid],dataset.train_dict[userid][0])
        # top_items.append(get_my_top_items(dataset.adj[dataset.train_dict[userid][0]]))
        top_items.append(get_my_top_items2(dataset, dataset.adj, dataset.train_dict[userid], ratings, a, b))

    # print('myeval2=>top_items',top_items)
    # print('myeval2=>used_items', used_items)

    items = []
    for i, item in enumerate(top_items):  # 第i个测试方法推荐的API列表item
        logger.info('context:%s;rank list:%s' % (str(used_items[i]), str(item)))
        # print('context:',used_items[i],'rank list:',item)
        # if i<=20:
        rec_item = [tid for tid in item if tid not in used_items[i]]
        # print(rec_item)
        items.append(rec_item[:20])

    def getMAP(N):
        qarr = []
        for i, uid in enumerate(users):
            r = 0
            drarr = []
            for k in range(1, N + 1):
                intersect = set(items[i][:k]) & set(test_set[uid])
                p = len(intersect) / k
                newr = len(intersect) / len(set(test_set[uid]))
                dr = (newr - r) * p
                drarr.append(dr)
                r = newr
            qarr.append(np.sum(drarr))
        return np.sum(qarr) / len(qarr)

    def res_at_k(k):
        suc_methods = []
        precisions = []
        recalls = []
        proj_suc = defaultdict(list)
        proj_pre = defaultdict(list)
        proj_recall = defaultdict(list)

        for i, uid in enumerate(users):
            pid = dataset.test_user2proj[uid]
            intersect = set(items[i][:k]) & set(test_set[uid])
            if len(intersect) > 0:
                suc_methods.append(uid)
                proj_suc[pid].append(1)
            else:
                logger.debug('failed uid %d' % uid)
                logger.debug('GT:{}, REC:{}'.format(test_set[uid], items[i]))
                proj_suc[pid].append(0)
            p = len(intersect) / k
            r = len(intersect) / len(set(test_set[uid]))
            precisions.append(p)
            recalls.append(r)
            proj_pre[pid].append(p)
            proj_recall[pid].append(r)
        suc_rate = len(suc_methods) / len(users)
        logger.info('----------------------result@%d--------------------------' % k)
        logger.info('success rate at method level %f' % (suc_rate))
        logger.info('mean precision:%f, mean recall:%f' % (np.mean(precisions), np.mean(recalls)))

        suc_project = [np.mean(val) for val in proj_suc.values()]
        pres = [np.mean(val) for val in proj_pre.values()]
        recs = [np.mean(val) for val in proj_recall.values()]
        logger.info('**********************************************************')
        logger.info('success rate at project level %f' % (np.mean(np.mean(suc_project))))
        logger.info('mean precision:%f, mean recall:%f' % (np.mean(pres), np.mean(recs)))
        return suc_rate, np.mean(precisions), np.mean(recalls), np.mean(np.mean(suc_project)), np.mean(pres), np.mean(
            recs)

    t2 = time.time()
    logger.info('test end time: {}s'.format(t2 - t1))
    for i in range(1, 21):
        suc, pre, rec, pro_suc, pro_pre, pro_rec = res_at_k(i)
        if suc > best_suc[i]:
            best_suc[i] = suc
        if pre > best_pre[i]:
            best_pre[i] = pre
        if rec > best_recall[i]:
            best_recall[i] = rec
        logger.warning('method level => top %d : best suc %f, best pre %f,  best recall %f' % (
        i, best_suc[i], best_pre[i], best_recall[i]))

        if pro_suc > pro_best_suc[i]:
            pro_best_suc[i] = pro_suc
        if pro_pre > pro_best_pre[i]:
            pro_best_pre[i] = pro_pre
        if pro_rec > pro_best_recall[i]:
            pro_best_recall[i] = pro_rec
        logger.warning('project level => top %d : best suc %f, best pre %f,  best recall %f' % (
        i, pro_best_suc[i], pro_best_pre[i], pro_best_recall[i]))

    logger.info('MAP: %f' % (getMAP(20)))

#载入数据并划分数据集
data = load_mydata(datasetname)
data.split_data(test_config)
build_my_new_adj_matrix(data,data.train_dict)
node_embeddings = get_node_embeddings(data)

name = './data/APIFeatures.GLAPI'
with open(name, 'wb') as f:
    pk.dump(node_embeddings, f)

name = './data/APIIndexs.GLAPI'
with open(name, 'wb') as f:
    pk.dump(data.item_method_id, f)
