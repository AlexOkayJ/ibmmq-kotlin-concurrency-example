<meta name="google-site-verification" content="t03ICop6NhPIuqR21x49l2cstXGqyEi8xfRYhFjvMUw" />

# ibm-mq-kotlin-concurrency
### FOR WHAT
As a backend developer, when starting using Kotlin, I always lacked examples of it's usage for enterprise backend tasks.
Present example shows how the Kotlin can be used with coroutine channels for asynchronous processing of IBM MQ messages in non-hierarchical CSP style - Kotlin might be replaced with any other MQ manager or message broker like Kafka, of course.

Main goal is to show how Kotlin non-blocking coroutines can be used for blocking tasks, when you have three tasks, each of those needs 5 seconds to be done (insert to database, send to rest etc.), in a typical case of consistent execute you need 15 seconds for such work. If you use threads, you will only need 5 seconds, but you will be blocking your threads, which is a very expensive operation. In our sample you need just 5 seconds for such work without blocking any thread.
In my opinion, this is a great method for messaging systems - we can process numerous messages per second, not caring about memory overhead or thread blocking, as opposed to processing all the messages the same way.

### USAGE
To start the sample you need IBM MQ manager - it can be taken from DockerHub as a docker image for start queue manager with a default configuration and a listener set up on port 1414 by the following command:

```sh
docker run \
  --env LICENSE=accept \
  --env MQ_QMGR_NAME=QM1 \
  --publish 1414:1414 \
  --publish 9443:9443 \
  --detach \
  ibmcom/mq
```
More details here: <https://github.com/ibm-messaging/mq-container>

## RUN LISTENER
To start a project you need to print  `gradle bootRun`. 
You can now see the "Hello world bro" message printed on your console every second.

## HOW IT WORKS 
First we a configuring our listener in a typical spring way. In this sample I'm adding a BackOff property to reconnect - by doing so, we will try to reconnect every 5 seconds if we catch any troubles with the network or brocker. I also recomend moving the recovery interval value to a property file.
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
We also have a listener and sender configured to the same que, so i configured very simple sender from JmsTemplate bean and scheduled it to send message every second:
```
@Scheduled(fixedRate = 1000)
fun task() = runBlocking {
        sender.sendMessage("Hello world bro")
}
```
This part is quite simple.

What's interesting is our MessageWorker class which have three channels:
1) inMessageChannel - used to receive message from listener
2) workerChannel - used to proceed the message
3) resultChannel - used to send the processed message

First of all we added our thread pool - I moved number of threads and workers (coroutines) to a properties file for convince.

MessageProcess is a function initializing our workers and downloader. 
In this case we repeat the worker function n times to initialize our worker process in this case n is count of concurrent process - as oppose to using threads, we can initialize hundreds of worker functions not worrying of memory overhead. See comments to understand the idea better.
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
For better understanding of MessageWorker class functioning I recommend watching Roman Elizarov's lecture on this topic from KotlinConf 2018  `https://www.youtube.com/watch?v=a3agLJQ6vt8`
and also his article ` https://medium.com/@elizarov/deadlocks-in-non-hierarchical-csp-e5910d137cc`
