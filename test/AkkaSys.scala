package ddbt.test
import ddbt.lib._
import org.scalatest._

object Consts {
  import WorkerActor._
  import Messages._
  val m0 = MapRef(0)
  val m1 = MapRef(1)
  val ev1 = TupleEvent(TupleInsert,"S",List(1))
  val ev2 = TupleEvent(TupleInsert,"T",List(2))
  val ev3 = TupleEvent(TupleInsert,"U",List(3))
  val ev4 = TupleEvent(TupleInsert,"V",List(4))
  val f1 = FunRef(1)
  val f2 = FunRef(2)
  val f3 = FunRef(3)
}
import Consts._

class Worker extends WorkerActor {
  import WorkerActor._

  val map0 = K3Map.make[Long,Long]()
  val map1 = K3Map.make[Double,Double]()
  val local = Array[Any](map0,map1)

  def forl(f:FunRef,args:Array[Any],co:Unit=>Unit) = f match {
    case f1 => val n=args(0).asInstanceOf[Int]; for (i<-0 until n) add(m0,i.toLong,1L); co()
  }
  def aggl(f:FunRef,args:Array[Any],co:Any=>Unit) = f match {
    case f2 => co(map0.aggr((k:Long,v:Long)=>v))
  }
}

class Master extends Worker with MasterActor {
  import WorkerActor._
  import Messages._
  import scala.util.continuations._

  val dispatch : PartialFunction[TupleEvent,Unit] = {
    case `ev1` => reset {
      add(m0,1L,1L); add(m0,2L,2L); add(m1,1.0,1.0)
      _barrier; _barrier; _barrier; // sequential multi-barriers test
      assert(1==_get[Long,Long](m0,1L),"Remote add")
      assert(2==_get[Long,Long](m0,2L),"Remote add")
      assert(1.0==_get[Double,Double](m1,1.0),"Remote add")
      clear(m0); _barrier
      assert(0==(_get[Long,Long](m0,1L)+_get[Long,Long](m0,2L)),"Clearing")
      assert(1.0==_get[Double,Double](m1,1.0),"Clean one")
      deq
    }
    case `ev2` => reset {
      clear(m0)
      val n = 500
      foreach(m0,f1,n) // remote call test
      _barrier // count, we should have 1000, (not true if we remove barrier)
      val n2 = _aggr[Long](m0,f2) // remote aggregation test
      println("Added "+n+"*workers elements")
      println("Remote aggregation : "+n2)
      println("Collect+local count: "+(0L /: _toMap(m0).asInstanceOf[Map[Long,Long]]){ case (a,(k,v)) => a+v})
      deq
    }
    case `ev3` => deq // trampoline test
    case `ev4` => println("Trampoline OK"); deq
    case e:TupleEvent => deq
  }
  def onSystemReady() {}
}

class AkkaSys extends FunSpec {
  import akka.actor.{Props,ActorSystem}
  import Messages._
  import WorkerActor._

  it("Akka simple tests") {
    val system = ActorSystem("DDBT")
    val m = system.actorOf(Props[Master])
    val ws = (0 until 5).map { i => system.actorOf(Props[Worker]) }
    // setup members
    m ! Members(m,ws.map{w=>(w,List(MapRef(0),MapRef(1)))}.toArray)
    m ! SystemInit
    // custom external events
    m ! ev1
    m ! ev2
    (0 until 20000).foreach { _ => m ! ev3 }
    m ! ev4
    // finalize

    val wait = 10000
    val timeout = akka.util.Timeout(wait)
    val r = scala.concurrent.Await.result(akka.pattern.ask(m,EndOfStream)(timeout), timeout.duration).asInstanceOf[(Long,Any)]._1
    info("Test passed in "+ddbt.Utils.time(r)+"s")
    system.shutdown()
  }
}

// =============================================================================
//
// WARNING: INCORRECT LEGACY ONLY BELOW, SEE docs/draft/m4.tex FOR REFERENCE.
//
// =============================================================================
/*
  // ---- legacy behavior
  // At present, we do not support group management, this is done at setup
  // as with previous implementation, worker should support membership.
  // members(self,props.map{p=>context.actorOf(p)})

  // ---- convenience wrapper for debugging (should be rewritten by compiler instead)
  import scala.language.implicitConversions
  implicit def mapRefConv(m:MapRef) = new MapFunc[Any](m)
  class MapFunc[P](m:MapRef,p:Int= -1,pk:P=null) { private val w=WorkerActor.this
    def get[K,V](k:K,co:V=>Unit) = w.get(m,k,co)
    def add[K,V](k:K,v:V) = w.add(m,k,v)
    def set[K,V](k:K,v:V) = w.set(m,k,v)
    def clear() = w.clear(m,p,pk)
    def slice(part:Int, partKey:Any) = new MapFunc(m,part,partKey)
    def foreach[K,V](f:FunRef,args:Array[Any]) = w.foreach(m,f,args)
    def aggr[R:ClassTag](f:FunRef,args:Array[Any],co:R=>Unit) = w.aggr(m,f,args,co)
  }

import scala.concurrent.{Future,Await}
import akka.pattern.ask
import akka.actor.Props

class MasterActor(props:Array[Props]) extends WorkerActor {
  init(self,props.map{p=>context.actorOf(p)})
}

LEGACY ARCHITECTURE OVERVIEW (push-based flow)
---------------------------------------------------

                  Source (streams)
                     |
                     v
Supervisor <---> Serializer -----> Storage
    ^          (broadcasting) <--- (setup tables)
    |                |
    |                v
    |             /+-+-+\
    |            / | | | \
    +---------- W W W W W Workers (partitioned)
                 \ | | | /
                  \+-+-+/
                     |
                     v
                  Display (query result)


The SOURCE represents the input stream, conceptually external to the system. It
could be: reading a file or receiving binary data from the network.

The SERIALIZER is the entry point all messages to the system. Its only duty is
to serialize and broacast all messages it receive: from source and from other
nodes that need to communicate.

The STORAGE has two roles:
- storage (database) for static relations (used at initialization)
- traditional database (preferably: compactness, easy recovery) or (WAL) log
  storing the stream. Purpose: recover from nodes crash, if necessary. 

The work is distributed to WORKERS that are responsible for processing the tuple
applying the deltas it generates to their local storage and exchange messages
with peers, if the peer computation requires their internal state. More
specifically, each node compute foreach modified map whether they own the slice
in which modification should happen. If:
- Yes: compute the delta, possibly waiting data from other nodes
- No : find owner, send it all _aggregated_ data related to modification

Example: SELECT COUNT(*) FROM R,S where R.r=S.s ==> maps mCOUNT,mR(r),mS(s)
    Assume 2 workers W1,W2 partitionned using modulo 2 on id, and mCOUNT on W1.
    When tuple <serial,+R(3)> arrives, owner=W1: mR is updated on W1.
    To compute mCOUNT+=1[R(3)]*COUNT(mS),
    - W1 does 1[R(3)]*COUNT(mS%2==1) and wait data from W2
    - W2 detects that its map is used, compute 1[R(3)]*COUNT(mS%2==0) and sends
      it to W1
    - W1 receive data from W2, completes its computation and update mCOUNT
The key here is to notice that since W1,W2 receive +R(3), they can pre aggregate
locally before agregating between nodes.

The DISPLAY is the receiver of the query maps, these can be send by all workers
whenever requested (request message needs to go through serializer).

Finally the SUPERVISOR coordinates all the nodes by handling failures and
checkpointing. To do that, all system messages are passed to the serializer.
This guarantees a coherent snapshot of the system.

FAULT TOLERANCE
---------------------------------------------------
Gap property: the application can sustain a gap in the stream without affecting
too much the result. Gap can be measured in time, #tuples, relative size, ...

Checkpointing: broadcast message for all workers and storage
- Workers write their map to permanent local storage.
- Storage as relations: make a snapshot (lock tables, ...)
- Storage as stream: if checkpoiting succeeds, discard stream up to check point

Failures
- Serializer fails: system waits for recovery. If the source does not enqueue
  messages and not Gap, system is stale and need to be reset.
- Storage fails: recovery of system becomes impossible.
  When detected, if has Gap property and worth it checkpoint (else useless).
- Supervisor or display fails: does not affect computation
- Worker fails: if Gap property or system could sustain a burst
  1. Enqueue all message at serializer
  2.a If storage holds the stream
      - Restore worker from its stable storage (tx_old)
      - Replay the stream (tx_old->tx_current) with help of storage and coworkers
  2.b If storage holds the relations
      - Recompute all the maps from the relations (we do not need local storage here)
  3. Release queue at serializer (burst), continue processing

**/

/*
abstract sealed class Msg
// Data
case object MsgEndOfStream extends Msg
case class MsgTuple(op:TupleOp,tx:Long,data:List[Any]) extends Msg
//case class MsgEndOfTrx(tx:Long,s:Long) extends Msg // all tuples have been exchanged between two workers for the sth statement of transaction tx
//case class MsgBulk(op:TupleOp,tx:Long,s:Long,data:List[List[Any]]) extends Msg // bulk transfer between two workers
// System
case object MsgNodeUp extends Msg    // worker is initialized
case object MsgNodeReady extends Msg // worker has received all tables content
case object MsgNodeDone extends Msg  // worker has received end of stream from all its sources
case class MsgState(num:Long,mem:Long) extends Msg // worker status (#processed tuples, memory usage)
case class MsgRate(state:Long,view:Long) extends Msg // how often nodes send state (to supervisor) and result (to display)
// case class MsgCheckpoint extends Msg
*/