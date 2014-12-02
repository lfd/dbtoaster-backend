/* Generated by Purgatory 2014 */

package ddbt.lib.store.deep

import ch.epfl.data.pardis
import pardis.ir._
import pardis.types.PardisTypeImplicits._
import pardis.effects._
import pardis.deep.scalalib._
import ddbt.lib.store._

trait MStoreOps extends Base with ArrayOps {  
  // Type representation
  case class MStoreType[E <: ddbt.lib.store.Entry](typeE: TypeRep[E]) extends TypeRep[MStore[E]] {
    def rebuild(newArguments: TypeRep[_]*): TypeRep[_] = MStoreType(newArguments(0).asInstanceOf[TypeRep[_ <: ddbt.lib.store.Entry]])
    private implicit val tagE = typeE.typeTag
    val name = s"MStore[${typeE.name}]"
    val typeArguments = List(typeE)
    
    val typeTag = scala.reflect.runtime.universe.typeTag[MStore[E]]
  }
      implicit def typeMStore[E <: ddbt.lib.store.Entry: TypeRep] = MStoreType(implicitly[TypeRep[E]])
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
   def __newMStore[E <: ddbt.lib.store.Entry](idxs : Rep[Array[E]], ops : Rep[Array[E]])(implicit typeE : TypeRep[E]) : Rep[MStore[E]] = mStoreNew[E](idxs, ops)(typeE)
  // case classes
  case class MStoreNew[E <: ddbt.lib.store.Entry](idxs : Rep[Array[E]], ops : Rep[Array[E]])(implicit val typeE : TypeRep[E]) extends ConstructorDef[MStore[E]](List(typeE), "MStore", List(List(idxs,ops))){
    override def curriedConstructor = (copy[E] _).curried
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

  // method definitions
   def mStoreNew[E <: ddbt.lib.store.Entry](idxs : Rep[Array[E]], ops : Rep[Array[E]])(implicit typeE : TypeRep[E]) : Rep[MStore[E]] = MStoreNew[E](idxs, ops)
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
trait MStoreImplicits extends MStoreOps { 
  // Add implicit conversions here!
}
trait MStoreImplementations extends MStoreOps { 
    override def mStoreUnsafeInsert[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
    override def mStoreInsert[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
    override def mStoreUpdate[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
    override def mStoreDelete1[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
    override def mStoreGet[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E])(implicit typeE : TypeRep[E]) : Rep[E] = {
???
    }
    override def mStoreForeach[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], f : Rep[((E) => Unit)])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
    override def mStoreSlice[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E], f : Rep[((E) => Unit)])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
    override def mStoreRange[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], min : Rep[E], max : Rep[E], withMin : Rep[Boolean], withMax : Rep[Boolean], f : Rep[((E) => Unit)])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
    override def mStoreDelete2[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]], idx : Rep[Int], key : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Unit] = {
???
    }
}

trait MStorePartialEvaluation extends MStoreComponent with BasePartialEvaluation {  
  // Immutable field inlining 
  override def mStore_Field_Ops[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) : Rep[Array[E]] = self match {
    case Def(node: MStoreNew[_]) => node.ops
    case _ => super.mStore_Field_Ops[E](self)(typeE)
  }
  override def mStore_Field_Idxs[E <: ddbt.lib.store.Entry](self : Rep[MStore[E]])(implicit typeE : TypeRep[E]) : Rep[Array[E]] = self match {
    case Def(node: MStoreNew[_]) => node.idxs
    case _ => super.mStore_Field_Idxs[E](self)(typeE)
  }

  // Mutable field inlining 
  // Pure function partial evaluation
}
trait MStoreComponent extends MStoreOps with MStoreImplicits {  }
