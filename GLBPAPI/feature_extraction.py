from collections import defaultdict
import os
from tqdm import tqdm
import pickle as pk
import scipy.sparse as sp
import numpy as np
import torch
from lex import LexParser

from mydataset import MyDataset

# 项目数据集提取目录，提取后保存在当前项目的data目录下（后缀为.pk形式）
basedir = 'F:\APIRecommendation\projects-APIs'

MAX_SEQ_LEN = 10

user_method_id = {}
item_method_id = {}
class_id = {}
project_id = {}
user_name = []
item_name = []
class_name = []
project_name = []

invocation_matrix = defaultdict(list)
proj_have_class = defaultdict(list)
class_have_method = defaultdict(list)
proj_have_users = []


def get_project_prefix(lines):
    packages = [line.split('#')[0].split('/')[:-2] for line in lines]
    l = min([len(m) for m in packages])
    while l > 0:  # 包名的最长公共前缀作为project prefix
        flag = True
        for m in packages:
            if m[l-1] != packages[0][l-1]:
                flag = False
                break
        if flag:
            break
        else:
            l -= 1
    if l == 0:
        print('error: project have no common prefix')
    return '/'.join(packages[0][:l])

# 判断方法和API是不是同一个项目，不是同一个项目说明API是第三方或者外部API，否则API是本地定义的函数
def is_same_project(p1, p2):
    #print(p1, p2) #com/logicmonitor/msp/dao/impl/UserDaoImpl/addOneToWatchList(java.lang.String,java.lang.String) java/sql/PreparedStatement/setString(int,java.lang.String)
    p1 = p1.split('/')[:-2]
    p2 = p2.split('/')[:-2]
    #print(p1,p2) #['com', 'logicmonitor', 'msp', 'dao', 'impl'] ['java', 'sql']
    size = min(len(p1), len(p2))
    equals = sum([1 for i in range(size) if p1[i] == p2[i]])
    if size > 0 and equals == 0:
        return False
    if equals == size or equals >= size//2:  # 超过一半前缀相同就认为是同一个project
        return True
    return False


def filter_line(lines):
    # 暂不考虑同一个project内的方法调用
    clean = [line for line in lines if not is_same_project(line[0], line[1])] # 过滤本地API函数
    user_cnt = defaultdict(int) #初始化字典元素个数为0
    for line in clean:
        user_cnt[line[0]] += 1 #统计一个方法中有多少API

    rm_entries = set([key for key in user_cnt if user_cnt[key] <= 2]) #筛选出调用的API数小于等于2的方法
    clean = [line for line in clean if line[0] not in rm_entries] #[['com/oneapm/basic_utilities/OrderingTest/testOrdering()', 'java/io/PrintStream/println(java.lang.String)'],...]
    return clean

def strip_user_prefix(name):
    return name[name.index('/')+1:]


def padding_seq(v):
    n = len(v)
    if n >= MAX_SEQ_LEN:
        return v[-MAX_SEQ_LEN:]
    else:
        return v + [0] * (MAX_SEQ_LEN - n)


def to_torch_sparse_tensor(sparse_mx):
    """Convert a scipy sparse matrix to a torch sparse tensor."""
    sparse_mx = sparse_mx.tocoo().astype(np.float32)
    indices = torch.from_numpy(np.vstack((sparse_mx.row, sparse_mx.col)).astype(np.int64))
    values = torch.from_numpy(sparse_mx.data)
    shape = torch.Size(sparse_mx.shape)
    return torch.sparse.FloatTensor(indices, values, shape)

def read_file():
    file_names = os.listdir(basedir)
    for fname in file_names:
        path = os.path.join(basedir, fname)
        with open(path, 'r', encoding='UTF-8') as f:
            '''同一个文件内的属于一个project
               使用文件名作为project id，因为存在相同项目的不同版本, 其prefix相同但文件名不同
               被调用者所属的project暂时没有考虑, 因为从数据中识别出项目名比较困难
            '''
            pname = fname[:-4]  # remove .txt
            project_id[pname] = len(project_id) # {'130e66a6d629a0fd37a3379952095b0e322767f4': 0, '146bfb0539ddc5bba829f2a321f4f4b42346a151': 1}

            p_id = project_id[pname]
            project_pre = 'PROJECT/'
            print(p_id, pname)  #项目索引号和项目名称

            lines = [line.strip().split('#') for line in f.readlines()] #[方法名,API]

            clean_lines = filter_line(lines) #[方法名,API],这里面的方法都是API数大于2的
            #print(clean_lines)

            if clean_lines:
                project_pre += clean_lines[0][0].split('/')[0]

            project_name.append(project_pre)

            users = set()
            for raw_pre, suc in clean_lines:

                pre = pname + '/' + raw_pre  # 加上文件名, 区分不同版本的同名API

                if pre not in user_method_id:
                    user_method_id[pre] = len(user_method_id) # 给每个项目的方法重新分配唯一索引号，users中保存索引号，user_name保存方法名称如：UMETHOD/toHexString(byte[])
                    users.add(user_method_id[pre])
                    user_name.append(raw_pre)
                if suc not in item_method_id:  # callee不存在关心不同版本的问题
                    item_method_id[suc] = len(item_method_id) # 给每个API重新分配唯一索引号，item_name保存API名称如：IMETHOD/append(java.lang.String)
                    item_name.append(suc)

                um_id = user_method_id[pre] # 获取方法索引
                im_id = item_method_id[suc] # 获取API索引
                invocation_matrix[um_id].append(im_id) # um_id调用了im_id，invocation_matrix格式{0: [0, 1, 2, 3], 1: [0, 4, 5, 3],...}

                # add class node for caller/user methods
                c_name = '/'.join(pre.split('/')[:-1])  # remove method name,如2c29a97a1794c023786273153e4d1220a90d1e44/org/apache/stanbol/commons/solr/managed/impl/ManagedSolrServerImpl

                if c_name not in class_id:
                    class_id[c_name] = len(class_id)    # 给每个类分配索引
                    proj_have_class[p_id].append(class_id[c_name])  # add a new class to project
                    class_name.append('UCLASS/'+c_name.split('/')[-1])  # remove the project name prefix
                cid = class_id[c_name]

                class_have_method[cid].append('u%d' % um_id)  # 区分是user method还是item method'''

                # add class node for callee/item methods
                c_name = '/'.join(suc.split('/')[:-1])
                if c_name not in class_id:
                    class_id[c_name] = len(class_id)
                    class_name.append('ICLASS/'+c_name.split('/')[-1])
                cid = class_id[c_name]
                class_have_method[cid].append('t%d' % im_id)
            proj_have_users.append(list(users))
            print('project have user methods:', len(users))

    print('method count','API count','class count')
    print(len(user_method_id), len(item_method_id), len(class_id))
    assert len(user_name) == len(user_method_id)
    calls = sum([len(val) for val in invocation_matrix.values()])
    print('invocation matrix counts: ', calls)
    rm_entries = set()
    for uid in invocation_matrix:
        if len(invocation_matrix[uid]) <= 1:
            rm_entries.add(uid)
    for uid in rm_entries:
        invocation_matrix.pop(uid, None)

    key = list(invocation_matrix.keys())[0]
    print(user_name[key], invocation_matrix[key])

# 定义我的数据集
def load_mydata(dataset_name):
    name = './data/%s-mydata.pk' % dataset_name
    if not os.path.exists(name):
        print('building dataset from raw file.')
        #os.makedirs(name)
        read_file()
        #user映射表user_method_id，item映射表item_method_id,方法与API调用关系invocation_matrix
        data = MyDataset(project_id, user_method_id,item_method_id, invocation_matrix, proj_have_users)
        data.adj = build_my_adj_matrix(data)
        build_my_word_embedding(data)
        with open(name, 'wb') as f:
            pk.dump(data, f)
    with open(name, 'rb') as f:
        data = pk.load(f)
        print('load dataset from disk.')
    data.adj = to_torch_sparse_tensor(data.adj)
    return data

# 构建邻接矩阵
def build_my_adj_matrix(data):
    n = len(data.item_method_id)
    A = sp.dok_matrix((n, n), dtype=np.float32)
    for user, items in tqdm(data.invocation_mx.items()):
        for i in range(len(items)):
            for j in range(i+1,len(items)):
                if A[items[i],items[j]] == 0:
                    A[items[i],items[j]] = A[items[j],items[i]] = 1.0/n
                else:
                    A[items[i], items[j]] = A[items[j], items[i]] = A[items[j], items[i]] + 1.0/n


    def normalize_adj(adj):
        rowsum = np.array(adj.sum(1))

        d_inv = np.power(rowsum, -1).flatten()
        d_inv[np.isinf(d_inv)] = 0.
        d_mat_inv = sp.diags(d_inv)

        norm_adj = d_mat_inv.dot(adj)
        return norm_adj

    #L = normalize_adj(A + sp.eye(A.shape[0]))
    print('normalized adj')
    return A

# 构建向量空间模型
def build_my_word_embedding(dataset):
    sents = item_name
    parser = LexParser(sents)
    vec = [parser.vectorize(name) for name in sents]
    print('build_my_word_embedding==>vec',vec)
    max_len = max([len(s) for s in vec])
    print('max sent len:', max_len)
    dataset.lookup_index = torch.LongTensor([padding_seq(s) for s in vec])

    dataset.word_pre_emb = torch.stack([torch.from_numpy(
        emb)for emb in parser.pre_embedding])
    dataset.vocab_sz = len(parser.vocab)

    dataset.user_pre_emb = torch.stack([torch.from_numpy(
        parser.get_embedding(name)) for name in user_name])
    dataset.item_pre_emb = torch.stack([torch.from_numpy(
        parser.get_embedding(name)) for name in item_name])


if __name__ == '__main__':
    dataset_name = 'projects-APIs'
    data = load_mydata(dataset_name)
    print(data.nb_proj)
    print(data.nb_user)
    print(data.nb_item)
    print(len(data.lookup_index))
    print(len(data.adj))