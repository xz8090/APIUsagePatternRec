# APIUsagePatternRec
## Project Parts
项目组成与说明
### Code2APISeq: API parsing and extraction
Code2APISeq：API解析与提取部分
#### Requirement JDK1.8. And download the dependencies through maven, set the project path in the Code2APISeq.java file, and start the project according to the main function.
要求JDK1.8。
通过maven下载相关依赖，然后在Code2APISeq.java文件中设置项目地址，并根据main函数启动项目。

### GLBPAPI: Model training and experimental verification
GLBPAPI：模型训练与实验验证部分
#### Requirement Python3.5+. First, store the resolved API file in the “projects-APIs” folder of the project, and then execute the ` python feature_extraction. py ` Generate the initial feature file in the "data" folder. The training parameters are configured in GLAPI.py, and model training and output verification are performed through 'python GLAPI. py'. The log is saved in the "log" folder.
要求Python 3.5+。
首先将解析的API文件存放在项目中的projects-APIs文件夹下，然后执行`python feature_extraction.py`生成data文件夹下的初始特征文件。
再GLAPI.py中配置训练参数，通过`python GLAPI.py`进行模型训练与输出验证。日志保存在log的文件夹下。

### APIRecommend: IDEA based API recommended plug-in
APIRecommend：基于IDEA的API推荐插件
#### This project is based on a recommendation plug-in developed by IDEA. It implements real-time code parsing through psi technology, and outputs the GLBPAPI project features as a back-end service. It calls this service to obtain the API features in real time to make recommendations. Import IDEA and run it directly. For detailed design structure, please click [API Usage Recommended Plug in Design](./APIRecommend/README.md)
该项目基于Idea开发的一款推荐插件，通过psi技术实现实时代码的解析，同时将GLBPAPI项目特征输出作为后端服务，通过调用该服务实时获取API特征，从而进行推荐。导入IDEA后直接运行即可。
详细设计结构请看[API使用模式推荐插件设计](./APIRecommend/README.md)



