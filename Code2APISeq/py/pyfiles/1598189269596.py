import utils

if __name__=='__main__':
    matrix = [[0, 0, 0, 0, 0, 0, 0, 0],[0, 0, 0, 0, 0, 0, 1, 1],[0, 0, 0, 0, 0, 0, 0, 0],[0, 0, 0, 0, 3, 0, 0, 0],[0, 0, 0, 3, 0, 0, 0, 0],[0, 0, 0, 0, 0, 0, 0, 0],[0, 1, 0, 0, 0, 0, 0, 1],[0, 1, 0, 0, 0, 0, 1, 0]]
    utils.adMmatrix2Img(matrix,'C:/Users/Administrator/git/repository/Code2APISeq/pngs/1598189269596.png')
    print('finish')