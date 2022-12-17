import numpy as np
from tqdm import tqdm
from collections import defaultdict


class MyDataset:
    def __init__(self, project_id, user_method_id, item_method_id, invocation_matrix, users, adj=None):
        self.nb_proj = len(project_id)
        self.nb_user = len(user_method_id)
        self.nb_item = len(item_method_id)
        self.project_id = project_id
        self.user_method_id = user_method_id
        self.item_method_id = item_method_id
        self.invocation_mx = invocation_matrix  # 方法与API的邻接矩阵
        self.proj_have_users = users
        self.adj = adj          # API的邻接矩阵
        self.train_dict = {}    # 筛选出需要测试的方法后剩余的方法及API，格式{0:[0,1,2],...}
        self.test_dict = {}     # 筛选出来的方法及API，格式{0:[3,4,5]}
        self.train = []         # train_dict格式转换成[(0,0),(0,1),(0,2),...]
        self.test_user2proj = {}# test_dict中每个方法编号映射到具体项目编号上，格式转换成[(0,8),(1,8),(2,2),...]
        self.config = (2, 2)
        self.user_pre_emb = []
        self.item_pre_emb = []
        self.other_pre_emb = []
        self.lookup_index = []
        self.word_pre_emb = []
        self.vocab_sz = 0
        self.FD={}
        #self.split_data('C2.2')

    def split_data(self, conf):
        """conf: C1.1 C1.2"""
        self.config = (int(conf[1]), int(conf[3]))
        np.random.seed(0)
        test_proj_id = set(np.random.choice(range(self.nb_proj), int(self.nb_proj*0.2), replace=False))
        total_users = sum([len(val) for val in self.proj_have_users])
        print('total user methods:{}, test_proj:{}'.format(total_users, test_proj_id))

        def get_test_user(user_id, k):
            gt_users = []
            for uid in user_id:
                if len(set(self.invocation_mx[uid])) <= k:
                    self.train_dict[uid] = self.invocation_mx[uid]
                else:
                    gt_users.append(uid)
            return gt_users

        def add_to_test(gt_users, test_cnt, k):
            for uid in gt_users[-test_cnt:]:
                # add first k invocation for train, and the last for test
                self.train_dict[uid] = self.invocation_mx[uid][:k]
                self.test_dict[uid] = self.invocation_mx[uid][k:]
                self.test_user2proj[uid] = pid
            for uid in gt_users[:-test_cnt]:
                self.train_dict[uid] = self.invocation_mx[uid]

        for pid in test_proj_id:
            size = len(self.proj_have_users[pid])
            # print('test pid and user size', pid, size)
            if self.config[0] == 1:  # remove half user methods
                user_id = self.proj_have_users[pid][: size//2]
            elif self.config[0] == 2:  # keep all user methods
                user_id = self.proj_have_users[pid]
            if self.config[1] == 2:  # retain 4 invocations
                # use 0.2 percent methods per project as active methods for test
                gt_users = get_test_user(user_id, 5)  # users having more than 5 invocations
                test_cnt = len(gt_users) - int(len(gt_users)*0.8)
                add_to_test(gt_users, test_cnt, 4)
            if self.config[1] == 1:  # reserve the first invocation
                gt_users = get_test_user(user_id, 4)
                test_cnt = len(user_id) - int(len(user_id)*0.8)
                add_to_test(gt_users, test_cnt, 1)

        cnt = sum([len(val) for val in self.test_dict.values()])
        print('test set methods count:{}, invocations:{}'.format(len(self.test_dict), cnt))

        for pid in range(self.nb_proj):
            if pid in test_proj_id:
                continue
            for uid in self.proj_have_users[pid]:
                self.train_dict[uid] = self.invocation_mx[uid]

        # 构建(api1,api2)
        # self.train = [(uid, tid) for uid, val in self.train_dict.items() for tid in val]
        print('load train datas ...')
        self.train = [(val[i], val[j]) for uid, val in tqdm(self.train_dict.items()) for i in range(len(val)) for j in range(i+1,len(val))]
        # self.train = [(i,j) for i in tqdm(range(len(self.adj))) for j in range(i+1,len(self.adj)) if self.adj[i,j]!=0]
        print('train set methods count:{}, invocation: {}'.format(len(self.train_dict), len(self.train)))

    # 负采样：uid是API节点，寻找num个与uid不相连的API节点
    def sample_negative_item(self, api1, num, co_frequency=0):
        neg_item = []
        while True:
            api2 = np.random.randint(0, self.nb_item)
            co_count = 0
            # 计算共现频率
            for uid, val in self.train_dict.items():
                if api1 in val and api2 in val:
                    co_count += 1
            if co_count <= co_frequency:
                neg_item.append(api2)
            #if self.adj[api1,api2]==0:    # 邻接矩阵对应权重为0
                #neg_item.append(api2)
            if len(neg_item) == num:
                break
        return neg_item

    def shuffle_train(self):
        np.random.shuffle(self.train)

    def gen_batch(self, batch_size, neg_size):
        m = len(self.train) // batch_size
        for i in range(m):
            batch = self.train[i*batch_size: (i+1)*batch_size]
            apinodes, pos_items = zip(*batch)
            # neg_items list: k * batch_sz
            neg_items = np.asarray([self.sample_negative_item(uid, neg_size)
                                   for uid in apinodes]).transpose().flatten()
            yield np.asarray(apinodes), np.asarray(pos_items), neg_items


def get_calls_ditribution(dataset):
    nb_calls = defaultdict(int)
    for pid in range(dataset.nb_proj):
        item_size = [len(dataset.invocation_mx[uid]) for uid in dataset.proj_have_users[pid]]
        for s in item_size:
            nb_calls[s] += 1
    s = 0
    for calls, num in nb_calls.items():
        if calls <= 6:
            print('user having {} API calls count as: {}'.format(calls, num))
        else:
            s += num
    print('user having more than 6 API calls count as: {}'.format(s))
