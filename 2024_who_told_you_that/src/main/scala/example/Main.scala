package example

import difflicious.DiffResult

import java.util.concurrent.{CountDownLatch, Executor, Executors}

@main
def main(): Unit = {
  val threadpool = Executors.newFixedThreadPool(4)
  val myContext = ThreadLocal.withInitial(() => "no_user")
  
  val cdl = new CountDownLatch(1)
  
  threadpool.execute(() => {
    myContext.set("user1")
    println(s"${Thread.currentThread().getName}: step 1 for ${myContext.get()}")
    
    threadpool.execute(() => {
      println(s"${Thread.currentThread().getName}: step 2 for ${myContext.get()}")
      
      threadpool.execute(() => {
        println(s"${Thread.currentThread().getName}: complete for ${myContext.get()}")
        cdl.countDown()
      })
      
    })
  })
  
  cdl.await()
  threadpool.shutdownNow()
}
