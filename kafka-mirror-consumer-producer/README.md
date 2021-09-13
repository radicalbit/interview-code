## Description
The purpose of this application is to mirror values between different Kafka topics.  
As it can be seen in `MirrorServiceRunner`, the program receives one or more pairs each composed of a source topic and output topic.
The goal is to send every record read in the source topic to its relative output topic.  
There is also the possibility to include a limit to the maximum number of records to mirror.
Once the limit is reached, messages on that topic will no longer be mirrored.
### Problems
There are integration tests present and they all pass.
However, once the application is run on real examples, some issues arise.
Specifically, when having to mirror multiple pairs of source and output topics, with no upper limit on the number of records, only one of the pairs gets mirrored.  
What could be the issue?
