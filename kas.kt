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
  OP_RDis0,
  OP_RDis,
  BR
}

class AsmError(override var message:String): Exception(message)

typealias IMElem = Triple<Triple<String, String, String>, instT, String>
typealias IMList = MutableList<IMElem>
typealias LabelList = MutableList<Pair<String, Int>>


fun parseInputList(lineList: List<String>): Pair<IMList, LabelList> {
  val spc = "[ \t]*"
  val op_pat = "([a-z][a-z][a-z]?)"
  val ident = "([a-z][a-z0-9]*)"
  val num = "([0-9][0-9a-f]*h?)"
  val mode_reg = "(acc|ix)"
  val mode_imm = "((\\+|\\-)?(" + num + "|" + ident + "))"
  val mode_dir = "\\[ *(" + num + "|" + ident + ") *\\]"
  val mode_dis0 = "\\[ *ix *\\]"
  val mode_dis = "\\[ *ix *(\\+|\\-) *(" + num + "|" + ident + ") *\\]"
  val sep = spc + ",?" + spc
  val inst_op = spc + op_pat + spc
  val inst_r = spc + op_pat + spc + mode_reg + spc
  val inst_rr = spc + op_pat + spc + mode_reg + sep + mode_reg + spc
  val inst_rimm = spc + op_pat + spc + mode_reg + sep + mode_imm + spc
  val inst_rdir = spc + op_pat + spc + mode_reg + sep + mode_dir + spc
  val inst_rdis0 = spc + op_pat + spc + mode_reg + sep + mode_dis0 + spc
  val inst_rdis = spc + op_pat + spc + mode_reg + sep + mode_dis + spc
  val inst_b = spc + op_pat + spc + ident + spc
  val lab_prefix = spc + "(" + ident + "?[ \t]*:)?"
  val regex_op = (lab_prefix + inst_op).toRegex() // L: SCF
  val regex_r = (lab_prefix + inst_r).toRegex() // L: SRA ACC
  val regex_rr = (lab_prefix + inst_rr).toRegex() // L: ADD ACC, IX
  val regex_rimm = (lab_prefix + inst_rimm).toRegex() // L: ADD ACC, 3
  val regex_rdir = (lab_prefix + inst_rdir).toRegex() // L: ADD ACC, [3]
  val regex_rdis0 = (lab_prefix + inst_rdis0).toRegex() // L: ADD ACC, [IX]
  val regex_rdis = (lab_prefix + inst_rdis).toRegex() // L: ADD ACC, [IX+3]
  val regex_b = (lab_prefix + inst_b).toRegex() // L: BNE L2
  val regex_equ = (spc + ident + spc + ":" + spc 
      + "(equ)" + spc + num + spc).toRegex() // L: EQU 80H

  var labels = mutableListOf<Pair<String,Int>>()
  var parsedList = mutableListOf<IMElem>()
  var byte_count = 0

  // used only for EQN
  fun parseNumber0(s: String): Int {
    val ca = s.toCharArray()
    if (!s[0].isDigit()) {
      throw AsmError("(in parseNumber0)" + s + ": not a number")
    } else if (s[ca.size-1] == 'h') {
      var s2 = s.trim({ch -> ch == 'h'})
      return (s2.toLong(radix = 16).toInt()) // 0AH
    } else {
      return s.toInt() // 3
    }
  }

  lineList.forEach {
    val line = it.toLowerCase()

    if (regex_op.matches(line)) { // L: SCF
      val (lblc, lbl, p1) = regex_op.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, "", ""), instT.OP, lblc))
      byte_count++

    } else if (regex_r.matches(line)) { // L: SRA ACC
      val (lblc, lbl, p1, p2) = regex_r.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, p2, ""), instT.OP_R, lblc))
      byte_count++

    } else if (regex_rr.matches(line)) { // L: ADD ACC, IX
      val (lblc, lbl, p1, p2, p3) = regex_rr.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, p2, p3), instT.OP_RR, lblc))
      byte_count++

    } else if (regex_rimm.matches(line)) { // L: ADD ACC, 3
      val (lblc, lbl, p1, p2, p3) = regex_rimm.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, p2, p3), instT.OP_RImm, lblc))
      byte_count += 2

    } else if (regex_rdir.matches(line)) { // L: ADD ACC, [3]
      val (lblc, lbl, p1, p2, p3) = regex_rdir.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, p2, p3), instT.OP_RDir, lblc))
      byte_count += 2

    } else if (regex_rdis0.matches(line)) { // L: ADD ACC, [IX]
      val (lblc, lbl, p1, p2) = regex_rdis0.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, p2, "0"), instT.OP_RDis0, lblc))
      byte_count += 2

    } else if (regex_rdis.matches(line)) { // L: ADD ACC, [IX+3]
      val (lblc, lbl, p1, p2, p3) = regex_rdis.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, p2, p3), instT.OP_RDis, lblc))
      byte_count += 2

    } else if (regex_b.matches(line)) { // L: BNE L2
      val (lblc, lbl, p1, p2) = regex_b.find(line)!!.destructured
      labels.add(Pair(lbl, byte_count))
      parsedList.add(Triple(Triple(p1, p2, ""), instT.BR, lblc))
      byte_count += 2

    } else if (regex_equ.matches(line)) { // L: EQU 80H
      val (lbl, p1, p2) = regex_equ.find(line)!!.destructured
      labels.add(Pair(lbl, parseNumber0(p2)))
      parsedList.add(Triple(Triple(p1, p2, ""), instT.EQU, lbl+":"))

    } else if (spc.toRegex().matches(line)) { // empty string
      // do nothing
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

  fun getAddr(lbl: String): Int { // label -> addr
    labels.forEach {if (it.first == lbl) return it.second}
    throw AsmError("(in getAddr)" + lbl + ": does not exist")
  }

  fun parseNumber(s: String): Int { // {ID, 0AH, 3} -> Int
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

  fun set_bytes_s(a1: Int, a2: Int, s2: String) {
    if (s2=="acc") mem.add(a1) else mem.add(a2)
  }
  fun set_bytes_b(op: Int, s2: String) {mem.add(op); mem.add(getAddr(s2))}
  fun set_bytes_a(s2: String, s3: String, aa: Int, ai: Int, ia: Int, ii: Int,
                 it: instT, p: Int, q: Int, r: Int, s: Int, t: Int, u: Int) {
    fun set_bytes0(it: instT, addr: Int, n1: Int, n2: Int, n3: Int, n4: Int) {
      fun f(n: Int) {mem.add(n); mem.add(addr)}
      when (it) {
	instT.OP_RImm -> f(n1)
	instT.OP_RDir -> f(n2)
	instT.OP_RDis0 -> f(n3)
	instT.OP_RDis -> f(n4)
	else -> throw AsmError("(in set_bytes0)" + it + ": illegal inst type")
      }
    }
    when (s2) {
      "acc" -> when (s3) {
	"acc" -> mem.add(aa) // EOR ACC, ACC
	"ix" -> mem.add(ai) // EOR ACC, IX
	else -> set_bytes0(it, parseNumber(s3), p, q, r, r)
      }
      "ix" -> when (s3) {
	"acc" -> mem.add(ia) // EOR IX, ACC
	"ix" -> mem.add(ii) // EOR IX, IX
	else -> set_bytes0(it, parseNumber(s3), s, t, u, u)
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
      "ba" -> set_bytes_b(0x30, s2) // BR
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
      // OP_RR, OP_RImm, OP_RDis0, OP_RDis, OP_RDir
      "ld" -> set_bytes_a(s2, s3, 0x60, 0x61, 0x68, 0x69, inst_type,
                          0x62, 0x64, 0x66, 0x6a, 0x6c, 0x6e)
      "st" -> set_bytes_a(s2, s3, 0x70, 0x71, 0x78, 0x79, inst_type,
                          0x72, 0x74, 0x76, 0x7a, 0x7c, 0x7e)
      "sbc" -> set_bytes_a(s2, s3, 0x80, 0x81, 0x88, 0x89, inst_type,
                          0x82, 0x84, 0x86, 0x8a, 0x8c, 0x6e)
      "adc" -> set_bytes_a(s2, s3, 0x90, 0x91, 0x98, 0x99, inst_type,
                          0x92, 0x94, 0x96, 0x9a, 0x9c, 0x9e)
      "sub" -> set_bytes_a(s2, s3, 0xa0, 0xa1, 0xa8, 0xa9, inst_type,
                          0xa2, 0xa4, 0xa6, 0xaa, 0xac, 0xae)
      "add" -> set_bytes_a(s2, s3, 0xb0, 0xb1, 0xb8, 0xb9, inst_type,
                          0xb2, 0xb4, 0xb6, 0xba, 0xbc, 0xbe)
      "eor" -> set_bytes_a(s2, s3, 0xc0, 0xc1, 0xc8, 0xc9, inst_type,
                          0xc2, 0xc4, 0xc6, 0xca, 0xcc, 0xce)
      "or" -> set_bytes_a(s2, s3, 0xd0, 0xd1, 0xd8, 0xd9, inst_type,
                          0xd2, 0xd4, 0xd6, 0xda, 0xdc, 0xde)
      "and" -> set_bytes_a(s2, s3, 0xe0, 0xe1, 0xe8, 0xe9, inst_type,
                          0xe2, 0xe4, 0xe6, 0xea, 0xec, 0xee)
      "cmp" -> set_bytes_a(s2, s3, 0xf0, 0xf1, 0xf8, 0xf9, inst_type,
                          0xf2, 0xf4, 0xf6, 0xfa, 0xfc, 0xfe)
      "equ" -> {} // pseudo operator
      "end" -> {} // pseudo operator
      else -> throw AsmError("(in arrangeByteSeq) "
                               + s1 + ": no such operator")
    }
  }
  return mem
}


fun printList(mem: List<Int>, parsedList: IMList) {
  var addr = 0
  val fmt_1B = " %02X :\t%02X\t\t%s\t%s\t%s\n"
  val fmt_1Bc = " %02X :\t%02X\t\t%s\t%s\t%s,\t%s\n"
  val fmt_2B = " %02X :\t%02X %02X\t\t%s\t%s\t%s\n"
  val fmt_2Bc = " %02X :\t%02X %02X\t\t%s\t%s\t%s,\t%s\n"
  val fmt_2Bc2 = " %02X :\t%02X %02X\t\t%s\t%s\t%s,\t[%s]\n"
  val fmt_pseudo = "\t\t\t%s\t%s\t%s\t%s\n"
  parsedList.forEach {
    val tri = it.first
    val inst_type = it.second
    val s1 = tri.first.toUpperCase()
    val s2 = tri.second.toUpperCase()
    val s3 = tri.third.toUpperCase()
    val lbl = it.third.toUpperCase()
    when (inst_type) {
      instT.EQU -> print(fmt_pseudo.format(lbl, s1, s2, s3))
      instT.OP -> {
	if (addr >= mem.size) { // pseudo operator (END)
	  print(fmt_pseudo.format(lbl, s1, s2, s3))
	} else {
	  print(fmt_1B.format(addr, mem[addr++], lbl, s1, s2))
	}
      }
      instT.OP_R -> print(fmt_1B.format(addr, mem[addr++], lbl, s1, s2))
      instT.OP_RR -> print(fmt_1Bc.format(addr, mem[addr++], lbl, s1, s2, s3))
      instT.OP_RImm -> 
        print(fmt_2Bc.format(addr, mem[addr++], mem[addr++], lbl, s1, s2, s3))
      instT.OP_RDir -> 
        print(fmt_2Bc2.format(addr, mem[addr++], mem[addr++], lbl, s1, s2, s3))
      instT.OP_RDis0 -> 
        print(fmt_2Bc2.format(addr, mem[addr++], mem[addr++], lbl, s1, s2, s3))
      instT.OP_RDis -> 
        print(fmt_2Bc2.format(addr, mem[addr++], mem[addr++], lbl, s1, s2, s3))
      instT.BR -> 
        print(fmt_2B.format(addr, mem[addr++], mem[addr++], lbl, s1, s2))
    }
  }
}


fun assemble(lineList: MutableList<String>) {
  val pair = parseInputList(lineList)
  val mem = arrangeByteSeq(pair)
  printList(mem, pair.first)
}


fun main(args: Array<String>) {
  if (args.size == 0) {
    println("Usage: kas filename")
    return
  }

  try {
    val inputStream: InputStream = File(args[0]).inputStream()
//  val inputString = inputStream.bufferedReader().use { it.readText() }

    val lineList = mutableListOf<String>()
    inputStream.bufferedReader().useLines { 
      lines -> lines.forEach {lineList.add(it)} 
    }
    assemble(lineList)
  } catch (e: java.io.FileNotFoundException) {
    println(args[0] + ": no such file")
  } catch (e: AsmError) {
    println(e)
  }
}
