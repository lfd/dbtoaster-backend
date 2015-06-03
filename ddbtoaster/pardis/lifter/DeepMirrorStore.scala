/* Generated by Purgatory 2014-2015 */

package ddbt.lib.store.deep

import ch.epfl.data.sc.pardis
import ddbt.lib.store.Entry
import pardis.ir._
import pardis.types.PardisTypeImplicits._
import pardis.effects._
import pardis.deep._
import pardis.deep.scalalib._
import pardis.deep.scalalib.collection._
import pardis.deep.scalalib.io._
trait MStoreOps extends Base with ArrayOps {  
  // Type representation
  val MStoreType = MStoreIRs.MStoreType
  type MStoreType[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreType[E]
  implicit def typeMStore[E <: ddbt.lib.store.Entry: TypeRep]: TypeRep[MStore[E]] = MStoreType(implicitly[TypeRep[E]])
  implicit class MStoreRep[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) {
     def unsafeInsert(idx : Rep[Int], e : Rep[E]) : Rep[Unit] = mStoreUnsafeInsert[E](self, idx, e)(typeE)
     def insert(e : Rep[E]) : Rep[Unit] = mStoreInsert[E](self, e)(typeE)
     def update(e : Rep[E]) : Rep[Unit] = mStoreUpdate[E](self, e)(typeE)
     def delete(e : Rep[E])(implicit overload1 : Overloaded1) : Rep[Unit] = mStoreDelete1[E](self, e)(typeE)
     def get(idx : Rep[Int], key : Rep[E]) : Rep[E] = mStoreGet[E](self, idx, key)(typeE)
     def foreach(f : Rep[(E => Unit)]) : Rep[Unit] = mStoreForeach[E](self, f)(typeE)
     def slice(idx : Rep[Int], key : Rep[E], f : Rep[(E => Unit)]) : Rep[Unit] = mStoreSlice[E](self, idx, key, f)(typeE)
     def range(idx : Rep[Int], min : Rep[E], max : Rep[E], withMin : Rep[Boolean], withMax : Rep[Boolean], f : Rep[(E => Unit)]) : Rep[Unit] = mStoreRange[E](self, idx, min, max, withMin, withMax, f)(typeE)
     def delete(idx : Rep[Int], key : Rep[E])(implicit overload2 : Overloaded2) : Rep[Unit] = mStoreDelete2[E](self, idx, key)(typeE)
     def n : Rep[Int] = mStore_Field_N[E](self)(typeE)
     def ops : Rep[Array[E]] = mStore_Field_Ops[E](self)(typeE)
     def idxs : Rep[Array[E]] = mStore_Field_Idxs[E](self)(typeE)
  }
  object MStore {

  }
  // constructors
   def __newMStore[E <: ddbt.lib.store.Entry](idxs : Rep[Array[E]], ops : Rep[Array[E]])(implicit overload1 : Overloaded1, typeE : TypeRep[E]) : Rep[MStore[E]] = mStoreNew1[E](idxs, ops)(typeE)
   def __newMStore[E <: ddbt.lib.store.Entry]()(implicit cE : Manifest[E], overload2 : Overloaded2, typeE : TypeRep[E]) : Rep[MStore[E]] = mStoreNew2[E]()(typeE, cE)
  // IR defs
  val MStoreNew1 = MStoreIRs.MStoreNew1
  type MStoreNew1[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreNew1[E]
  val MStoreNew2 = MStoreIRs.MStoreNew2
  type MStoreNew2[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreNew2[E]
  val MStoreUnsafeInsert = MStoreIRs.MStoreUnsafeInsert
  type MStoreUnsafeInsert[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreUnsafeInsert[E]
  val MStoreInsert = MStoreIRs.MStoreInsert
  type MStoreInsert[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreInsert[E]
  val MStoreUpdate = MStoreIRs.MStoreUpdate
  type MStoreUpdate[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreUpdate[E]
  val MStoreDelete1 = MStoreIRs.MStoreDelete1
  type MStoreDelete1[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreDelete1[E]
  val MStoreGet = MStoreIRs.MStoreGet
  type MStoreGet[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreGet[E]
  val MStoreForeach = MStoreIRs.MStoreForeach
  type MStoreForeach[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreForeach[E]
  val MStoreSlice = MStoreIRs.MStoreSlice
  type MStoreSlice[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreSlice[E]
  val MStoreRange = MStoreIRs.MStoreRange
  type MStoreRange[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreRange[E]
  val MStoreDelete2 = MStoreIRs.MStoreDelete2
  type MStoreDelete2[E <: ddbt.lib.store.Entry] = MStoreIRs.MStoreDelete2[E]
  val MStore_Field_N = MStoreIRs.MStore_Field_N
  type MStore_Field_N[E <: ddbt.lib.store.Entry] = MStoreIRs.MStore_Field_N[E]
  val MStore_Field_Ops = MStoreIRs.MStore_Field_Ops
  type MStore_Field_Ops[E <: ddbt.lib.store.Entry] = MStoreIRs.MStore_Field_Ops[E]
  val MStore_Field_Idxs = MStoreIRs.MStore_Field_Idxs
  type MStore_Field_Idxs[E <: ddbt.lib.store.Entry] = MStoreIRs.MStore_Field_Idxs[E]
  // method definitions
   def mStoreNew1[E <: ddbt.lib.store.Entry](idxs : Rep[Array[E]], ops : Rep[Array[E]])(implicit typeE : TypeRep[E]) : Rep[MStore[E]] = MStoreNew1[E](idxs, ops)
   def mStoreNew2[E <: ddbt.lib.store.Entry]()(implicit typeE : TypeRep[E], cE : Manifest[E]) : Rep[MStore[E]] = MStoreNew2[E]()
   def mStoreUnsafeInsert[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreUnsafeInsert[E](self, idx, e)
   def mStoreInsert[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreInsert[E](self, e)
   def mStoreUpdate[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreUpdate[E](self, e)
   def mStoreDelete1[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreDelete1[E](self, e)
   def mStoreGet[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E])(implicit typeE : TypeRep[E]) : Rep[E] = MStoreGet[E](self, idx, key)
   def mStoreForeach[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], f : Rep[((E) => Unit)])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreForeach[E](self, f)
   def mStoreSlice[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E], f : Rep[((E) => Unit)])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreSlice[E](self, idx, key, f)
   def mStoreRange[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], min : Rep[E], max : Rep[E], withMin : Rep[Boolean], withMax : Rep[Boolean], f : Rep[((E) => Unit)])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreRange[E](self, idx, min, max, withMin, withMax, f)
   def mStoreDelete2[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = MStoreDelete2[E](self, idx, key)
   def mStore_Field_N[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) : Rep[Int] = MStore_Field_N[E](self)
   def mStore_Field_Ops[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) : Rep[Array[E]] = MStore_Field_Ops[E](self)
   def mStore_Field_Idxs[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) : Rep[Array[E]] = MStore_Field_Idxs[E](self)
  type MStore[E <: ddbt.lib.store.Entry] = ddbt.lib.store.MStore[E]
}
object MStoreIRs extends Base {
  import ArrayIRs._
  // Type representation
  case class MStoreType[E <: ddbt.lib.store.Entry](typeE: TypeRep[E]) extends TypeRep[MStore[E]] {
    def rebuild(newArguments: TypeRep[_]*): TypeRep[_] = MStoreType(newArguments(0).asInstanceOf[TypeRep[_ <: ddbt.lib.store.Entry]])
    private implicit val tagE = typeE.typeTag
    val name = s"MStore[${typeE.name}]"
    val typeArguments = List(typeE)
    
    val typeTag = scala.reflect.runtime.universe.typeTag[MStore[E]]
  }
      implicit def typeMStore[E <: ddbt.lib.store.Entry: TypeRep]: TypeRep[MStore[E]] = MStoreType(implicitly[TypeRep[E]])
  // case classes
  case class MStoreNew1[E <: ddbt.lib.store.Entry](idxs : Rep[Array[E]], ops : Rep[Array[E]])(implicit val typeE : TypeRep[E]) extends ConstructorDef[MStore[E]](List(typeE), "MStore", List(List(idxs,ops))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreNew2[E <: ddbt.lib.store.Entry]()(implicit val typeE : TypeRep[E], val cE : Manifest[E]) extends ConstructorDef[MStore[E]](List(typeE), "MStore", List(List())){
    override def curriedConstructor = (x: Any) => copy[E]()
  }

  case class MStoreUnsafeInsert[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], e : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "unsafeInsert", List(List(idx,e))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreInsert[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "insert", List(List(e))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreUpdate[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "update", List(List(e))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreDelete1[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "delete", List(List(e))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreGet[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[E](Some(self), "get", List(List(idx,key))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreForeach[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], f : Rep[((E) => Unit)])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "foreach", List(List(f))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreSlice[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E], f : Rep[((E) => Unit)])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "slice", List(List(idx,key,f))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreRange[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], min : Rep[E], max : Rep[E], withMin : Rep[Boolean], withMax : Rep[Boolean], f : Rep[((E) => Unit)])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "range", List(List(idx,min,max,withMin,withMax,f))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStoreDelete2[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[Unit](Some(self), "delete", List(List(idx,key))){
    override def curriedConstructor = (copy[E] _).curried
  }

  case class MStore_Field_N[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit val typeE : TypeRep[E]) extends FieldDef[Int](self, "n"){
    override def curriedConstructor = (copy[E] _)
    override def isPure = true

  }

  case class MStore_Field_Ops[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit val typeE : TypeRep[E]) extends FieldDef[Array[E]](self, "ops"){
    override def curriedConstructor = (copy[E] _)
    override def isPure = true

    override def partialEvaluate(children: Any*): Array[E] = {
      val self = children(0).asInstanceOf[MStore[E]]
      self.ops
    }
    override def partialEvaluable: Boolean = true

  }

  case class MStore_Field_Idxs[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit val typeE : TypeRep[E]) extends FieldDef[Array[E]](self, "idxs"){
    override def curriedConstructor = (copy[E] _)
    override def isPure = true

    override def partialEvaluate(children: Any*): Array[E] = {
      val self = children(0).asInstanceOf[MStore[E]]
      self.idxs
    }
    override def partialEvaluable: Boolean = true

  }

  type MStore[E <: ddbt.lib.store.Entry] = ddbt.lib.store.MStore[E]
}
trait MStoreImplicits extends MStoreOps { 
  // Add implicit conversions here!
}
trait MStorePartialEvaluation extends MStoreComponent with BasePartialEvaluation {  
  // Immutable field inlining 
  override def mStore_Field_Ops[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) : Rep[Array[E]] = self match {
    case Def(node: MStoreNew1[_]) => node.ops
    case _ => super.mStore_Field_Ops[E](self)(typeE)
  }
  override def mStore_Field_Idxs[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) : Rep[Array[E]] = self match {
    case Def(node: MStoreNew1[_]) => node.idxs
    case _ => super.mStore_Field_Idxs[E](self)(typeE)
  }

  // Mutable field inlining 
  // Pure function partial evaluation
}
trait MStoreComponent extends MStoreOps with MStoreImplicits {  }