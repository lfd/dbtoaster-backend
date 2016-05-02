package ddbt.transformer


import ch.epfl.data.sc.pardis.ir.{PardisStructArg, PardisStruct}
import ch.epfl.data.sc.pardis.optimization.Rule.Statement
import ch.epfl.data.sc.pardis.optimization.{Optimizer, RuleBasedTransformer}
import ddbt.lib.store.deep.StoreDSL

class CodeMotion(override val IR: StoreDSL)  extends Optimizer[StoreDSL](IR) {
  import IR._

  val effectAnalysis = new SideEffectsAnalysis(IR)

  def optimize[T: TypeRep](node: Block[T]): Block[T] = {
    effectAnalysis.optimize(node)
    node
  }

  /**
   * First, in the analysis phase, it collects the statements constructing a HashMap
   * or MultiMap. Furthermore, it looks for the dependent statements, since hoisting
   * statements without hoisting the dependent statements makes the program incorrect.
   * Second, all hoisted statements are scheduled in the right order.
   * Finally, the scheduled statements are moved to the loading part and are removed
   * from the query processing time.
   */
  // def optimize[T: TypeRep](node: Block[T]): to.Block[T] = {
  //   do {
  //     newStatementHoisted = false
  //     traverseBlock(node)
  //   } while (newStatementHoisted)
  //   scheduleHoistedStatements()
  //   transformProgram(node)
  // }

  // var startCollecting = false
  // var newStatementHoisted = false
  // /**
  //  * Specifies the nesting level of the statements that we are traversing over.
  //  */
  // var depthLevel = 0
  // val hoistedStatements = collection.mutable.ArrayBuffer[Stm[Any]]()
  // // TODO check if we can remove this one?
  // val currentHoistedStatements = collection.mutable.ArrayBuffer[Stm[Any]]()
  // /**
  //  * Contains the list of symbols that we should find their dependency in the next
  //  * analysis iteration.
  //  */
  // val workList = collection.mutable.Set[Sym[Any]]()

  // /**
  //  * Schedules the statments that should be hoisted.
  //  */
  // def scheduleHoistedStatements() {
  //   val result = Graph.schedule(hoistedStatements.toList, (stm1: Stm[Any], stm2: Stm[Any]) =>
  //     getDependencies(stm2.rhs).contains(stm1.sym))
  //   hoistedStatements.clear()
  //   hoistedStatements ++= result
  // }

  // /**
  //  * Returns the symbols that the given definition is dependent on them.
  //  */
  // def getDependencies(node: Def[_]): List[Sym[Any]] =
  //   node.funArgs.filter(_.isInstanceOf[Sym[Any]]).map(_.asInstanceOf[Sym[Any]])

  // override def traverseDef(node: Def[_]): Unit = node match {
  //   case GenericEngineRunQueryObject(b) => {
  //     startCollecting = true
  //     depthLevel = 0
  //     currentHoistedStatements.clear()
  //     traverseBlock(b)
  //     hoistedStatements.prependAll(currentHoistedStatements)
  //     startCollecting = false
  //   }
  //   case _ => super.traverseDef(node)
  // }

  // override def traverseBlock(block: Block[_]): Unit = {
  //   depthLevel += 1
  //   super.traverseBlock(block)
  //   depthLevel -= 1
  // }

  // def isDependentOnMutation(exp: Rep[_]): Boolean = exp match {
  //   case Def(node) => node match {
  //     case ReadVar(_) => true
  //     case _ => node.funArgs.collect({
  //       case e: Rep[_] => isDependentOnMutation(e)
  //     }).exists(identity)
  //   }
  //   case _ => false
  // }

  // /**
  //  * Gathers the statements that should be hoisted.
  //  */
  // override def traverseStm(stm: Stm[_]): Unit = stm match {
  //   case Stm(sym, rhs) => {
  //     def hoistStatement() {
  //       currentHoistedStatements += stm.asInstanceOf[Stm[Any]]
  //       workList ++= getDependencies(rhs)
  //       newStatementHoisted = true
  //     }
  //     rhs match {
  //       case HashMapNew() if startCollecting && !hoistedStatements.contains(stm) => {
  //         hoistStatement()
  //       }
  //       case MultiMapNew() if startCollecting && !hoistedStatements.contains(stm) => {
  //         hoistStatement()
  //       }
  //       case ArrayNew(size) if isDependentOnMutation(size) =>
  //         super.traverseStm(stm)
  //       case ArrayNew(_) if startCollecting && depthLevel == 1 && !hoistedStatements.contains(stm) => {
  //         hoistStatement()
  //       }
  //       case _ if startCollecting && workList.contains(sym) && !hoistedStatements.contains(stm) => {
  //         hoistStatement()
  //         workList -= sym
  //       }
  //       case _ => super.traverseStm(stm)
  //     }
  //   }
  // }

  // /**
  //  * Removes the hoisted statements from the query processing time.
  //  */
  // override def transformStmToMultiple(stm: Stm[_]): List[to.Stm[_]] =
  //   if (hoistedStatements.contains(stm.asInstanceOf[Stm[Any]]))
  //     Nil
  //   else
  //     super.transformStmToMultiple(stm)

  // /**
  //  * Reifies the hoisted statements in the loading time.
  //  */
  // override def transformDef[T: PardisType](node: Def[T]): to.Def[T] = (node match {
  //   case GenericEngineRunQueryObject(b) =>
  //     for (stm <- hoistedStatements) {
  //       reflectStm(stm)
  //     }
  //     val newBlock = transformBlock(b)
  //     GenericEngineRunQueryObject(newBlock)(newBlock.tp)
  //   case _ => super.transformDef(node)
  // }).asInstanceOf[to.Def[T]]
}

class SideEffectsAnalysis(override val IR: StoreDSL) extends RuleBasedTransformer[StoreDSL](IR) {
  import IR._

  var currentContext: Block[_] = _

  sealed trait EffectInfo {
    // def meet(o: EffectInfo): EffectInfo
    // def join(o: EffectInfo): EffectInfo
    def isPure: Boolean = this == Pure
  }
  case object Pure extends EffectInfo 
  case object Effect extends EffectInfo

  val symEffectsInfo = scala.collection.mutable.Map[Rep[_], EffectInfo]()
  val blockEffectsInfo = scala.collection.mutable.Map[Block[_], EffectInfo]()

  def getEffectOfStm(stm: Stm[_]): EffectInfo = {
    symEffectsInfo.getOrElseUpdate(stm.sym, if(stm.rhs.isPure) Pure else Effect)
  }

  def getEffectOfBlock(block: Block[_]): EffectInfo = {
    blockEffectsInfo.getOrElseUpdate(block, if(block.stmts.map(getEffectOfStm).forall(_ == Pure)) Pure else Effect)
  }

  override def postAnalyseProgram[T: TypeRep](node: Block[T]): Unit = {
    // System.err.println(s"Done with effect analysis!")
    // blockEffectsInfo.foreach(x => System.err.println(s"block ${x._1.res}: ${x._2}"))
  }


  analysis += statement {
    case sym -> IfThenElse(cond, thenp, elsep) => {
      traverseBlock(thenp)
      traverseBlock(elsep)
      val eff = if (getEffectOfBlock(thenp) == Pure && getEffectOfBlock(elsep) == Pure)
        Pure
      else
        Effect
      symEffectsInfo += sym -> eff
      ()
    }
  }

  analysis += rule {
    case block: Block[_] => {
      val oldContext = currentContext
      currentContext = block
      traverseBlock(block)
      // blockEffectsInfo(block) = if(block.stmts.map(getEffectOfStm).forall(_ == Pure)) Pure else Effect
      getEffectOfBlock(block)
      currentContext = oldContext
    }
  }
}
