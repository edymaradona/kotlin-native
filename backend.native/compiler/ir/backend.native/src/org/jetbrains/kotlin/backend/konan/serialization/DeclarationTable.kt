package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.descriptors.isTopLevelDeclaration
import org.jetbrains.kotlin.backend.konan.irasdescriptors.fqNameSafe
import org.jetbrains.kotlin.backend.konan.irasdescriptors.name
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


internal val IrDeclarationParent.fqNameUnique: FqName
    get() = when(this) {
        is IrPackageFragment -> this.fqName
        is IrDeclaration -> this.parent.fqNameUnique.child(this.uniqName)
        else -> error(this)
    }

val IrDeclaration.uniqName: Name
    get() = when (this) {
        is IrSimpleFunction -> Name.special("<${this.uniqFunctionName}>")
        else -> this.name
    }

internal fun IrDeclaration.symbolName(): String = when (this) {
    is IrFunction
    -> {
        //println("### this.uniqFunctionName = ${this.uniqFunctionName}")
        this.uniqFunctionName
    }
    is IrProperty
    -> this.symbolName
    is IrClass
    -> this.typeInfoSymbolName
    is IrField
    -> this.symbolName
    is IrEnumEntry
    -> this.symbolName
    else -> error("Unexpected exported declaration: $this")
}

internal val IrDeclaration.uniqId: Long
    get() = this.symbolName().localHash.value

fun <K, V> MutableMap<K, V>.putOnce(k:K, v: V): Unit {
    // TODO: there are
    // kotlinx.cinterop.ObjCClassOf<T>.create(format: kotlin.String): T defined in platform.Foundation in file Foundation.kt
    // and
    // kotlinx.cinterop.ObjCClassOf<T>.create(string: kotlin.String): T defined in platform.Foundation in file Foundation.kt
    // and other clashes
    //if (v is IrSimpleFunction && this.containsKey(k) && this[k] != v) {
    //    println("a clash:")
    //    println("${v.name} in ${v.parent}")
    //}
    //if (this.containsKey(k) && v is IrSimpleFunction &&  v.parent is IrFile && (v.parent as IrFile).fileEntry.name.endsWith("Foundation.kt")) return
    assert(!this.containsKey(k) || this[k] == v) {
        println("adding $v for $k, but it is already ${this[k]} for $k")
    }
    this.put(k, v)
}

class DescriptorTable {
    val descriptors = mutableMapOf<DeclarationDescriptor, Long>()

    // See comment for serializeDescriptorReference() for more details.
    fun descriptorIndex(descriptor: DeclarationDescriptor, uniqId: UniqId) {
        descriptors.putOnce(descriptor, uniqId.index)
    }
}

data class UniqId (
    val index: Long,
    val isLocal: Boolean
)

data class UniqIdKey private constructor(val uniqId: UniqId, val moduleDescriptor: ModuleDescriptor?) {
    constructor(moduleDescriptor: ModuleDescriptor?, uniqId: UniqId)
            : this(uniqId, if (uniqId.isLocal) moduleDescriptor else null)
}

// TODO: We don't manage id clashes anyhow now.
class DeclarationTable(val builtIns: IrBuiltIns, val descriptorTable: DescriptorTable) {

    val table = mutableMapOf<IrDeclaration, UniqId>()
    val reverse = mutableMapOf<UniqId, IrDeclaration>() // TODO: remove me. Only needed during the development.
    val textual = mutableMapOf<UniqId, String>()
    val descriptors = descriptorTable
    var currentIndex = 0L

    init {
        builtIns.knownBuiltins.forEach {
            table.put(it, UniqId(currentIndex ++, false))
        }
    }

    fun indexByValue(value: IrDeclaration): UniqId {
        val index = table.getOrPut(value) {

            if (value.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
                !value.isExported()
                    || value is IrVariable
                    || value is IrTypeParameter
                    || value is IrValueParameter
                    || value is IrAnonymousInitializerImpl
            ) {
                UniqId(currentIndex++, true)
            } else {
                UniqId(value.uniqId, false)
            }
        }
        reverse.putOnce(index, value)

        textual.put(index, "${if (index.isLocal) "" else value.symbolName()} descriptor = ${value.descriptor}")

        return index
    }
}

val IrBuiltIns.knownBuiltins: List<IrSimpleFunction> // TODO: why do we have this list??? We need the complete list!
    get() = (lessFunByOperandType.values +
            lessOrEqualFunByOperandType.values +
            greaterOrEqualFunByOperandType.values +
            greaterFunByOperandType.values +
            ieee754equalsFunByOperandType.values +
            eqeqeqFun + eqeqFun +
            throwNpeFun + booleanNotFun + noWhenBranchMatchedExceptionFun + enumValueOfFun +
            dataClassArrayMemberToStringFun + dataClassArrayMemberHashCodeFun)


internal val IrProperty.symbolName: String
    get() {
        val extensionReceiver: String = getter!!.extensionReceiverParameter ?. extensionReceiverNamePart ?: ""

        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kprop:$containingDeclarationPart$extensionReceiver$name"
    }

internal val IrEnumEntry.symbolName: String
    get() {
        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kenumentry:$containingDeclarationPart$name"
    }

// This is basicly the same as .symbolName, but diambiguates external functions with the same C name.
// In addition functions appearing in fq sequence appear as <full signature>.
internal val IrFunction.uniqFunctionName: String
    get() {
        val parent = this.parent

        val containingDeclarationPart = parent.fqNameUnique.let {
            if (it.isRoot) "" else "$it."
        }

        return "kfun:$containingDeclarationPart#$functionName"
    }