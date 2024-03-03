package example

import java.util.concurrent.{CountDownLatch, Executor, Executors}
import Context.CONTEXT

@main
def capture(): Unit = {
  val ex = Executors.newFixedThreadPool(4)
  val threadpool = new CurrentContextExecutor(ex)
  
  val cdl = new CountDownLatch(1)

  threadpool.execute(() => {
    CONTEXT.set("user1")
    println(s"${Thread.currentThread().getName}: step 1 for ${CONTEXT.get()}")
    
    threadpool.execute(() => {
      println(s"${Thread.currentThread().getName}: step 2 for ${CONTEXT.get()}")

      threadpool.execute(() => {
        println(s"${Thread.currentThread().getName}: complete for ${CONTEXT.get()}")
        cdl.countDown()
      })
    })
  })
  
  cdl.await()
  val _ = ex.shutdownNow()
}

class CurrentContextExecutor(delegate: Executor) extends Executor:
  override def execute(task: Runnable): Unit =
    val wrappedTask = passContext(task)      // In submitting thread
    delegate.execute(wrappedTask)            // ..
    
  def passContext(task: Runnable): Runnable =
    val curCtx = CONTEXT.get()               // In submitting thread
    () => {                                  // ..
      val prevCtx = CONTEXT.get()            // Will be executed in next executing thread
      try                                    // ..          
        CONTEXT.set(curCtx)                  // ..      
        task.run()                           // ..
      finally                                // ..
        CONTEXT.set(prevCtx)                 // ..
    }


object Context:
  val NO_USER = "no_user"
  val CONTEXT = ThreadLocal.withInitial(() => NO_USER)

