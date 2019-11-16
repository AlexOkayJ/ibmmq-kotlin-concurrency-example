# ibm-mq-kotlin-concurrency
### FOR WHAT
I am backend developer and when I started using Kotlin, I constantly lacked examples of how it can be used for enterprise backend tasks.
This example shows how you can use Kotlin and channels for asynchronous processing of IBM MQ messages in non-hierarchical CSP style, and of course you can replace it with another MQ manager or message broker like Kafka.

Main goal of this is use Kotlin non blocking coroutines for blocking tasks, when you have three tasks where every task need 5 second for have been done (insert to database, send to rest etc.) in typically way with consistent execute you need 15 second for this work if u use threads u need 5 second but you block threads and this is very expensive operation in our sample you need just 5 second for this and you don't block thread.
And i think its great for messaging because usually when we work with some message system all messages are being processed in the same way, so we can process numerous messages per second by this way without care about memory overhead or block threads.

### USAGE
For start this sample you need IBM MQ manager. You can take it from DockerHub as a docker image you can run a queue manager with the default configuration and a listener on port 1414 using the following command. :
```sh
docker run \
  --env LICENSE=accept \
  --env MQ_QMGR_NAME=QM1 \
  --publish 1414:1414 \
  --publish 9443:9443 \
  --detach \
  ibmcom/mq
```
for details see: <https://github.com/ibm-messaging/mq-container>

## RUN LISTENER
For start project you need print gradle bootRun.
You can see message "Hello world bro" print on you console every second.

## HOW IT WORKS 
First we configured our listener in typical spring way, in this sample i added BackOff property for try to reconnect when we catch some troubles with network or our broker, so we will try to reconnect every 5 second. I recommend move recovery interval value to property file.
```
@Bean
fun listenerFactory(configurer: DefaultJmsListenerContainerFactoryConfigurer, connectionFactory: ConnectionFactory): JmsListenerContainerFactory<*> {
        val factory = DefaultJmsListenerContainerFactory()
        factory.setBackOff(FixedBackOff())
        factory.setRecoveryInterval(5000)
        configurer.configure(factory, connectionFactory)
        return factory
}
```
Also we have listener and sender configured to the same que, so i configured very simple sender from JmsTemplate bean and scheduled it to send message every second:
```
@Scheduled(fixedRate = 1000)
fun task() = runBlocking {
        sender.sendMessage("Hello world bro")
}
```
It's quite simple part. 

Interesting is our MessageWorker class which have three channels:
1) inMessageChannel - we send message to this channel from listener
2) workerChannel - channel for do some work with message
3) resultChannel - channel for send work result

First of all we added our thread pool I moved number of threads and workers (coroutines) to properties file for convince.

MessageProcess is function which initialize our workers and downloader. 
We repeat n times worker function for initialize them, opposite to threads we can initialize hundreds of worker functions, 
and don't care about memory overhead. I wrote comments for better understand the idea.
```
//Select message and send them to channel or if it income from resultChannel
//we can answer to our MQ manager for example.
private fun CoroutineScope.downloader(
        inMessages: ReceiveChannel<String>,
        workChannel: SendChannel<String>,
        resultChannel: ReceiveChannel<String>
    ) = launch {
        while (true) {
            select<Unit> {
                resultChannel.onReceive { resultMessage ->
                    sendResult(resultMessage)
                }
                inMessages.onReceive { inMessage ->
                    workChannel.send(inMessage)
                }
            }
        }
}
//Read message from workChannel and do some work with them, uploadContent is suspend fun
//it can be call to database, rest or whatever you want
private fun CoroutineScope.worker(
        workChannel: ReceiveChannel<String>,
        resultChannel: SendChannel<String>
    ) = launch {
        for (message in workChannel) {
            val result = uploadContent(message)
            resultChannel.send(result)
        }
}
//Repeat N times worker fun it will be work concurrently so it is not guaranteed
//message order, we can poll message 1,2,3 and our answer will be 2,1,3 for example. 
private fun CoroutineScope.messageProcess(inMessages: ReceiveChannel<String>) {
        val workers = workersNumber.toInt()
        val workChannel = Channel<String>()
        val resultChannel = Channel<String>(1) 

        repeat(workers) { worker(workChannel, resultChannel) }
        downloader(inMessages, workChannel, resultChannel)
}
```    

#### Also
For understand how MessageWorker class is work
I  recommend watch Roman Elizarov talk about this pattern from KotlinConf 2018  `https://www.youtube.com/watch?v=a3agLJQ6vt8`
and after read this Roman article ` https://medium.com/@elizarov/deadlocks-in-non-hierarchical-csp-e5910d137cc`
