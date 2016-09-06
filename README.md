RxCacheSamples
==============
This project shows how to use [RxCache](https://github.com/VictorAlbertos/RxCache/tree/2.x) for both Android and Java projects using [RxJava2](https://github.com/ReactiveX/RxJava/tree/2.x). 

sample_data
-----------
Java module which actually implements RxCache, it acts as a central data repository making http calls to Github api using [Retrofit](https://github.com/square/retrofit). 

sample_android
--------------
Android module which uses sample_data module.

sample_java
-----------
Java module which uses sample_data module.
