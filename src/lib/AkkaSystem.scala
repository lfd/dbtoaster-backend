package ddbt.lib

import scala.reflect.ClassTag
import akka.actor.{Actor,ActorRef}

/**
 * Model: fully asynchronous nodes. To provide a sequential-like model, the
 * master must execute event handlers on a separate (blocking) thread.
 * Distributed operations:
 * - Request/response (RR): the execution is paused into a continuation
 *   (in *Matcher classes) that will be resumed when the response is received.
 * - Remote calls (RC): context+closure identifier is sent, remote nodes
 *   execute the closure asynchronously.
 *
 * Synchronization:
 * - RR: if there is no pending request in the matcher, obviously all operations
 *   have been completed.
 * - RC: an acknowledgement must be produced at the end of the procedure. These
 *   are aggregated in a barrier (only when synchronization is needed).
 *
 * Barrier:
 * Nodes maintain two counters: CR:=#RC received, CS[n]=#RC sent to node n.
 * 1. Master broadcasts <start> to enable barrier mode on all nodes.
 *    At this point, the master is not allowed to send additional RC.
 * 2. In barrier mode, after every RC fully processed:
 *    if (there is no pending request in the matcher) {
 *      if (CR!=0 || CS!={0..0}) {
 *        send ack(CR,CS) to the master
 *        set CR=0, CS={0..0}
 *      }
 *    }
 * 3. When master receives ack(CR,CS), it sums with local counters.
 * 4. When (CR!=0 && CS!={0..0}) on master (there is no in-flight message), the
 *    master broadcast <stop> to disable barrier mode and continues processing.
 * Note 1: <start> counts as one RC to enforce barrier.
 * Note 2: during the barrier, the cache must be disable to avoid propagating
 *         stale informations after the barrier.
 *
 *
 * @author TCK
 */
 
/*
 * Current Issues: 
 * - K3Vars need to be put on a single host/stored on all hosts, acks need to be corrected
 * - Broadcast to all workers within the same host => worker < host < cluster hierarchy
 * - OnSystemReady needs not to go through network and ignore non-local updates
 * - Batching update and lookups (with timeout/explicit end-of-batch?)
 * - Use multiple timestamps and queue updates in front of maps?
 *   -> If so, use maps as timed key-value store?
 *   -> Expunge previous versions when timestamp completed
 * XXX: _INFER_ (encode) functional dependencies in the maps(?)
 */

/** Internal types and messages */
object WorkerActor {
  type MapRef = Byte;    def MapRef(i:Int):MapRef = i.toByte
  type FunRef = Short;   def FunRef(i:Int,internal:Boolean=false):FunRef = (if (internal) java.lang.Short.MIN_VALUE+i else i).toShort
  type NodeRef = Short;  def NodeRef(i:Int):NodeRef = i.toShort
  
  // Internal messages
  case class Members(master:ActorRef,workers:Array[(ActorRef,List[MapRef])]) // master can also be a worker for some maps
  case class Barrier(en:Boolean) // switch barrier mode
  case class Ack(to:Array[NodeRef],num:Array[Long]) // cumulative ack to master (to, count) : -1 for sent, 1 for recv
  //case class TellJoin(master:ActorRef) // ->worker: join the master
  //case object TellLeave // ->worker: ask for departure
  //case class Join(ms:List[MapRef]) // worker->master: join request
  //case object Leave // worker->master: departure request
  //case object Goodbye // worker is allowed to stop responding

  // Data messages
  case class Get[K](map:MapRef,key:K) // => Val[K,V]
  case class Val[K,V](map:MapRef,key:K,value:V)
  case class Add[K,V](map:MapRef,key:K,value:V)
  case class Set[K,V](map:MapRef,key:K,value:V)
  case class Clear[P](map:MapRef,part:Int,partKey:P)
  case class Foreach(f:FunRef,args:Array[Any])
  case class Aggr(id:Int,f:FunRef,args:Array[Any]) // => Sum
  case class AggrPart[R](id:Int,res:R)
}

/** Worker, owns portions of all maps determined by hash() function. */
abstract class WorkerActor extends Actor {
  import scala.collection.JavaConversions.mapAsScalaMap
  import WorkerActor._
  // ---- concrete maps and local operations
  val local:Array[M3Map[_,_]] // ref->local maps conversion
  def local_wr(m:MapRef,v:Any,add:Boolean) {} // write in local variable
  def local_rd(m:MapRef):Any = "<???>" // generic read a local variable
  def hash(m:MapRef,k:Any) = k.hashCode
  def forl(f:FunRef,args:Array[Any],co:Unit=>Unit) // local foreach
  def aggl(f:FunRef,args:Array[Any],co:Any=>Unit) // local aggregation

  // ---- membership management
  private var master : ActorRef = null
  private var workers : Array[ActorRef] = null
  private val owns = new java.util.HashMap[MapRef,Array[ActorRef]]()
  @inline private def owner(m:MapRef,k:Any):ActorRef = { val o=owns.get(m); val n=o.length; val h=hash(m,k); o((h%n+n)%n) }
  @inline private def members(m:ActorRef,ws:Array[(ActorRef,List[MapRef])]) {
    master=m; workers=ws.map(_._1); owns.clear;
    ws.flatMap(_._2).toSet.foreach { m:MapRef => owns.put(m,ws.filter(w=>w._2.contains(m)).map(_._1).toArray) }
    if (master==self) workers.foreach { w => w!Members(m,ws) }
  }
  // XXX: protected def addWorker(w:ActorRef,ms:List[Maps]) {}
  // XXX: protected def delWorker(w:ActorRef) {}

  // ----------------------------------------------------------
  // TODO: implement more efficient serializer
  // - possibly exchange 2 Arrays(Key,Val) instead of Scala Map
  // ----------------------------------------------------------
  // http://code.google.com/p/fast-serialization/
  // http://doc.akka.io/docs/akka/snapshot/java/serialization.html
  // https://github.com/romix/akka-kryo-serialization
  // https://github.com/talex004/akka-kryo-serialization
  // https://github.com/twitter/chill

  // ---- get/continuations matcher
  private object matcherGet {
    private var enabled = true // enable cache
    private val cache = new java.util.HashMap[(MapRef,Any),Any]()
    private val cont = new java.util.HashMap[(MapRef,Any),List[Any=>Unit]]()
    @inline def req[K,V](map:MapRef,key:K,co:V=>Unit) { // map.get(key,continuation)
      val o=owner(map,key);
      if (o==self) co(local(map).asInstanceOf[M3Map[K,V]].get(key)) // resolves locally
      else {
        val k=(map,key); val v=if (enabled) cache.get(k) else null;
        if (v!=null) co(v.asInstanceOf[V]) // cached
        else {
          val cs=cont.get(k); val c=co.asInstanceOf[Any=>Unit];
          cont.put(k,c::(if (cs!=null) cs else Nil))
          if (cs==null) o ! Get(map,key) // send request
        }
      }
    }
    @inline def res[K,V](map:MapRef,key:K,value:V) { // onReceive Val(map,key,value)
      val k=(map,key); if (enabled) cache.put(k,value); cont.remove(k).foreach{ _(value) }
      if (self!=master) bar.ack // ack pending barrier if needed
    }
    @inline def ready = cont.size==0
    @inline def clear(en:Boolean=true) = { cache.clear; enabled=en }
  }

  // ---- aggregation/continuations matcher
  private object matcherAggr {
    private var enabled = true // enable cache
    private val cache = new java.util.HashMap[(FunRef,Array[Any]),Any]()
    private val cont = new java.util.HashMap[Int,List[Any=>Unit]]
    private val sum = new java.util.HashMap[Int,(Int/*count_down*/,Any/*sum*/,(Any,Any)=>Any/*plus*/)]
    private val params = new java.util.HashMap[Int,(FunRef,Array[Any])]()
    private val uniq:Long = math.abs(self.path.toString.hashCode) // unique among the cluster and membership-independent
    private var ctr:Int = 0
    @inline private def getId(f:FunRef,args:Array[Any]):Int = { params.foreach{ case (k,v) => if (v==(f,args)) return k; }; ctr=ctr+1; (ctr*uniq).toInt }
    @inline def req[R](m:MapRef,f:FunRef,args:Array[Any],co:R=>Unit,zero:Any,plus:(Any,Any)=>Any) { // call aggregation ("f(args)",continuation) on map m (m used only to restrict broadcasting)
      val k=(f,args); val v=if (enabled) cache.get(k) else null;
      if (v!=null) co(v.asInstanceOf[R]) // cached
      else {
        val k=getId(f,args); val cs=cont.get(k); val c=co.asInstanceOf[Any=>Unit];
        cont.put(k,c::(if (cs!=null) cs else Nil))
        if (cs==null) { // new request, create all structures and broadcast request to workers owning map m
          params.put(k,(f,args))
          val os = owns.get(m)
          sum.put(k,(os.length,zero,plus))
          os.foreach{ w=> w!Aggr(k,f,args) } // send
        }
      }
    }
    @inline def res[R](id:Int,res:R) { // onReceive AggrPart(id,res)
      val s = sum.get(id)
      val r = (s._1-1,s._3(s._2,res.asInstanceOf[Any]),s._3)
      if (r._1>0) sum.put(id,r) // incomplete aggregation, wait...
      else {
        if (enabled) cache.put(params.get(id),r._2); cont.remove(id).foreach { _(r._2) }
        sum.remove(id); params.remove(id) // cleanup other entries
        if (self!=master) bar.ack // ack pending barrier if needed
      }
    }
    @inline def ready = cont.size==0
    @inline def clear(en:Boolean=true) = { cache.clear; enabled=en }
  }

  // ---- barrier and counters management
  protected object bar {
    private var bco: Unit=>Unit = null
    private var enabled = false
    private val count = new java.util.HashMap[ActorRef,Long]() // destination, count (-1 for sent, 1 for recv)
    @inline private def add(a:ActorRef,v:Long) { if (count.containsKey(a)) { val n=count.get(a)+v; if (n==0) count.remove(a) else count.put(a,n) } else if (v!=0) count.put(a,v) }
    @inline def ack = if (enabled && matcherGet.ready && matcherAggr.ready && count.size>0) {
      var ds : List[NodeRef] = Nil
      var cs : List[Long] = Nil
      count.foreach { case (a,n) => ds=NodeRef(workers.indexOf(a))::ds; cs=n::cs }
      master ! Ack(ds.toArray,cs.toArray); count.clear
    }
    @inline def sumAck(to:Array[NodeRef],num:Array[Long]) {
      (to.map(workers(_)) zip num).foreach { case (w,n) => add(w,n) }
      if (enabled && count.size==0) { set(false); if (bco!=null) { val co=bco; bco=null; co(); } }
    }
    def set(en:Boolean,co:(Unit=>Unit)=null) {
      if (enabled==en) return; enabled=en; matcherGet.clear(!en); matcherAggr.clear(!en)
      if (self!=master) { if (en) add(self,1); ack }
      else {
        if (en && co!=null) bco=co;
        workers.foreach { w => if (w!=master) { if (en) add(w,-1); w ! Barrier(en) } } // count barrier msg
      }
    }
    // local counters usage
    @inline def recv = { add(self,1); ack }
    @inline def send(to:ActorRef,msg:Any) { add(to,-1); to ! msg }
  }

  // ---- message passing
  protected val fun_collect = FunRef(0,true)
  protected var local_map:Array[M3Map[Any,Any]] = null 
  def receive = {
    case Members(m,ws) => members(m,ws); local_map=local.asInstanceOf[Array[M3Map[Any,Any]]] // fix initialization order
    case Barrier(en) => bar.set(en) // assert(self!=master)
    case Ack(to,num) => bar.sumAck(to,num) // assert(self==master)
    case Get(m,k) => sender ! Val(m,k,local_map(m).get(k)) // get(var) is handled locally
    case Val(m,k,v) => matcherGet.res(m,k,v)
    case Add(m,k,v) => if (k==null) local_wr(m,v,true) else local_map(m).add(k,v); bar.recv
    case Set(m,k,v) => if (k==null) local_wr(m,v,false) else local_map(m).set(k,v); bar.recv
    case Clear(m,p,pk) => val mm=local(m); (if (p<0) mm else mm.slice(p,pk)).clear; bar.recv
    case Foreach(f,as) => forl(f,as,_ => bar.recv)
    case Aggr(id,fun_collect,Array(m:MapRef)) => sender ! AggrPart(id,local(m)) // collect map data
    case Aggr(id,f,as) => aggl(f,as,(r:Any) => sender ! AggrPart(id,r))
    case AggrPart(id,res) => matcherAggr.res(id,res)
    case m => println("Not understood: "+m.toString)
  }

  // ---- map operations
  // Element operation: if key hashes to local, apply locally, otherwise call remote worker
  def get[K,V](m:MapRef,k:K,co:V=>Unit) = matcherGet.req(m,k,co)
  def add[K,V](m:MapRef,k:K,v:V) {
    if (k==null) { local_wr(m,v,true); workers.foreach(w => if (w!=self) bar.send(w,Add(m,k,v))) }
    else { val o=owner(m,k); if (o==self) local(m).asInstanceOf[M3Map[K,V]].add(k,v) else bar.send(o,Add(m,k,v)) }
  }
  def set[K,V](m:MapRef,k:K,v:V) {
    if (k==null) { local_wr(m,v,false); workers.foreach(w => if (w!=self) bar.send(w,Set(m,k,v))) }
    else { val o=owner(m,k); if (o==self) local(m).asInstanceOf[M3Map[K,V]].set(k,v) else bar.send(o,Set(m,k,v)) }
  }
  // Group operations: broadcast to owners, workers then process locally
  def clear[P](m:MapRef,p:Int= -1,pk:P=null) = owns(m).foreach { bar.send(_,Clear(m,p,pk)) }
  def foreach(m:MapRef,f:FunRef,args:Any*) { owns(m).foreach { bar.send(_,Foreach(f,args.toArray)) } }
  
  // XXX: idea = create multiple functions instead of only 1
  def aggr[R](m:MapRef,f:FunRef,args:Array[Any],zero:Any=null,co:R=>Unit)(implicit cR:ClassTag[R]) = {
    val (z,p) = if (zero!=null) (zero,((m1:M3Map[Any,Any],m2:M3Map[Any,Any])=>{m2.sum(m1); m1}).asInstanceOf[(Any,Any)=>Any])
    else (M3Map.zero[R](),(cR.toString match {
      case "Long" => (v0:Long,v1:Long)=>v0+v1
      case "Double" => (v0:Double,v1:Double)=>v0+v1
      case "java.lang.String" => (v0:String,v1:String)=>v0+v1
      case "java.util.Date" => (v0:java.util.Date,v1:java.util.Date)=> new java.util.Date(v0.getTime+v1.getTime)
      case n => sys.error("No additivity for "+n)
    }).asInstanceOf[(Any,Any)=>Any])
    matcherAggr.req(m,f,args,co,z,p)
  }

  // ---- debugging
  def msg(m:Any) { val s=self.path.toString; val p0=s.indexOf("System"); val p1=s.indexOf("/user/");
    println((if(p0>=0) s.substring(p0+6,p1) else "")+s.substring(p1+5)+": "+m.toString);
  }
}

/**
 * Master is an additional role for a worker: it acts as streams entry point. It
 * processes external events sequentially, keeps track of 'invalid' maps and enforce
 * writing in-flight messages with a barrier when necessary. To provide a blocking
 * execution, we use a continuation that fetch next events when finished.
 * To write imperative-style in a CPS environment, use the continuations plug-in:
 * http://www.scala-lang.org/api/current/index.html#scala.util.continuations.package
 */
trait MasterActor extends WorkerActor {
  import scala.collection.JavaConversions.mapAsScalaMap
  import WorkerActor._
  import Messages._
  import scala.util.continuations._

  def barrier(co:Unit=>Unit) = bar.set(true,co);
  def toMap[K,V](m:MapRef,co:Map[K,V]=>Unit)(implicit cV:ClassTag[V]) = {
    val zero = if (cV.toString=="Object"||cV.toString=="Any") local(m).asInstanceOf[M3MapBase[K,V]].acc else M3Map.temp[K,V]()
    super.aggr(m,fun_collect,Array(m),zero,(mm:M3Map[K,V])=>co(mm.toMap))
  }

  // --- sequential to CPS conversion
  // XXX: to go away as it is more complicated to generate
  def _get[K,V](m:MapRef,k:K):V @cps[Unit] = shift { co:(V=>Unit) => super.get(m,k,co) }
  def _aggr[R:ClassTag](m:MapRef,f:FunRef,args:Any*) = shift { co:(R=>Unit) => super.aggr(m,f,args.toArray,null,co) }
  def _barrier = shift { co:(Unit=>Unit) => bar.set(true,co); }
  def _toMap[K,V:ClassTag](m:MapRef):Map[K,V] @cps[Unit] = shift { co:(Map[K,V]=>Unit) => toMap(m,co) }
  def _pre(write:MapRef,read:MapRef*) = shift { co:(Unit=>Unit) => pre(write,read.toArray,co) }

  // ---- coherency mechanism: if a map used is not flushed, flush all
  private val invalid = scala.collection.mutable.Set[MapRef]()
  def pre(write:MapRef,read:Array[MapRef],co:Unit=>Unit) = {
    if (read.filter{r=>invalid.contains(r)}.isEmpty) { invalid+=write; co() }
    else { invalid.clear; invalid+=write; bar.set(true,co) }
  }

  // ---- handle stream events
  private var t0:Long = 0 // startup time
  private val eq = new java.util.LinkedList[(StreamEvent,ActorRef)]() // external event queue
  private var est = 0 // state: 0=no loop, 1=loop pending, 2=trampoline, 3=bounce
  protected def deq {
    if (est==2) est=3; // bounce
    else do {
      if (eq.size==0) est=0 // loop exits
      else {
        est=2 // expose trampoline
        val (ev,sender)=eq.removeFirst
        ev match {
          case SystemInit => reset { onSystemReady(); println("Pre"); _barrier; println("Post"); t0=System.nanoTime(); deq }
          case EndOfStream | GetSnapshot(_) => val time=System.nanoTime()-t0
            def collect(n:Int,acc:List[Any]):Unit = n match {
              case 0 => sender ! (time,acc); deq
              case n => val r=MapRef(n-1)
                if (local(r)==null) collect(n-1,local_rd(r)::acc)
                else toMap(r,(m:Map[_,_])=>collect(n-1,m::acc))
            }
            collect(local.length,Nil)
          case e:TupleEvent => dispatch(e)
        }
        if (est==2) est=1 // disable trampoline
      }
    } while(est==3) // trampoline
  }

  val dispatch:PartialFunction[TupleEvent,Unit] // to be implemented by subclasses
  override def receive = masterRecv orElse super.receive
  private val masterRecv : PartialFunction[Any,Unit] = {
    case ev:StreamEvent => val p=(est==0); est=1; eq.add((ev,if (ev.isInstanceOf[TupleEvent]) null else sender)); if (p) deq;
  }
  
  def onSystemReady() // {}
}
