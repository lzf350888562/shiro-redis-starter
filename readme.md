项目背景: 为实现状态共享以及方便项目以后水平扩容, 欲将验证授权数据以及session缓存到集中式缓存redis中. 

因为项目使用的是shiro, 而市面上相关整合shiro与redis的开源框架shiro-redis存在两个问题:

1. 框架底层另外创建了jedis客户端操作redis, 如果项目本身使用了spring-data-redis, 将同时存在多个客户端, 并且不能利用高版本spring-data-redis中更高性能的Lettuce;
2. 项目中如果使用了spring-devtools, 因为其实现原理为通过不断替换RestartClassloader来实现热加载, 与shiro-redis对存入redis的对象使用jdk原生反序列化, 生成的对象被新替换的RestartClassloader加载, 会导致Subject#getPrincipal方法获取主体并强转失败, 报错ClassCastException

> 全限定类名与其类加载器决定类是否相同

既然是jdk原生序列化与devtools的冲突, 但又不想放弃devtools, 只能改变序列化方式了.

shiro并没有直接使用servlet中的session, 而是自己定义Session接口, 实际使用的是SimpleSession.....

todo...


