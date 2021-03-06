/*
  KUE-CHIP2 Assembler

  Copyright (C) 2018 Hideyuki Kawabata
*/

import java.io.File
import java.io.InputStream

enum class instT {
  EQU,
  OP,
  OP_R,
  OP_RR,
  OP_RImm,
  OP_RDir,
  OP_RDis,
  OP_RDir2,
  OP_RDis2,
  OP_Imm,
  LBL,
  PROG
}

class AsmError(override var message:String): Exception(message)

typealias IMElem = Triple<Triple<String, String, String>, instT, String>
typealias IMList = MutableList<IMElem>

class LabelList() {
  var labels: MutableList<Pair<String, Int>>
  var ca: Int
  init {
    labels = mutableListOf<Pair<String,Int>>()
    ca = 0
  }
  fun resetCA() {ca = 0}
  fun addCA(i: Int) {ca += i}
  fun updateLabel(s: String) {
    if (s != "") labels.add(Pair(s, ca))
  }
  fun updateLabelWithValue(s: String, v: Int) {
    if (s != "") labels.add(Pair(s, v))
  }
  fun getAddr(lbl: String): Int { // label -> addr
    if (lbl == "ca") return ca
    labels.forEach {if (it.first == lbl) return it.second}
    throw AsmError("(in getAddr)" + lbl + ": does not exist")
  }
  fun parseNumberSimple(s: String): Int { // {ID, 0AH, 3} -> Int
    val ca = s.toCharArray()
    if (!s[0].isDigit()) {
      return getAddr(s) // ID
    } else if (s[ca.size-1] == 'h') {
      var s2 = s.trim({ch -> ch == 'h'})
      return (s2.toLong(radix = 16).toInt()) // 0AH
    } else {
      return s.toInt() // 3
    }
  }
  fun evalExpr(s: String): Int {
    val pat = "[ \t]*(\\+|\\-)?[ \t]*([0-9][0-9a-f]*h?|[a-z][a-z0-9]*)(.*)".toRegex()
    tailrec fun f(s: String, n: Int): Int {
      if (pat.matches(s)) {
        val (o, nb, rest) = pat.find(s)!!.destructured
        val v = parseNumberSimple(nb)
        val n2 = if (o == "-") (n - v) else (n + v)
        return f(rest, n2)
      } else {
        return n
      }
    }
    val n = f(s, 0).rem(256)
    return (if (n >= 0) n else n + 256)
  }
  fun evalExprWithCA(s: String, i: Int): Int {
    val tmp = ca
    ca = i
    val v = evalExpr(s)
    ca = tmp
    return v
  }
}


fun parseInputList(lineList: List<String>): Pair<IMList, LabelList> {
  val spc = "[ \t]*"
  val op_pat = "([a-z][a-z][a-z]?)"
  val ident = "([a-z][a-z0-9]*)"
  val num = "([0-9][0-9a-f]*h?)"
  val mode_reg = "(acc|ix)"
  val mode_imm = "((\\+|\\-)?(" + num + "|" + ident + "))"
  val expr = "((" + "(\\+|\\-)?" + spc + "(" + num + "|" + ident + ")" + ")*)"
  val mode_dir = "\\[" + spc + expr + spc + "\\]"
  val mode_dis = "\\[" + spc + "ix" + spc + expr + spc + "\\]"
  val mode_dir2 = "\\(" + spc + expr + spc + "\\)"
  val mode_dis2 = "\\(" + spc + "ix" + spc + expr + spc + "\\)"
  val sep = spc + ",?" + spc
  val comment = "(;.*|\\*.*)?"
  val inst_op = spc + op_pat + spc + comment
  val inst_r = spc + op_pat + spc + mode_reg + spc + comment
  val inst_rr = spc + op_pat + spc + mode_reg + sep + mode_reg + spc + comment
  val inst_rimm = spc + op_pat + spc + mode_reg + sep + mode_imm + spc + comment
  val inst_rdir = spc + op_pat + spc + mode_reg + sep + mode_dir + spc + comment
  val inst_rdis = spc + op_pat + spc + mode_reg + sep + mode_dis + spc + comment
  val inst_rdir2 = spc + op_pat + spc + mode_reg + sep + mode_dir2 + spc + comment
  val inst_rdis2 = spc + op_pat + spc + mode_reg + sep + mode_dis2 + spc + comment
  val inst_b = spc + op_pat + spc + expr + spc + comment
  val inst_equ = spc + "(equ)" + spc + expr + spc + comment
  val inst_prog = spc + "(prog|data)" + spc + expr + spc + comment
  val lab_prefix = spc + "(" + ident + "?[ \t]*:)?"
  val regex_op = (lab_prefix + inst_op).toRegex() // L: SCF
  val regex_r = (lab_prefix + inst_r).toRegex() // L: SRA ACC
  val regex_rr = (lab_prefix + inst_rr).toRegex() // L: ADD ACC, IX
  val regex_rimm = (lab_prefix + inst_rimm).toRegex() // L: ADD ACC, 3
  val regex_rdir = (lab_prefix + inst_rdir).toRegex() // L: ADD ACC, [3]
  val regex_rdis = (lab_prefix + inst_rdis).toRegex() // L: ADD ACC, [IX+3]
  val regex_rdir2 = (lab_prefix + inst_rdir2).toRegex() // L: ADD ACC, (3)
  val regex_rdis2 = (lab_prefix + inst_rdis2).toRegex() // L: ADD ACC, (IX+3)
  val regex_b = (lab_prefix + inst_b).toRegex() // L: BNE L2
  val regex_lline = (lab_prefix + spc + comment).toRegex()
  val regex_equ = (lab_prefix + inst_equ).toRegex() // L: EQU 80H
  val regex_prog = (lab_prefix + inst_prog).toRegex() // L: PROG 80H

  var labels = LabelList()
  var parsedList = mutableListOf<IMElem>()

  fun parseLn(lbl: String, op: String, r1: String, r2: String, ty: instT) {
    val lblc = if (lbl == "") "" else (lbl + ":")
    parsedList.add(Triple(Triple(op, r1, r2), ty, lblc))
    if (ty == instT.EQU) {
      labels.updateLabelWithValue(lbl, labels.evalExpr(r1))
    } else {
      labels.updateLabel(lbl) // use CA
    }
    when (ty) {
      instT.EQU -> {}
      instT.OP -> labels.addCA(1)
      instT.OP_R -> labels.addCA(1)
      instT.OP_RR -> labels.addCA(1)
      instT.OP_RImm -> labels.addCA(2)
      instT.OP_RDis -> labels.addCA(2)
      instT.OP_RDir -> labels.addCA(2)
      instT.OP_RDis2 -> labels.addCA(2)
      instT.OP_RDir2 -> labels.addCA(2)
      instT.OP_Imm -> labels.addCA(2)
      instT.LBL -> {}
      instT.PROG -> labels.addCA(1)
    }
  }

  labels.resetCA()
  lineList.forEach {
    val line = it.toLowerCase()
    if (regex_op.matches(line)) { // L: SCF
      val (_, lbl, p1) = regex_op.find(line)!!.destructured
      parseLn(lbl, p1, "", "", instT.OP)
    } else if (regex_r.matches(line)) { // L: SRA ACC
      val (_, lbl, p1, p2) = regex_r.find(line)!!.destructured
      parseLn(lbl, p1, p2, "", instT.OP_R)
    } else if (regex_rr.matches(line)) { // L: ADD ACC, IX
      val (_, lbl, p1, p2, p3) = regex_rr.find(line)!!.destructured
      parseLn(lbl, p1, p2, p3, instT.OP_RR)
    } else if (regex_rimm.matches(line)) { // L: ADD ACC, 3
      val (_, lbl, p1, p2, p3) = regex_rimm.find(line)!!.destructured
      parseLn(lbl, p1, p2, p3, instT.OP_RImm)
    } else if (regex_rdis.matches(line)) { // L: ADD ACC, [IX+3]
      val (_, lbl, p1, p2, p3) = regex_rdis.find(line)!!.destructured
      parseLn(lbl, p1, p2, p3, instT.OP_RDis)
    } else if (regex_rdir.matches(line)) { // L: ADD ACC, [3]
      val (_, lbl, p1, p2, p3) = regex_rdir.find(line)!!.destructured
      parseLn(lbl, p1, p2, p3, instT.OP_RDir)
    } else if (regex_rdis2.matches(line)) { // L: ADD ACC, (IX+3)
      val (_, lbl, p1, p2, p3) = regex_rdis2.find(line)!!.destructured
      parseLn(lbl, p1, p2, p3, instT.OP_RDis2)
    } else if (regex_rdir2.matches(line)) { // L: ADD ACC, (3)
      val (_, lbl, p1, p2, p3) = regex_rdir2.find(line)!!.destructured
      parseLn(lbl, p1, p2, p3, instT.OP_RDir2)
    } else if (regex_equ.matches(line)) { // L: EQU 80H
      val (_, lbl, p1, p2) = regex_equ.find(line)!!.destructured
      parseLn(lbl, p1, p2, "", instT.EQU)
    } else if (regex_prog.matches(line)) { // L: PROG 80H
      val (_, lbl, p1, p2) = regex_prog.find(line)!!.destructured
      parseLn(lbl, p1, p2, "", instT.PROG)
    } else if (regex_b.matches(line)) { // L: BNE L2
      val (_, lbl, p1, p2) = regex_b.find(line)!!.destructured
      parseLn(lbl, p1, p2, "", instT.OP_Imm)
    } else if (regex_lline.matches(line)) { // L: 
      val (_, lbl) = regex_lline.find(line)!!.destructured
      parseLn(lbl, "*lbl*", "", "", instT.LBL)
    } else if (spc.toRegex().matches(line)) { // empty string; do nothing
    } else {
      throw AsmError("(in parseInputList) " + line + ": syntax error")
    }
  }
  return Pair(parsedList, labels)
}


fun arrangeByteSeq(arg: Pair<IMList, LabelList>): List<Int> {
  val parsedList = arg.first
  val labels = arg.second
  var mem: MutableList<Int> = mutableListOf<Int>()

  fun set_bytes_s(a1: Int, a2: Int, s2: String) {
    if (s2=="acc") mem.add(a1) else mem.add(a2)
  }
  fun set_bytes_b(op: Int, s2: String) {
    mem.add(op)
    mem.add(labels.evalExprWithCA(s2, mem.size-1))
  }
  fun set_bytes_a(s2: String, s3: String, aa: Int, ai: Int, ia: Int, ii: Int,
                  it: instT, p: Int, q: Int, r: Int, s: Int, t: Int, u: Int,
                  v: Int, w: Int, x: Int, y: Int) {
      fun set_bytes0(it: instT, addr: Int, n1: Int, n2: Int,
                     n4: Int, n5: Int, n6: Int) {
      fun f(n: Int) {mem.add(n); mem.add(addr)}
      when (it) {
        instT.OP_RImm -> f(n1)
        instT.OP_RDir -> f(n2)
        instT.OP_RDis -> f(n4)
        instT.OP_RDir2 -> f(n5)
        instT.OP_RDis2 -> f(n6)
        else -> throw AsmError("(in set_bytes0)" + it + ": illegal inst type")
      }
    }
    when (s2) {
      "acc" -> when (s3) {
        "acc" -> mem.add(aa) // EOR ACC, ACC
        "ix" -> mem.add(ai) // EOR ACC, IX
        else -> set_bytes0(it, labels.evalExprWithCA(s3, mem.size), p, q, r, v, w)
      }
      "ix" -> when (s3) {
        "acc" -> mem.add(ia) // EOR IX, ACC
        "ix" -> mem.add(ii) // EOR IX, IX
        else -> set_bytes0(it, labels.evalExprWithCA(s3, mem.size), s, t, u, x, y)
      }
    }
  }

  parsedList.forEach{
    val tri = it.first
    val s1 = tri.first
    val s2 = tri.second
    val s3 = tri.third
    val inst_type = it.second
    when (s1) {
      "nop" -> mem.add(0x00) // OP
      "hlt" -> mem.add(0x0f)
      "out" -> mem.add(0x10)
      "in" -> mem.add(0x1f)
      "rcf" -> mem.add(0x20)
      "scf" -> mem.add(0x2f)
      "sra" -> set_bytes_s(0x40, 0x48, s2) // OP_R
      "sla" -> set_bytes_s(0x41, 0x49, s2)
      "srl" -> set_bytes_s(0x42, 0x4a, s2)
      "sll" -> set_bytes_s(0x43, 0x4b, s2)
      "rra" -> set_bytes_s(0x44, 0x4c, s2)
      "rla" -> set_bytes_s(0x45, 0x4d, s2)
      "rrl" -> set_bytes_s(0x46, 0x4e, s2)
      "rll" -> set_bytes_s(0x47, 0x4f, s2)
      "ba" -> set_bytes_b(0x30, s2) // OP_Imm
      "bvf" -> set_bytes_b(0x38, s2)
      "bnz" -> set_bytes_b(0x31, s2)
      "bz" -> set_bytes_b(0x39, s2)
      "bzp" -> set_bytes_b(0x32, s2)
      "bn" -> set_bytes_b(0x3a, s2)
      "bp" -> set_bytes_b(0x33, s2)
      "bzn" -> set_bytes_b(0x3b, s2)
      "bni" -> set_bytes_b(0x34, s2)
      "bno" -> set_bytes_b(0x3c, s2)
      "bnc" -> set_bytes_b(0x35, s2)
      "bc" -> set_bytes_b(0x3d, s2)
      "bge" -> set_bytes_b(0x36, s2)
      "blt" -> set_bytes_b(0x3e, s2)
      "bgt" -> set_bytes_b(0x37, s2)
      "ble" -> set_bytes_b(0x3f, s2)
      // OP_RR, OP_RImm, OP_RDis, OP_RDir, OP_RDis2, OP_RDir2
      "ld" -> set_bytes_a(s2, s3, 0x60, 0x61, 0x68, 0x69, inst_type,
                          0x62, 0x64, 0x66, 0x6a, 0x6c, 0x6e, 0x65, 0x67, 0x6d, 0x6f)
      "st" -> set_bytes_a(s2, s3, 0x70, 0x71, 0x78, 0x79, inst_type,
                          0x72, 0x74, 0x76, 0x7a, 0x7c, 0x7e, 0x75, 0x77, 0x7d, 0x7f)
      "sbc" -> set_bytes_a(s2, s3, 0x80, 0x81, 0x88, 0x89, inst_type,
                          0x82, 0x84, 0x86, 0x8a, 0x8c, 0x8e, 0x85, 0x87, 0x8d, 0x8f)
      "adc" -> set_bytes_a(s2, s3, 0x90, 0x91, 0x98, 0x99, inst_type,
                          0x92, 0x94, 0x96, 0x9a, 0x9c, 0x9e, 0x95, 0x97, 0x9d, 0x9f)
      "sub" -> set_bytes_a(s2, s3, 0xa0, 0xa1, 0xa8, 0xa9, inst_type,
                          0xa2, 0xa4, 0xa6, 0xaa, 0xac, 0xae, 0xa5, 0xa7, 0xad, 0xaf)
      "add" -> set_bytes_a(s2, s3, 0xb0, 0xb1, 0xb8, 0xb9, inst_type,
                          0xb2, 0xb4, 0xb6, 0xba, 0xbc, 0xbe, 0xb5, 0xb7, 0xbd, 0xbf)
      "eor" -> set_bytes_a(s2, s3, 0xc0, 0xc1, 0xc8, 0xc9, inst_type,
                          0xc2, 0xc4, 0xc6, 0xca, 0xcc, 0xce, 0xc5, 0xc7, 0xcd, 0xcf)
      "or" -> set_bytes_a(s2, s3, 0xd0, 0xd1, 0xd8, 0xd9, inst_type,
                          0xd2, 0xd4, 0xd6, 0xda, 0xdc, 0xde, 0xd5, 0xd7, 0xdd, 0xdf)
      "and" -> set_bytes_a(s2, s3, 0xe0, 0xe1, 0xe8, 0xe9, inst_type,
                          0xe2, 0xe4, 0xe6, 0xea, 0xec, 0xee, 0xe5, 0xe7, 0xed, 0xef)
      "cmp" -> set_bytes_a(s2, s3, 0xf0, 0xf1, 0xf8, 0xf9, inst_type,
                          0xf2, 0xf4, 0xf6, 0xfa, 0xfc, 0xfe, 0xf5, 0xf7, 0xfd, 0xff)
      "equ" -> {} // pseudo operator
      "prog" -> mem.add(labels.evalExprWithCA(s2, mem.size)) // pseudo operator
      "data" -> mem.add(labels.evalExprWithCA(s2, mem.size)) // pseudo operator
      "end" -> {} // pseudo operator
      "*lbl*" -> {}
      else -> throw AsmError("(in arrangeByteSeq) "
                               + s1 + ": no such operator")
    }
  }
  return mem
}


fun printList(mem: List<Int>, arg: Pair<IMList, LabelList>) {
  val parsedList = arg.first
  val labels = arg.second
  var addr = 0
  val fmt_1B     = " %02X :\t%02X\t\t%S\t%S\t%S\n"
  val fmt_1Bd    = "1%02X :\t%02X\t\t%S\t%S\t%S\n"
  val fmt_1Bc    = " %02X :\t%02X\t\t%S\t%S\t%S,\t%S\n"
  val fmt_2B     = " %02X :\t%02X %02X\t\t%S\t%S\t%S\n"
  val fmt_2Bc    = " %02X :\t%02X %02X\t\t%S\t%S\t%S,\t%S\n"
  val fmt_2Bc2   = " %02X :\t%02X %02X\t\t%S\t%S\t%S,\t[%S]\n"
  val fmt_2Bc3   = " %02X :\t%02X %02X\t\t%S\t%S\t%S,\t[IX%S]\n"
  val fmt_2Bc22  = " %02X :\t%02X %02X\t\t%S\t%S\t%S,\t(%S)\n"
  val fmt_2Bc32  = " %02X :\t%02X %02X\t\t%S\t%S\t%S,\t(IX%S)\n"
  val fmt_pseudo = "\t\t\t%S\t%S\t%S\t%S\n"
  val fmt_equ     = " %02X :\t\t\t%S\t%S\t%S\t%S\n"
  val fmt_lbl    = "\t\t\t%S\n"
  parsedList.forEach {
    val tri = it.first
    val inst_type = it.second
    val op = tri.first
    val r1 = tri.second
    val r2 = tri.third
    val lbl = it.third
    when (inst_type) {
      instT.EQU -> {
        print(fmt_equ.format(labels.evalExprWithCA(lbl, 0), lbl, op, r1, r2))
      }
      instT.PROG -> {
        if (op == "prog") print(fmt_1B.format(addr, mem[addr++], lbl, op, r1))
        else print(fmt_1Bd.format(addr, mem[addr++], lbl, op, r1)) // DATA
      }
      instT.OP -> {
        if (op == "end") {
          print(fmt_pseudo.format(lbl, op, r1, r2))
        } else {
          print(fmt_1B.format(addr, mem[addr++], lbl, op, r1))
        }
      }
      instT.OP_R -> print(fmt_1B.format(addr, mem[addr++], lbl, op, r1))
      instT.OP_RR -> print(fmt_1Bc.format(addr, mem[addr++], lbl, op, r1, r2))
      instT.OP_RImm -> 
        print(fmt_2Bc.format(addr, mem[addr++], mem[addr++], lbl, op, r1, r2))
      instT.OP_RDir -> 
        print(fmt_2Bc2.format(addr, mem[addr++], mem[addr++], lbl, op, r1, r2))
      instT.OP_RDis -> 
        print(fmt_2Bc3.format(addr, mem[addr++], mem[addr++], lbl, op, r1, r2))
      instT.OP_RDir2 -> 
        print(fmt_2Bc22.format(addr, mem[addr++], mem[addr++], lbl, op, r1, r2))
      instT.OP_RDis2 -> 
        print(fmt_2Bc32.format(addr, mem[addr++], mem[addr++], lbl, op, r1, r2))
      instT.OP_Imm -> 
        print(fmt_2B.format(addr, mem[addr++], mem[addr++], lbl, op, r1))
      instT.LBL -> print(fmt_lbl.format(lbl))
    }
  }
}


fun printHex(mem: List<Int>) {
  mem.forEach {print("%02X\n".format(it))}
}


fun assemble(lineList: MutableList<String>, otype: String) {
  val pair = parseInputList(lineList)
  val mem = arrangeByteSeq(pair)
  when (otype) {
    "hex" -> printHex(mem)
    else -> printList(mem, pair)
  }
}


fun main(args: Array<String>) {
  if (args.size == 0) {
    println("Usage: kas filename")
    println("Usage: kas filename hex")
    return
  }
  try {
    val inputStream: InputStream = File(args[0]).inputStream()
    val lineList = mutableListOf<String>()
    inputStream.bufferedReader().useLines { 
      lines -> lines.forEach {lineList.add(it)} 
    }
    if (args.size >= 2 && args[1] == "hex") {
      assemble(lineList, "hex")
    } else {
      assemble(lineList, "")
    }
  } catch (e: java.io.FileNotFoundException) {
    println(args[0] + ": no such file")
  } catch (e: AsmError) {
    println(e)
  }
}
