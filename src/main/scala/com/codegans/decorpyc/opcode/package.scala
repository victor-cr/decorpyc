package com.codegans.decorpyc

package object opcode {
  trait OpcodeInstruction

  case class OpcodeRoot(attributes: Map[String, _], children: List[_]) extends OpcodeInstruction

  case object OpcodeMarkObject extends OpcodeInstruction

  case class GlobalFunction(moduleName: String, functionName: String) extends OpcodeInstruction

  case class NewInstance(classRef: Any, args: List[Any]) extends OpcodeInstruction

  case class Invocation(objRef: Any, args: List[Any]) extends OpcodeInstruction

  case class MapInstanceWithData(instance: Any, data: Map[_, _]) extends OpcodeInstruction

  case class SetState(instance: Any, state: List[_]) extends OpcodeInstruction

  case class SetUpdate(instance: Any, data: Map[_, _]) extends OpcodeInstruction

  case class SetMapItem(instance: Any, key: Any, value: Any) extends OpcodeInstruction

}
