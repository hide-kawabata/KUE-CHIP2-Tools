/*
  KUE-CHIP2 Simulator

  Copyright (C) 2018 Hideyuki Kawabata
*/

import java.io.File
import java.io.InputStream

class Byte(n: Int) {
  var bit = Array(8, {_->0})
  var cf = 0
  var vf = 0
  var nf = 0
  var zf = 0
  init {
    num2byte(n)
  }
  fun getBit(i: Int): Int {return bit[i]}
  fun getCF(): Int {return cf}
  fun getVF(): Int {return vf}
  fun getNF(): Int {return nf}
  fun getZF(): Int {return zf}
  fun num2byte(n: Int) {
    tailrec fun f(n: Int, i: Int) {
      if (i > 7) return
      bit[i] = n.rem(2)
      f(n.div(2), i+1)
    }
    tailrec fun make_positive(n: Int): Int {
      if (n < 0) {
	return make_positive(256 + n)
      } else {
	return n
      }
    }
    f(make_positive(n), 0)
  }
  fun byte2num(): Int {
    tailrec fun f(n: Int, i: Int): Int {
      if (i > 7) return n
      return f(n*2 + bit[7-i], i+1)
    }
    return f(0, 0)
  }
  fun toStr(): String {
    var s: String = "["
    for (i: Int in 0..3) {
      s = s.plus("%1d".format(bit[7-i]))
    }
//    s = s.plus(" ")
    for (i: Int in 4..7) {
      s = s.plus("%1d".format(bit[7-i]))
    }
    s = s.plus("]")
    return s
  }
  fun print() {
    for (i: Int in 0..3) {
      print("%1d".format(bit[7-i]))
    }
//    print(" ")
    for (i: Int in 4..7) {
      print("%1d".format(bit[7-i]))
    }
    println()
  }
  fun copy(b: Byte): Byte {
    val bbit = b.getBits()
    for (i: Int in 0..7) {
      bit[i] = bbit[i]
    }
    return this
  }
  fun getBits(): Array<Int> {
    return bit
  }
  fun and(b: Byte): Byte {
    tailrec fun f(i: Int) {
      if (i > 7) return
      if (b.getBits()[i] == 0) bit[i] = 0
      f(i+1)
    }
    f(0)
    return this
  }
  fun or(b: Byte): Byte {
    tailrec fun f(i: Int) {
      if (i > 7) return
      if (b.getBits()[i] > 0) bit[i] = 1
      f(i+1)
    }
    f(0)
    return this
  }
  fun eor(b: Byte): Byte {
    tailrec fun f(i: Int) {
      if (i > 7) return
      if (b.getBits()[i] == bit[i]) {
	bit[i] = 0
      } else {
	bit[i] = 1
      }
      f(i+1)
    }
    f(0)
    return this
  }
  fun add(b: Byte): Byte {
    val tn = this.byte2num()
    val bn = b.byte2num()
    val n = tn + bn
    if (tn < 128 && bn < 128 && n >= 128) {
      vf = 1
    } else if (tn >=128 && bn >= 128 && n < 384) {
      vf = 1
    } else {
      vf = 0
    }
    if (n >= 256) {
      cf = 1
    } else {
      cf = 0
    }
    this.num2byte(n)
    return this
  }
  fun sub(b: Byte): Byte {
    val tn = this.byte2num()
    val bn = b.byte2num()
    val n = tn - bn
    if (tn < 128 && bn >= 128 && n >= 384) {
      vf = 1
    } else if (tn >= 128 && bn < 128 && n < 384) {
      vf = 1
    } else {
      vf = 0
    }
    if (tn < bn) {
      cf = 1
    } else {
      cf = 0
    }
    this.num2byte(n)
    return this
  }
  fun cmp(b: Byte): Int {
    val tn = this.byte2num()
    val bn = b.byte2num()
    return (tn - bn)
  }
  fun sra(): Byte {
    cf = bit[0]
    for (i: Int in 0..6) {
      bit[i] = bit[i+1]
    }
    vf = 0
    return this
  }
  fun sla(): Byte {
    cf = bit[7]
    for (i: Int in 0..6) {
      bit[7-i] = bit[7-i-1]
    }
    bit[0] = 0
    vf = if (cf != bit[7]) {1} else {0}
    return this
  }
  fun srl(): Byte {
    cf = bit[0]
    for (i: Int in 0..6) {
      bit[i] = bit[i+1]
    }
    bit[7] = 0
    vf = 0
    return this
  }
  fun sll(): Byte {
    cf = bit[7]
    for (i: Int in 0..6) {
      bit[7-i] = bit[7-i-1]
    }
    bit[0] = 0
    vf = 0
    return this
  }
  fun rra(cf_: Int): Byte {
    val t = bit[0]
    for (i: Int in 0..6) {
      bit[i] = bit[i+1]
    }
    bit[7] = cf_
    cf = t
    vf = 0
    return this
  }
  fun rla(cf_: Int): Byte {
    var t = bit[7]
    for (i: Int in 0..6) {
      bit[7-i] = bit[7-i-1]
    }
    bit[0] = cf_
    cf = t
    vf = if (cf == bit[7]) {0} else {1}
    return this
  }
  fun rrl(): Byte {
    cf = bit[0]
    for (i: Int in 0..6) {
      bit[i] = bit[i+1]
    }
    bit[7] = cf
    vf = 0
    return this
  }
  fun rll(): Byte {
    cf = bit[7]
    for (i: Int in 0..6) {
      bit[7-i] = bit[7-i-1]
    }
    bit[0] = cf
    vf = 0
    return this
  }
  fun inc(): Byte {
    var num = this.byte2num()
    this.num2byte(num + 1)
    return this
  }
}



class KueChip2Board() {
  var opflag = 0
  var acc = Byte(0)
  var ix = Byte(0)
  var pc = Byte(0)
  var mar = Byte(0)
  var ir = Byte(0)
  var dr = Byte(0)
  var zerof = 0
  var negf = 0
  var vf = 0
  var cf = 0
  var mem = Array(256, {_->Byte(0)})
  var dmem = Array(256, {_->Byte(0)})
//  var pmode = "decimal"
  var pmode = "hex"

  // the same as the function in kas.kt
  fun parseNumber0(s: String): Int {
//println("(parseNumber0) s=" + s)
    val ca = s.toCharArray()
    if (!s[0].isDigit()) {
      throw SymError("(in parseNumber0)" + s + ": not a number")
    } else if (s[ca.size-1] == 'h') {
      var s2 = s.trim({ch -> ch == 'h'})
      try {
	return (s2.toLong(radix = 16).toInt()) // 0AH
      } catch (e: NumberFormatException) {
	throw SymError("(in parseNumber0)" + s + ": not a number")
      }
    } else {
      try {
	return s.toInt() // 3
      } catch (e: NumberFormatException) {
	throw SymError("(in parseNumber0)" + s + ": not a number")
      }
    }

  }

  fun togglePrintMode() {
    if (pmode == "hex") pmode = "decimal" else pmode = "hex"
  }
  fun printAll() {
    printRegs()

    val v_pc = pc.byte2num()
    val s_pc = if (v_pc < 128) { 
      "(" + v_pc.toString() + ")"
    } else {
      "(" + (v_pc-256).toString() + "/" + v_pc.toString() + ")"
    }
    print("PC" + pc.toStr() + "=" + s_pc + ", ")

    val v_mar = mar.byte2num()
    val s_mar = if (v_mar < 128) { 
      "(" + v_mar.toString() + ")"
    } else {
      "(" + (v_mar-256).toString() + "/" + v_mar.toString() + ")"
    }
    print("MAR" + mar.toStr() + "=" + s_mar + ", ")    

    val v_ir = ir.byte2num()
    val s_ir = if (v_ir < 128) { 
      "(" + v_ir.toString() + ")"
    } else {
      "(" + (v_ir-256).toString() + "/" + v_ir.toString() + ")"
    }
    print("IR" + ir.toStr() + "=" + s_ir + ", ")    

    val v_dr = dr.byte2num()
    val s_dr = if (v_dr < 128) { 
      "(" + v_dr.toString() + ")"
    } else {
      "(" + (v_dr-256).toString() + "/" + v_dr.toString() + ")"
    }
    print("DR" + dr.toStr() + "=" + s_dr)

    print(", OP=" + opflag)

    println()
    printMem()
  }
  fun printRegs() {
    val v_acc = acc.byte2num()
    val s_acc = if (v_acc < 128) { 
      "(" + v_acc.toString() + ")"
    } else {
      "(" + (v_acc-256).toString() + "/" + v_acc.toString() + ")"
    }
    print("ACC" + acc.toStr() + "=" + s_acc + ", ")

    val v_ix = ix.byte2num()
    val s_ix = if (v_ix < 128) { 
      "(" + v_ix.toString() + ")"
    } else {
      "(" + (v_ix-256).toString() + "/" + v_ix.toString() + ")"
    }
    print("IX" + ix.toStr() + "=" + s_ix + ", ")

    print("CF=%d, VF=%d, NF=%d, ZF=%d".format(cf,vf,negf,zerof))
    println()
//    println()
  }
  fun printMem() {
    var fmt_entity: String
    var fmt_addr: String
    if (pmode == "hex") {
      fmt_entity = " %02X"
      fmt_addr = "%02X"
    } else {
      fmt_entity = "%4d"
      fmt_addr = "%3d"
    }
    for (addr: Int in 0..255) {
      if (addr.rem(16) == 0) {
	if (addr != 0) println()
//	print("%3d".format(addr) + " : ")
	print(fmt_addr.format(addr) + " : ")
      }
//      print("%4d".format(mem[addr].byte2num()))
      print(fmt_entity.format(mem[addr].byte2num()))
    }
    println()
//    println()
  }
  fun printDMem() {
    var fmt_entity: String
    var fmt_addr: String
//    if (pmode == "hex") {
      fmt_entity = " %02X"
      fmt_addr = "1%02X"
//    } else {
//      fmt_entity = "%4d"
//      fmt_addr = "%3d"
//    }
    for (addr: Int in 0..255) {
      if (addr.rem(16) == 0) {
	if (addr != 0) println()
	print(fmt_addr.format(addr) + " : ")
      }
      print(fmt_entity.format(dmem[addr].byte2num()))
    }
    println()
  }

  fun setMem(v: String, a: String) {
    val value = parseNumber0(v)
    val addr = parseNumber0(a)
    mem[addr].num2byte(value)
  }

  fun setDMem(v: String, a: String) {
    val value = parseNumber0(v)
    val addr = parseNumber0(a)
    dmem[addr].num2byte(value)
  }

  // "acc" -> acc
  fun name2reg(s: String): Byte {
    when (s) {
      "acc" -> return acc
      "ix" -> return ix
//      else -> return Byte(s.toInt()) // immediate value
      else -> return Byte(parseNumber0(s)) // immediate value
    }
  }

  // FLAG
  fun setFlag(b: Byte) {
    negf = b.getBit(7)
    when (b.byte2num()) {
      0 -> zerof = 1
      else -> zerof = 0
    }
  }

  // LD
  fun ld(ty: instT, opr1: String, opr2: String) {
    val r1 = name2reg(opr1)
    val r2 = name2reg(opr2)
//    val n2 = parseNumber0(opr2)
    val n2 = r2.byte2num()
//println("(LD) opr1=" + opr1 + " opr2=" + opr2 + " n2=" + n2)
    when (ty) {
      instT.OP_RR -> r1.copy(r2) // LD ACC, IX
      instT.OP_RImm -> r1.copy(r2) // LD ACC, 3
      instT.OP_RDir -> r1.copy(mem[n2]) // LD ACC, [3]
      instT.OP_RDis -> r1.copy(mem[(ix.byte2num() + n2).rem(256)]) // LD ACC, [IX+3]
      instT.OP_RDir2 -> r1.copy(dmem[n2]) // LD ACC, (3)
      instT.OP_RDis2 -> r1.copy(dmem[(ix.byte2num() + n2).rem(256)]) // LD ACC, (IX+3)
      else -> throw SymError("(in  ld)" + ty + ": illegal addressing")
    }
//    pc.inc()
  }

  // ST
  fun st(ty: instT, opr1: String, opr2: String) {
//println("(ST) opr1=" + opr1 + " opr2=" + opr2)
    val r1 = name2reg(opr1)
    val n2 = parseNumber0(opr2)
    when (ty) {
      instT.OP_RR -> {}
      instT.OP_RImm -> {}
      instT.OP_RDir -> mem[n2].copy(r1) // ST ACC, [5]
      instT.OP_RDis -> mem[(ix.byte2num() + n2).rem(256)].copy(r1) // ST ACC, [IX+3]
      instT.OP_RDir2 -> dmem[n2].copy(r1) // ST ACC, (5)
      instT.OP_RDis2 -> dmem[(ix.byte2num() + n2).rem(256)].copy(r1) // ST ACC, (IX+3)
      else -> throw SymError("(in  ld)" + ty + ": illegal addressing")
    }
//    pc.inc()
  }


  // ADD
  fun add(ty: instT, opr1: String, opr2: String) {
    val r1 = name2reg(opr1)
    val r2 = name2reg(opr2)
    var n2 = r2.byte2num()
    when (ty) {
      instT.OP_RR -> r1.add(r2) // ADD ACC, IX
      instT.OP_RImm -> r1.add(r2) // ADD ACC, 3
      instT.OP_RDir -> r1.add(mem[n2]) // ADD ACC, [5]
      instT.OP_RDis -> r1.add(mem[(ix.byte2num() + n2).rem(256)]) // ADD ACC, [IX+2]
      instT.OP_RDir2 -> r1.add(dmem[n2]) // ADD ACC, (5)
      instT.OP_RDis2 -> r1.add(dmem[(ix.byte2num() + n2).rem(256)]) // ADD ACC, (IX+2)
      else -> throw SymError("(in  add)" + ty + ": illegal addressing")
    }
//    cf = r1.getCF()
    vf = r1.getVF()
    setFlag(r1)
//    pc.inc()
  }
  
  // SUB
  fun sub(ty: instT, opr1: String, opr2: String) {
    val r1 = name2reg(opr1)
    val r2 = name2reg(opr2)
//    val n2 = parseNumber0(opr2)
    val n2 = r2.byte2num()
    when (ty) {
      instT.OP_RR -> r1.sub(r2) // SUB ACC, IX
      instT.OP_RImm -> r1.sub(r2) // SUB ACC, 3
      instT.OP_RDir -> r1.sub(mem[n2]) // SUB ACC, [5]
      instT.OP_RDis -> r1.sub(mem[(ix.byte2num() + n2).rem(256)]) // SUB ACC, [IX+2]
      instT.OP_RDir2 -> r1.sub(dmem[n2]) // SUB ACC, (5)
      instT.OP_RDis2 -> r1.sub(dmem[(ix.byte2num() + n2).rem(256)]) // SUB ACC, (IX+2)
      else -> throw SymError("(in  sub)" + ty + ": illegal addressing")
    }
//    cf = r1.getCF()
    vf = r1.getVF()
    setFlag(r1)
//    pc.inc()
  }

  // ADC
  fun adc(ty: instT, opr1: String, opr2: String) {
    add(ty, opr1, opr2)
    val r1 = name2reg(opr1)
    val cf_1 = r1.getCF()
    r1.add(Byte(cf))
    val cf_2 = r1.getCF()
    cf = if (cf_1 > cf_2) cf_1 else cf_2
  }

  // SBC
  fun sbc(ty: instT, opr1: String, opr2: String) {
    sub(ty, opr1, opr2)
    val r1 = name2reg(opr1)
//    cf = r1.getCF()
//    r1.sub(Byte(cf))
    val cf_1 = r1.getCF()
    r1.sub(Byte(cf))
    val cf_2 = r1.getCF()
    cf = if (cf_1 > cf_2) cf_1 else cf_2
  }

  // AND
  fun and(ty: instT, opr1: String, opr2: String) {
    val r1 = name2reg(opr1)
    val r2 = name2reg(opr2)
//    val n2 = parseNumber0(opr2)
    val n2 = r2.byte2num()
    when (ty) {
      instT.OP_RR -> r1.and(r2) // AND ACC, IX
      instT.OP_RImm -> r1.and(r2) // AND ACC, 3
      instT.OP_RDir -> r1.and(mem[n2]) // AND ACC, [5]
      instT.OP_RDis -> r1.and(mem[(ix.byte2num() + n2).rem(256)]) // AND ACC, [IX+2]
      instT.OP_RDir2 -> r1.and(dmem[n2]) // AND ACC, (5)
      instT.OP_RDis2 -> r1.and(dmem[(ix.byte2num() + n2).rem(256)]) // AND ACC, (IX+2)
      else -> throw SymError("(in  and)" + ty + ": illegal addressing")
    }
    setFlag(r1)
    vf = 0
  }

  // OR
  fun or(ty: instT, opr1: String, opr2: String) {
    val r1 = name2reg(opr1)
    val r2 = name2reg(opr2)
//    val n2 = parseNumber0(opr2)
    val n2 = r2.byte2num()
    when (ty) {
      instT.OP_RR -> r1.or(r2) // OR ACC, IX
      instT.OP_RImm -> r1.or(r2) // OR ACC, 3
      instT.OP_RDir -> r1.or(mem[n2]) // OR ACC, [5]
      instT.OP_RDis -> r1.or(mem[(ix.byte2num() + n2).rem(256)]) // OR ACC, [IX+2]
      instT.OP_RDir2 -> r1.or(dmem[n2]) // OR ACC, (5)
      instT.OP_RDis2 -> r1.or(dmem[(ix.byte2num() + n2).rem(256)]) // OR ACC, (IX+2)
      else -> throw SymError("(in  or)" + ty + ": illegal addressing")
    }
    setFlag(r1)
    vf = 0
  }

  // EOR
  fun eor(ty: instT, opr1: String, opr2: String) {
    val r1 = name2reg(opr1)
    val r2 = name2reg(opr2)
//    val n2 = parseNumber0(opr2)
    val n2 = r2.byte2num()
    when (ty) {
      instT.OP_RR -> r1.eor(r2) // EOR ACC, IX
      instT.OP_RImm -> r1.eor(r2) // EOR ACC, 3
      instT.OP_RDir -> r1.eor(mem[n2]) // EOR ACC, [5]
      instT.OP_RDis -> r1.eor(mem[(ix.byte2num() + n2).rem(256)]) // EOR ACC, [IX+2]
      instT.OP_RDir2 -> r1.eor(dmem[n2]) // EOR ACC, (5)
      instT.OP_RDis2 -> r1.eor(dmem[(ix.byte2num() + n2).rem(256)]) // EOR ACC, (IX+2)
      else -> throw SymError("(in  eor)" + ty + ": illegal addressing")
    }
    setFlag(r1)
    vf = 0
  }

  // CMP
  fun cmp(ty: instT, opr1: String, opr2: String) {  
    val r1 = name2reg(opr1)
    val r2 = name2reg(opr2)
    val t = Byte(0)
//    val n2 = parseNumber0(opr2)
    val n2 = r2.byte2num()
    when (ty) {
      instT.OP_RR -> t.copy(r1).sub(r2) // CMP ACC, IX
      instT.OP_RImm -> t.copy(r1).sub(r2) // CMP ACC, 3
      instT.OP_RDir -> t.copy(r1).sub(mem[n2]) // CMP ACC, [5]
      instT.OP_RDis -> t.copy(r1).sub(mem[(ix.byte2num() + n2).rem(256)]) // CMP ACC, [IX+2]
      instT.OP_RDir2 -> t.copy(r1).sub(dmem[n2]) // CMP ACC, (5)
      instT.OP_RDis2 -> t.copy(r1).sub(dmem[(ix.byte2num() + n2).rem(256)]) // CMP ACC, (IX+2)
      else -> throw SymError("(in  cmp)" + ty + ": illegal addressing")
    }
    vf = t.getVF()
    setFlag(t)
  }
  fun sra(opr1: String) {
    val b = name2reg(opr1).sra()
    cf = b.getCF()
    vf = b.getVF()
    setFlag(b)
  }
  fun sla(opr1: String) { 
    val b = name2reg(opr1).sla()
    cf = b.getCF()
    vf = b.getVF()    
    setFlag(b)
  }
  fun srl(opr1: String) {
    val b = name2reg(opr1).srl() 
    cf = b.getCF()
    vf = b.getVF()    
    setFlag(b)
  }
  fun sll(opr1: String) {
    val b = name2reg(opr1).sll() 
    cf = b.getCF()
    vf = b.getVF()    
    setFlag(b)
  }
  fun rra(opr1: String) {
    val b = name2reg(opr1).rra(cf) 
    cf = b.getCF()
    vf = b.getVF()    
    setFlag(b)
  }
  fun rla(opr1: String) {
    val b = name2reg(opr1).rla(cf) 
    cf = b.getCF()
    vf = b.getVF()    
    setFlag(b)
  }
  fun rrl(opr1: String) {
    val b = name2reg(opr1).rrl() 
    cf = b.getCF()
    vf = b.getVF()    
    setFlag(b)
  }
  fun rll(opr1: String) {
    val b = name2reg(opr1).rll() 
    cf = b.getCF()
    vf = b.getVF()    
    setFlag(b)
  }

  fun ba(opr1: String) {  
    pc.num2byte(parseNumber0(opr1))
  }
  fun bvf(opr1: String) {  
    if (vf == 1) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bnz(opr1: String) {  
    if (zerof == 0) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bz(opr1: String) {  
    if (zerof == 1) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bzp(opr1: String) {  
    if (negf == 0) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bn(opr1: String) {  
    if (negf == 1) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bp(opr1: String) {  
    if ((negf == 0) && (zerof == 0)) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bzn(opr1: String) {  
    if ((negf == 1) || (zerof == 1)) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bni(opr1: String) {  
    println("bni: not implemented yet")
    if (false) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bno(opr1: String) {  
    println("bno: not implemented yet")
    if (false) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bnc(opr1: String) {  
    if (cf == 0) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bc(opr1: String) {  
    if (cf == 1) pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bge(opr1: String) {  
    if ((vf == 0) && (negf == 0) || (vf == 1) && (negf == 1)) 
        pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun blt(opr1: String) {  
    if ((vf == 1) && (negf == 0) || (vf == 0) && (negf == 1)) 
        pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun bgt(opr1: String) {  
    if (((vf == 0) && (negf == 0) || (vf == 1) && (negf == 1)) && zerof == 0)
        pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }
  fun ble(opr1: String) {  
    if (((vf == 1) && (negf == 0) || (vf == 0) && (negf == 1)) || zerof == 1)
        pc.num2byte(parseNumber0(opr1)) //else pc.inc()
  }

  fun nop() {}//pc.inc()}
  fun hlt() {opflag = 0}//pc.inc()}
  fun outop() {}//pc.inc()}
  fun inop() {}//pc.inc()}
  fun rcf() {cf = 0}//; pc.inc()}
  fun scf() {cf = 1}//; pc.inc()}

  fun reset() {
    pc.num2byte(0)
    ir.num2byte(0)
    dr.num2byte(0)
    acc.num2byte(0)
    ix.num2byte(0)
    mar.num2byte(0)
    zerof = 0
    negf = 0
    cf = 0
    vf = 0
  }

  fun loadfile(fname: String) {
    try {
      val inputStream: InputStream = File(fname).inputStream()
      val hex = mutableListOf<String>()
      inputStream.bufferedReader().useLines { 
	lines -> lines.forEach {hex.add(it)} 
      }
      var addr = 0
      hex.forEach {
	mem[addr].num2byte((hex[addr]).toLong(radix = 16).toInt())
	addr++
      }
      println("OK.")
    } catch (e: java.io.FileNotFoundException) {
      println(fname + ": no such file")
    } catch (e: SymError) {
      println(e)
    }
  }


  fun single(): Int {
    // Read one byte from [PC] to ir and inc PC.
    ir.copy(mem[pc.byte2num()])
    pc.inc()
    // Decode.
    // If the instruction is a two-byte long,
    // read next byte form [PC] to dr and inc PC.
    val p = decode()
    val ty = p.first.first
    val op = p.first.second
    var opr1 = p.first.third
    var opr2 = p.second
    if (ty == instT.OP_RImm ||
        ty == instT.OP_RDir ||
        ty == instT.OP_RDis ||
        ty == instT.OP_RDir2 ||
        ty == instT.OP_RDis2 ||
        ty == instT.BR) {
      dr.copy(mem[pc.byte2num()])
      opr2 = dr.byte2num().toString()
      pc.inc()
    }
    if (opr1 == "") {
      opr1 = opr2
      opr2 = ""
    }
    println("inst: " + op + " " + opr1 + " " + opr2)
    // Organize the instruction and execute it.
    dispatch(this, op, ty, opr1, opr2)
    return opflag
  }

  tailrec fun run(): Int {
    opflag = 1
    val f = single()
    if (f > 0) {
      print(".")
      return run()
    } else {
      print(".\n")
      return f
    }
  }


  fun decode(): Pair<Triple<instT, String, String>, String> {
    when (ir.byte2num()) {
      0x00 -> return Pair(Triple(instT.OP_R, "nop", ""), "")
      0x0f -> return Pair(Triple(instT.OP_R, "hlt", ""), "")
      0x10 -> return Pair(Triple(instT.OP_R, "out", ""), "")
      0x1f -> return Pair(Triple(instT.OP_R, "in", ""), "")
      0x20 -> return Pair(Triple(instT.OP_R, "rcf", ""), "")
      0x2f -> return Pair(Triple(instT.OP_R, "scf", ""), "")

      0x30 -> return Pair(Triple(instT.BR, "ba", ""), "")
      0x38 -> return Pair(Triple(instT.BR, "bvf", ""), "")
      0x31 -> return Pair(Triple(instT.BR, "bnz", ""), "")
      0x39 -> return Pair(Triple(instT.BR, "bz", ""), "")
      0x32 -> return Pair(Triple(instT.BR, "bzp", ""), "")
      0x3a -> return Pair(Triple(instT.BR, "bn", ""), "")
      0x33 -> return Pair(Triple(instT.BR, "bp", ""), "")
      0x3b -> return Pair(Triple(instT.BR, "bzn", ""), "")
      0x34 -> return Pair(Triple(instT.BR, "bni", ""), "")
      0x3c -> return Pair(Triple(instT.BR, "bno", ""), "")
      0x35 -> return Pair(Triple(instT.BR, "bnc", ""), "")
      0x3d -> return Pair(Triple(instT.BR, "bc", ""), "")
      0x36 -> return Pair(Triple(instT.BR, "bge", ""), "")
      0x3e -> return Pair(Triple(instT.BR, "blt", ""), "")
      0x37 -> return Pair(Triple(instT.BR, "bgt", ""), "")
      0x3f -> return Pair(Triple(instT.BR, "ble", ""), "")

      0x40 -> return Pair(Triple(instT.OP_R, "sra", "acc"), "")
      0x41 -> return Pair(Triple(instT.OP_R, "sla", "acc"), "")
      0x42 -> return Pair(Triple(instT.OP_R, "srl", "acc"), "")
      0x43 -> return Pair(Triple(instT.OP_R, "sll", "acc"), "")
      0x44 -> return Pair(Triple(instT.OP_R, "rra", "acc"), "")
      0x45 -> return Pair(Triple(instT.OP_R, "rla", "acc"), "")
      0x46 -> return Pair(Triple(instT.OP_R, "rrl", "acc"), "")
      0x47 -> return Pair(Triple(instT.OP_R, "rll", "acc"), "")
      0x48 -> return Pair(Triple(instT.OP_R, "sra", "ix"), "")
      0x49 -> return Pair(Triple(instT.OP_R, "sla", "ix"), "")
      0x4a -> return Pair(Triple(instT.OP_R, "srl", "ix"), "")
      0x4b -> return Pair(Triple(instT.OP_R, "sll", "ix"), "")
      0x4c -> return Pair(Triple(instT.OP_R, "rra", "ix"), "")
      0x4d -> return Pair(Triple(instT.OP_R, "rla", "ix"), "")
      0x4e -> return Pair(Triple(instT.OP_R, "rrl", "ix"), "")
      0x4f -> return Pair(Triple(instT.OP_R, "rll", "ix"), "")

      0x60 -> return Pair(Triple(instT.OP_RR, "ld", "acc"), "acc")
      0x61 -> return Pair(Triple(instT.OP_RR, "ld", "acc"), "ix")
      0x62 -> return Pair(Triple(instT.OP_RImm, "ld", "acc"), "")
      0x64 -> return Pair(Triple(instT.OP_RDir, "ld", "acc"), "")
      0x65 -> return Pair(Triple(instT.OP_RDir2, "ld", "acc"), "")
      0x66 -> return Pair(Triple(instT.OP_RDis, "ld", "acc"), "")
      0x67 -> return Pair(Triple(instT.OP_RDis2, "ld", "acc"), "")
      0x68 -> return Pair(Triple(instT.OP_RR, "ld", "ix"), "acc")
      0x69 -> return Pair(Triple(instT.OP_RR, "ld", "ix"), "ix")
      0x6a -> return Pair(Triple(instT.OP_RImm, "ld", "ix"), "")
      0x6c -> return Pair(Triple(instT.OP_RDir, "ld", "ix"), "")
      0x6d -> return Pair(Triple(instT.OP_RDir2, "ld", "ix"), "")
      0x6e -> return Pair(Triple(instT.OP_RDis, "ld", "ix"), "")
      0x6f -> return Pair(Triple(instT.OP_RDis2, "ld", "ix"), "")

      0x74 -> return Pair(Triple(instT.OP_RDir, "st", "acc"), "")
      0x75 -> return Pair(Triple(instT.OP_RDir2, "st", "acc"), "")
      0x76 -> return Pair(Triple(instT.OP_RDis, "st", "acc"), "")
      0x77 -> return Pair(Triple(instT.OP_RDis2, "st", "acc"), "")
      0x7c -> return Pair(Triple(instT.OP_RDir, "st", "ix"), "")
      0x7d -> return Pair(Triple(instT.OP_RDir2, "st", "ix"), "")
      0x7e -> return Pair(Triple(instT.OP_RDis, "st", "ix"), "")
      0x7f -> return Pair(Triple(instT.OP_RDis2, "st", "ix"), "")

      0x80 -> return Pair(Triple(instT.OP_RR, "sbc", "acc"), "acc")
      0x81 -> return Pair(Triple(instT.OP_RR, "sbc", "acc"), "ix")
      0x82 -> return Pair(Triple(instT.OP_RImm, "sbc", "acc"), "")
      0x84 -> return Pair(Triple(instT.OP_RDir, "sbc", "acc"), "")
      0x85 -> return Pair(Triple(instT.OP_RDir2, "sbc", "acc"), "")
      0x86 -> return Pair(Triple(instT.OP_RDis, "sbc", "acc"), "")
      0x87 -> return Pair(Triple(instT.OP_RDis2, "sbc", "acc"), "")
      0x88 -> return Pair(Triple(instT.OP_RR, "sbc", "ix"), "acc")
      0x89 -> return Pair(Triple(instT.OP_RR, "sbc", "ix"), "ix")
      0x8a -> return Pair(Triple(instT.OP_RImm, "sbc", "ix"), "")
      0x8c -> return Pair(Triple(instT.OP_RDir, "sbc", "ix"), "")
      0x8d -> return Pair(Triple(instT.OP_RDir2, "sbc", "ix"), "")
      0x8e -> return Pair(Triple(instT.OP_RDis, "sbc", "ix"), "")
      0x8f -> return Pair(Triple(instT.OP_RDis2, "sbc", "ix"), "")

      0x90 -> return Pair(Triple(instT.OP_RR, "adc", "acc"), "acc")
      0x91 -> return Pair(Triple(instT.OP_RR, "adc", "acc"), "ix")
      0x92 -> return Pair(Triple(instT.OP_RImm, "adc", "acc"), "")
      0x94 -> return Pair(Triple(instT.OP_RDir, "adc", "acc"), "")
      0x95 -> return Pair(Triple(instT.OP_RDir2, "adc", "acc"), "")
      0x96 -> return Pair(Triple(instT.OP_RDis, "adc", "acc"), "")
      0x97 -> return Pair(Triple(instT.OP_RDis2, "adc", "acc"), "")
      0x98 -> return Pair(Triple(instT.OP_RR, "adc", "ix"), "acc")
      0x99 -> return Pair(Triple(instT.OP_RR, "adc", "ix"), "ix")
      0x9a -> return Pair(Triple(instT.OP_RImm, "adc", "ix"), "")
      0x9c -> return Pair(Triple(instT.OP_RDir, "adc", "ix"), "")
      0x9d -> return Pair(Triple(instT.OP_RDir2, "adc", "ix"), "")
      0x9e -> return Pair(Triple(instT.OP_RDis, "adc", "ix"), "")
      0x9f -> return Pair(Triple(instT.OP_RDis2, "adc", "ix"), "")

      0xa0 -> return Pair(Triple(instT.OP_RR, "sub", "acc"), "acc")
      0xa1 -> return Pair(Triple(instT.OP_RR, "sub", "acc"), "ix")
      0xa2 -> return Pair(Triple(instT.OP_RImm, "sub", "acc"), "")
      0xa4 -> return Pair(Triple(instT.OP_RDir, "sub", "acc"), "")
      0xa5 -> return Pair(Triple(instT.OP_RDir2, "sub", "acc"), "")
      0xa6 -> return Pair(Triple(instT.OP_RDis, "sub", "acc"), "")
      0xa7 -> return Pair(Triple(instT.OP_RDis2, "sub", "acc"), "")
      0xa8 -> return Pair(Triple(instT.OP_RR, "sub", "ix"), "acc")
      0xa9 -> return Pair(Triple(instT.OP_RR, "sub", "ix"), "ix")
      0xaa -> return Pair(Triple(instT.OP_RImm, "sub", "ix"), "")
      0xac -> return Pair(Triple(instT.OP_RDir, "sub", "ix"), "")
      0xad -> return Pair(Triple(instT.OP_RDir2, "sub", "ix"), "")
      0xae -> return Pair(Triple(instT.OP_RDis, "sub", "ix"), "")
      0xaf -> return Pair(Triple(instT.OP_RDis2, "sub", "ix"), "")

      0xb0 -> return Pair(Triple(instT.OP_RR, "add", "acc"), "acc")
      0xb1 -> return Pair(Triple(instT.OP_RR, "add", "acc"), "ix")
      0xb2 -> return Pair(Triple(instT.OP_RImm, "add", "acc"), "")
      0xb4 -> return Pair(Triple(instT.OP_RDir, "add", "acc"), "")
      0xb5 -> return Pair(Triple(instT.OP_RDir2, "add", "acc"), "")
      0xb6 -> return Pair(Triple(instT.OP_RDis, "add", "acc"), "")
      0xb7 -> return Pair(Triple(instT.OP_RDis2, "add", "acc"), "")
      0xb8 -> return Pair(Triple(instT.OP_RR, "add", "ix"), "acc")
      0xb9 -> return Pair(Triple(instT.OP_RR, "add", "ix"), "ix")
      0xba -> return Pair(Triple(instT.OP_RImm, "add", "ix"), "")
      0xbc -> return Pair(Triple(instT.OP_RDir, "add", "ix"), "")
      0xbd -> return Pair(Triple(instT.OP_RDir2, "add", "ix"), "")
      0xbe -> return Pair(Triple(instT.OP_RDis, "add", "ix"), "")
      0xbf -> return Pair(Triple(instT.OP_RDis2, "add", "ix"), "")

      0xc0 -> return Pair(Triple(instT.OP_RR, "eor", "acc"), "acc")
      0xc1 -> return Pair(Triple(instT.OP_RR, "eor", "acc"), "ix")
      0xc2 -> return Pair(Triple(instT.OP_RImm, "eor", "acc"), "")
      0xc4 -> return Pair(Triple(instT.OP_RDir, "eor", "acc"), "")
      0xc5 -> return Pair(Triple(instT.OP_RDir2, "eor", "acc"), "")
      0xc6 -> return Pair(Triple(instT.OP_RDis, "eor", "acc"), "")
      0xc7 -> return Pair(Triple(instT.OP_RDis2, "eor", "acc"), "")
      0xc8 -> return Pair(Triple(instT.OP_RR, "eor", "ix"), "acc")
      0xc9 -> return Pair(Triple(instT.OP_RR, "eor", "ix"), "ix")
      0xca -> return Pair(Triple(instT.OP_RImm, "eor", "ix"), "")
      0xcc -> return Pair(Triple(instT.OP_RDir, "eor", "ix"), "")
      0xcd -> return Pair(Triple(instT.OP_RDir2, "eor", "ix"), "")
      0xce -> return Pair(Triple(instT.OP_RDis, "eor", "ix"), "")
      0xcf -> return Pair(Triple(instT.OP_RDis2, "eor", "ix"), "")

      0xd0 -> return Pair(Triple(instT.OP_RR, "or", "acc"), "acc")
      0xd1 -> return Pair(Triple(instT.OP_RR, "or", "acc"), "ix")
      0xd2 -> return Pair(Triple(instT.OP_RImm, "or", "acc"), "")
      0xd4 -> return Pair(Triple(instT.OP_RDir, "or", "acc"), "")
      0xd5 -> return Pair(Triple(instT.OP_RDir2, "or", "acc"), "")
      0xd6 -> return Pair(Triple(instT.OP_RDis, "or", "acc"), "")
      0xd7 -> return Pair(Triple(instT.OP_RDis2, "or", "acc"), "")
      0xd8 -> return Pair(Triple(instT.OP_RR, "or", "ix"), "acc")
      0xd9 -> return Pair(Triple(instT.OP_RR, "or", "ix"), "ix")
      0xda -> return Pair(Triple(instT.OP_RImm, "or", "ix"), "")
      0xdc -> return Pair(Triple(instT.OP_RDir, "or", "ix"), "")
      0xdd -> return Pair(Triple(instT.OP_RDir2, "or", "ix"), "")
      0xde -> return Pair(Triple(instT.OP_RDis, "or", "ix"), "")
      0xdf -> return Pair(Triple(instT.OP_RDis2, "or", "ix"), "")

      0xe0 -> return Pair(Triple(instT.OP_RR, "and", "acc"), "acc")
      0xe1 -> return Pair(Triple(instT.OP_RR, "and", "acc"), "ix")
      0xe2 -> return Pair(Triple(instT.OP_RImm, "and", "acc"), "")
      0xe4 -> return Pair(Triple(instT.OP_RDir, "and", "acc"), "")
      0xe5 -> return Pair(Triple(instT.OP_RDir2, "and", "acc"), "")
      0xe6 -> return Pair(Triple(instT.OP_RDis, "and", "acc"), "")
      0xe7 -> return Pair(Triple(instT.OP_RDis2, "and", "acc"), "")
      0xe8 -> return Pair(Triple(instT.OP_RR, "and", "ix"), "acc")
      0xe9 -> return Pair(Triple(instT.OP_RR, "and", "ix"), "ix")
      0xea -> return Pair(Triple(instT.OP_RImm, "and", "ix"), "")
      0xec -> return Pair(Triple(instT.OP_RDir, "and", "ix"), "")
      0xed -> return Pair(Triple(instT.OP_RDir2, "and", "ix"), "")
      0xee -> return Pair(Triple(instT.OP_RDis, "and", "ix"), "")
      0xef -> return Pair(Triple(instT.OP_RDis2, "and", "ix"), "")

      0xf0 -> return Pair(Triple(instT.OP_RR, "cmp", "acc"), "acc")
      0xf1 -> return Pair(Triple(instT.OP_RR, "cmp", "acc"), "ix")
      0xf2 -> return Pair(Triple(instT.OP_RImm, "cmp", "acc"), "")
      0xf4 -> return Pair(Triple(instT.OP_RDir, "cmp", "acc"), "")
      0xf5 -> return Pair(Triple(instT.OP_RDir2, "cmp", "acc"), "")
      0xf6 -> return Pair(Triple(instT.OP_RDis, "cmp", "acc"), "")
      0xf7 -> return Pair(Triple(instT.OP_RDis2, "cmp", "acc"), "")
      0xf8 -> return Pair(Triple(instT.OP_RR, "cmp", "ix"), "acc")
      0xf9 -> return Pair(Triple(instT.OP_RR, "cmp", "ix"), "ix")
      0xfa -> return Pair(Triple(instT.OP_RImm, "cmp", "ix"), "")
      0xfc -> return Pair(Triple(instT.OP_RDir, "cmp", "ix"), "")
      0xfd -> return Pair(Triple(instT.OP_RDir2, "cmp", "ix"), "")
      0xfe -> return Pair(Triple(instT.OP_RDis, "cmp", "ix"), "")
      0xff -> return Pair(Triple(instT.OP_RDis2, "cmp", "ix"), "")

      else -> return Pair(Triple(instT.EQU, "dummy", ""), "")
    }
  }
}

enum class instT {
  EQU,
  OP,
  OP_R,
  OP_RR,
  OP_RImm,
  OP_RDir,
  OP_RDir2,
  OP_RDis,
  OP_RDis2,
  BR,
  COM
}

class SymError(override var message:String): Exception(message)


tailrec fun repl(cpu: KueChip2Board) {
  val spc = "[ \t]*"
  val op_pat = "([a-z][a-z][a-z]?)"
//  val ident = "([a-z][a-z0-9]*)"
  val mode_reg = "(acc|ix)"
  val num = "([\\+\\-]?[0-9][0-9a-f]*h?)"
  val mode_imm = "((\\+|\\-)?" + num + ")"
  val mode_dir = "\\[ *" + num + " *\\]"
  val mode_dir2 = "\\( *" + num + " *\\)"
  val mode_dis0 = "\\[ *ix *\\]"
  val mode_dis = "\\[ *ix *(\\+|\\-) *" + num + " *\\]"
  val mode_dis2 = "\\( *ix *(\\+|\\-) *" + num + " *\\)"
  val sep = spc + ",?" + spc
  val inst_op = spc + op_pat + spc
  val inst_r = spc + op_pat + spc + mode_reg + spc
  val inst_rr = spc + op_pat + spc + mode_reg + sep + mode_reg + spc
  val inst_rimm = spc + op_pat + spc + mode_reg + sep + mode_imm + spc
  val inst_rdir = spc + op_pat + spc + mode_reg + sep + mode_dir + spc
  val inst_rdir2 = spc + op_pat + spc + mode_reg + sep + mode_dir2 + spc
  val inst_rdis0 = spc + op_pat + spc + mode_reg + sep + mode_dis0 + spc
  val inst_rdis = spc + op_pat + spc + mode_reg + sep + mode_dis + spc
  val inst_rdis2 = spc + op_pat + spc + mode_reg + sep + mode_dis2 + spc
//  val inst_b = spc + op_pat + spc + ident + spc
  val inst_b = spc + op_pat + spc + "([0-9][0-9a-f]*h?)" + spc
  val regex_op = inst_op.toRegex()
  val regex_r = inst_r.toRegex()
  val regex_rr = inst_rr.toRegex()
  val regex_rimm = inst_rimm.toRegex()
  val regex_rdir = inst_rdir.toRegex()
  val regex_rdir2 = inst_rdir2.toRegex()
  val regex_rdis0 = inst_rdis0.toRegex()
  val regex_rdis = inst_rdis.toRegex()
  val regex_rdis2 = inst_rdis2.toRegex()
  val regex_b = inst_b.toRegex()
  val regex_command = (" *(reg|mem|dmem|help|pmode|bye|all|rst|si|ss|set) *").toRegex()
  val regex_lf = " *(lf) *([\\./a-z][\\./a-z0-9]*) *".toRegex()
  val regex_set = (spc + "(set)" + spc + num + sep
                  + "\\[" + spc + num + spc + "\\]" + spc).toRegex()
  val regex_setd = (spc + "(set)" + spc + num + sep
                  + "\\(" + spc + num + spc + "\\)" + spc).toRegex()

  print("> ")
  val line0 = readLine()

  if (line0 != null) {

    try {


    val line = line0.toLowerCase()
    if (regex_command.matches(line)) { // COM
      val (p1) = regex_command.find(line)!!.destructured
      when (p1) {
	"help" -> {
	  println("\tmem \t: print memory")
	  println("\treg \t: print register")
	  println("\tall \t: print memory and register")
	  println("\tlf file\t: load file in HEX format")
	  println("\trst \t: reset system")
	  println("\tset v [a]\t: set mem[a] to v")
	  println("\tsi \t: execute single instruction pointed by PC")
	  println("\tss \t: start executing instructions from the place pointed by PC")
	  println("\tpmode \t: toggle print mode (hex/decimal)")
	  println("\tbye \t: exit")
	}
	"bye" -> return
	"reg" -> {cpu.printRegs(); println()}
	"mem" -> {cpu.printMem(); println()}
	"dmem" -> {cpu.printDMem(); println()}
	"all" -> {cpu.printAll(); println()}
	"rst" -> cpu.reset()
	"si" -> cpu.single()
	"ss" -> cpu.run()
	"pmode" -> cpu.togglePrintMode()
      }
    } else if (regex_lf.matches(line)) {
      val (p1, p2) = regex_lf.find(line)!!.destructured
      when (p1) {
	"lf" -> cpu.loadfile(p2)
      }
    } else if (regex_set.matches(line)) {
      val (p1, p2, p3) = regex_set.find(line)!!.destructured
//      println("(SET) p1=" + p1 + " p2=" + p2 + " p3=" + p3)
      when (p1) {
	"set" -> cpu.setMem(p2, p3)
      }
    } else if (regex_setd.matches(line)) {
      val (p1, p2, p3) = regex_setd.find(line)!!.destructured
//      println("(SETD) p1=" + p1 + " p2=" + p2 + " p3=" + p3)
      when (p1) {
	"set" -> cpu.setDMem(p2, p3)
      }
    } else if (regex_rr.matches(line)) { // OP_RR
      val (p1, p2, p3) = regex_rr.find(line)!!.destructured
      dispatch(cpu, p1, instT.OP_RR, p2, p3)
    } else if (regex_rimm.matches(line)) { // OP_RImm
      val (p1, p2, p3) = regex_rimm.find(line)!!.destructured
//      println("(RImm) p1=" + p1 + " p2=" + p2 + " p3=" + p3)
      dispatch(cpu, p1, instT.OP_RImm, p2, p3)
    } else if (regex_rdir.matches(line)) { // OP_RDir
      val (p1, p2, p3) = regex_rdir.find(line)!!.destructured
//      println("(RDir) p1=" + p1 + " p2=" + p2 + " p3=" + p3)
      dispatch(cpu, p1, instT.OP_RDir, p2, p3)
    } else if (regex_rdir2.matches(line)) { // OP_RDir2
      val (p1, p2, p3) = regex_rdir2.find(line)!!.destructured
//      println("(RDir2) p1=" + p1 + " p2=" + p2 + " p3=" + p3)
      dispatch(cpu, p1, instT.OP_RDir2, p2, p3)
    } else if (regex_rdis0.matches(line)) { // OP_RDis
      val (p1, p2) = regex_rdis0.find(line)!!.destructured
//      println("(RDis0) p1=" + p1 + " p2=" + p2)
      dispatch(cpu, p1, instT.OP_RDis, p2, "0")
    } else if (regex_rdis.matches(line)) { // OP_RDis
      val (p1, p2, p3, p4) = regex_rdis.find(line)!!.destructured
//      println("(RDis) p1=" + p1 + " p2=" + p2 + " p3=" + p3 + " p4=" + p4)
      dispatch(cpu, p1, instT.OP_RDis, p2, if (p3=="+") p4 else {p3+p4})
    } else if (regex_rdis2.matches(line)) { // OP_RDis
      val (p1, p2, p3, p4) = regex_rdis2.find(line)!!.destructured
//      println("(RDis2) p1=" + p1 + " p2=" + p2 + " p3=" + p3 + " p4=" + p4)
      dispatch(cpu, p1, instT.OP_RDis2, p2, if (p3=="+") p4 else {p3+p4})
    } else if (regex_r.matches(line)) { // OP_R
      val (p1, p2) = regex_r.find(line)!!.destructured
      dispatch(cpu, p1, instT.OP_R, p2, "")
    } else if (regex_op.matches(line)) { // OP
      val (p1) = regex_op.find(line)!!.destructured
      dispatch(cpu, p1, instT.OP, "", "")
    } else if (regex_b.matches(line)) { // BR
      val (p1, p2) = regex_b.find(line)!!.destructured
      dispatch(cpu, p1, instT.BR, p2, "")
/*
    } else if (regex_set_op.matches(line)) {
      val (addr, p1) = regex_set.find(line)!!.destructured
*/      
    } else if (spc.toRegex().matches(line)) {
      // do nothing
    } else {
      println("parse error")
    }




    } catch (e: SymError) {
      println(e)
    }

    return repl(cpu)
  }
}


fun dispatch(cpu: KueChip2Board, op: String, inst_type: instT,
             opr1: String, opr2: String) {
    when (op) {
      "ld" -> cpu.ld(inst_type, opr1, opr2)
      "st" -> cpu.st(inst_type, opr1, opr2)
      "add" -> cpu.add(inst_type, opr1, opr2)
      "sub" -> cpu.sub(inst_type, opr1, opr2)
      "adc" -> cpu.adc(inst_type, opr1, opr2)
      "sbc" -> cpu.sbc(inst_type, opr1, opr2)
      "and" -> cpu.and(inst_type, opr1, opr2)
      "or" -> cpu.or(inst_type, opr1, opr2)
      "eor" -> cpu.eor(inst_type, opr1, opr2)
      "cmp" -> cpu.cmp(inst_type, opr1, opr2)
      "sra" -> cpu.sra(opr1)
      "sla" -> cpu.sla(opr1)
      "srl" -> cpu.srl(opr1)
      "sll" -> cpu.sll(opr1)
      "rra" -> cpu.rra(opr1)
      "rla" -> cpu.rla(opr1)
      "rrl" -> cpu.rrl(opr1)
      "rll" -> cpu.rll(opr1)

      "ba" -> cpu.ba(opr1)
      "bvf" -> cpu.bvf(opr1)
      "bnz" -> cpu.bnz(opr1)
      "bz" -> cpu.bz(opr1)
      "bzp" -> cpu.bzp(opr1)
      "bn" -> cpu.bn(opr1)
      "bp" -> cpu.bp(opr1)
      "bzn" -> cpu.bzn(opr1)
      "bni" -> cpu.bni(opr1)
      "bno" -> cpu.bno(opr1)
      "bnc" -> cpu.bnc(opr1)
      "bc" -> cpu.bc(opr1)
      "bge" -> cpu.bge(opr1)
      "blt" -> cpu.blt(opr1)
      "bgt" -> cpu.bgt(opr1)
      "ble" -> cpu.ble(opr1)

      "nop" -> cpu.nop()
      "hlt" -> cpu.hlt()
      "out" -> cpu.outop()
      "in" -> cpu.inop()
      "rcf" -> cpu.rcf()
      "scf" -> cpu.scf()
      else -> {
	println("illegal command/instruction")
      }
    }
    return
}


fun monitor() {
  print("Starting KueChip2 Simulator ...")
  val cpu = KueChip2Board()
  println(" OK.")
  repl(cpu)
}


fun main(args: Array<String>) {
  monitor()
  println("Bye.")
}

