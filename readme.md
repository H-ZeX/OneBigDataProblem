### 题目要求

- 100G的文本文件，找出第一个不重复的词，找到的结果不允许出现错误（即不可以使用有出错概率的算法）
- 只允许扫描一遍原文件
- 尽可能少的IO
- 内存限制16G
- 假设：文件中一行一个词

### 概述

- 基本思路：分桶+逐个桶统计
- 两种操作子：`split`和`filter`
- `split`
   - 作用于一个输入文件
   - 使用`hashFunction`，将该文件拆分到一组目录中，每个目录中放一个拆分后的文件（目录list中可以有重复的目录，从而可以所有的文件都放在同一个目录中）
   - 因为使用的是`hashFunction`，所以相同的词会被分到同一个文件中
   - 可能会有分桶不均匀的情况，所以对于过大的文件，递归分桶
- `filter`逐个读取中间文件，对每个文件获取该文件的第一个不重复的词（可能是null），然后遍历所有`filter`得到的结果，就可以得到最终的结果。

### `split`

- 根据最大可用内存计算出最小剩余可用内存（默认为0.1倍最大内存）和每个中间文件的允许大小（默认为0.75倍最大内存），允许用户通过命令行参数指定这两个数值
- 对于每个桶，在内存中维护一个HashMap（而不是Trie）。不断读入源文件，按照hash，放进对应的HashMap
- 需要定期的flushToDisk。做法就是：当可用内存低于最低可用内存时，就flushToDisk
- 每次在当前HashMap中发现重复词时，就可以标记该词为`deprecated`，在输出到文件时，将其`cnt`输出为2。之所以仍然需要输出到文件，是为标记其为`deprecated`，避免`filter`后面遇到该词时，认为其是只出现一次的词
- **不使用**独立的线程进行读取
  - 测试中发现，所有操作都在单个线程中时，磁盘IO并没有跑满，读取速度比较低
  - 在常规的程序中，一般可以使用另一个线程进行读取，push进BlockingQueue。这样就可以加快处理速度
  - 但由于在本题中并不适合，原因是
     - 内存是主要瓶颈，而独立线程读取需要buf
     - 由于内存不足而导致的GC时间也是影响程序速度的因素
- 输出不使用独立的线程，因为输出的目的是尽可能的清空内存，如果使用独立线程，那么需要同时保留当前已有的每个HashMap，并且建立并填充新的HashMap，从而需要更多的内存
- **由于题目中只有一个文件，所以只能利用一个磁头。如果文件分布在多个磁盘上，那么可以同时使用多个`split`操作子，并且把中间文件flush到多个磁盘上。从而实现并发读取和并发写入。这里的并发数只取决于磁头数目**
- 析构：为了在split结束时，尽可能早的回收占用的内存，所以需要析构，将`Split`类中的field置为`null`，然后使用`System.gc`建议JVM进行GC

### `filter`

- 因为每个文件都可以完全放入内存，所以可以直接使用HashMap进行筛选
- 如果中间文件分布在多个磁盘上，那么可以多个`filter`操作子一起运行

### `HashMap` vs `Trie`

- 因为需要对于任意输入都可用，所以每个`char`需要对应一个`node`，而每个node如果有children，就需要一个`HashMap`，来映射对应的`char`到对应的`node`
- 首先计算一下，当词的平均长度是100个char时，100G文件在最坏情况下需要多少个node
   - 假设只有小写字母——26个
   - 除了root，其他node都对应一个char，所以第k层有`26**(k-1)`个node
   - 为了达到`10**9`个词，那么需要有8层，因为第8层有大于`10**9`个node（第7层少于`10**9`）
   - 接下来还需要93层，因为第8层的每个node对应一个词，而一个词有100个char那么长
   - 那么就有大于`94*10**9`个node
- 对应的，如果使用HashMap，则是`100*10**9`个char。加上每个str对应一个HashMap中的slot，从而至少需要`10**9`个slot，每个slot对应于一个HashMap中的`node`
- 在Java中每个char是16bit，每个Object（`node`是Object）至少需要128bit。所以对应之下，使用Trie会带来更大的内存消耗

### `HashFunction`

- 这个程序允许最多递归split 4次（加上最初的split，就是5次split），第一次split使用的是`String::hashCode`
- 为了区分同一`HashCode`的不同字符串，并且因为`String::hashCode`并不是线性的——两个字符串拼接在一起，得到的新的字符串的hashCode并不等于原本的两个字符串的hashCode相加，即`hashCode(xy)!=hashCode(x)+hashCode(y)`，所以第n次split的hashFunction是：把原串等分成n个部分，分别求hashCode后相加
- 对于具体的场景，用户或许需要根据自己的数据特征定制`hashFunction`以尽可能避免或减少递归split

### 实测结果

- 情况一
   - 10G的文件，每个词0~1000个字符长度
   - 除了target，其他词都出现了两次及以上，并且都是相邻地出现两次及以上
   - 参数：1.6GB的内存，`0.70*1.6GB`的最大中间文件大小，`0.1*1.6GB`的最低剩余可用内存
   - split过程中分为10个中间文件，split过程中需要3次flushToDisk，总共需要`3*10+10+1=41`次IO
     - 一次读原文件
     - 每次flushToDisk需要写10个文件，从而`3*10`
     - filter的过程需要读10个文件
- 情况二
   - 862M的文件，每个词为0~1000字符长度
   - 在target之后，其他词以50%的概率出现一次，以50%的概率出现两次，并且都是相邻地出现两次
   - 参数：138MB的内存，`0.75*138MB`的最大中间文件大小，`0.1*138MB`的最低剩余可用内存
   - split过程中分为9个中间文件，split过程中需要10次flushToDisk，总共需要`9*10+9+1=100`次IO
- 情况三
   - 5.7G的文件，每个词为0~1000字符长度
   - target是第25个，从第26个开始，所有词大概率上是唯一的
   - 参数：900MB的内存，`0.70*900MB`的最大中间文件大小，`0.15*900MB`的最低剩余可用内存
   - split过程中分为10个中间文件，split过程中需要13次flushToDisk，总共需要`13*10+10+1=141`次IO

### 更好的方法

- 使用哈希函数使得如果用户没有指定对应于自己的数据集的哈希函数，那么可能出现“坏的情况”——数据切分后分布非常不均匀
- 如果我们借助merge sort的方法，可以这样做
   - 每次读取15G的数据，每个词组装成一个tuple（包含词本身、其源文件中的位置），在内存中排序后输出到一个文件
   - 每次，同时从这n个文件吸取15G/n的数据，在内存中做归并。如果发现某个词没有重复，那么就可以输出到结果文件
   - 排序该结果文件，获得第一个不重复的词
   - 这里存在的问题是：第二步可能因为数据分布而使得从一些文件中读进来的数据一直没有办法归并进去，使得这些数据一直占据内存，从而导致非常糟糕的内存使用情况和IO次数的增加
   - 可以这样解决问题：在第一步sort之后，我们存一些元数据（比如第一步产生的某文件的十个十等分点处的数据），那么第二步读入该文件的数据时，就可以通过对应的十等分点处的数据信息，决定这一次merge是否读取这个文件的这一个数据块
   - 决定某次读取要读取的数据块对应的文件的伪代码如下
      ```cpp
      // 获取下一个要读取的10个数据块所在的文件
      // 元数据文件(对应于`data`)中是这种数据`(a, n, m)`的一个数组
      // n代表第n个十等分点，m代表第m个文件
      // a是该十等分点的原始数据
      // 该数组已按照`a`进行排序
      int* get_next(meta[] data, int index, int data_len)
      {
      
          int* file_to_read = new int[10];
          for (int i = index; i < data_len && i < index + 10; i++) {
              file_to_read.append(data[i].m);
          }
          return file_to_read;
      }
      ```

### 可改进的点

- 使得中间文件尽可能的顺序IO
   - 如果直接使用文件系统，那么事实上一个文件是分散为多个块的
   - 如果可以使得整个文件连续分布，那么可能性能上会好一些

### 依赖说明

- 推荐使用JDK11，使用JDK8时，应当调大最小剩余内存、缩小每个中间文件的大小
