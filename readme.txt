这是一个复杂的工程组，存在各种依赖关系

如果想执行起来，需要再根目录上执行命令：
mvn clean install 
执行环境是java1.8，至于1.7是否可以没有试过

这个大工程外面还依赖easymongo工程，别的就不依赖了

Spring Boot升级到了1.4，因为spring session依赖于1.4
