# KUE-CHIP2 Tools

KUE-CHIP2 is a tiny 8-bit processor [1][2].
Here we present a set of tools for helping beginners, e.g., high school students, get a feel of the KUE-CHIP2 CPU. We are planning to prepare an instruction set-level simulator (interpreter) and an assembler. All tools are written in Kotlin, so that you can execute those tools on any environment with the Java Rutime Environment.

[1] H. Kanbara and H. Yasuura, KUE-CHIP2: A microprocessor for education of LSI design and computer hardware, Proc. Synthesis and System Integration of Mixed Technologies (SASIMI'95) pp.233-240, 1995.
[2] H. Kanbara, KUE-CHIP: A Microprocessor for education of Computer Architecture and LSI design, Proc. IEEE ASIC Seminar and Exhibit, 1990.

## KUE-CHIP2 Assembler

   ```
$ cat mul.asm
DATA1:  EQU     80H
DATA2:  EQU     81H
ANS:    EQU     90H
        LD      IX,     [DATA2]
        EOR     ACC,    ACC
LABEL:  ADD     ACC,    [DATA1]
        SUB     IX,     1
        BNZ     LABEL
        ST      ACC,    [ANS]
        HLT
        END
$ ./kas mul.asm
 80 :                   DATA1:  EQU     80H
 81 :                   DATA2:  EQU     81H
 90 :                   ANS:    EQU     90H
 00 :   6C 81                   LD      IX,     [DATA2]
 02 :   C0                      EOR     ACC,    ACC
 03 :   B4 80           LABEL:  ADD     ACC,    [DATA1]
 05 :   AA 01                   SUB     IX,     1
 07 :   31 03                   BNZ     LABEL
 09 :   74 90                   ST      ACC,    [ANS]
 0B :   0F                      HLT
                                END
$
   ```


## KUE-CHIP2 Simulator

   ```
$ ./kas mul.asm hex > mul.hex
$ ./ksim
Starting KueChip2 Simulator ... OK.
> mem
 00 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 10 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 20 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 30 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 40 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 50 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 60 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 70 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 80 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 90 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 A0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 B0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 C0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 D0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 E0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 F0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

> reg
ACC=00(0), IX=00(0), CF=0, VF=0, NF=0, ZF=0
PC=00(0), MAR=00(0), IR=00(0), DR=00(0)

> lf mul.hex
OK.
> set 3, [80H]
> set 4, [81H]
> rst
> ss
................
> all
ACC=0C(12), IX=00(0), CF=0, VF=0, NF=0, ZF=1
PC=0C(12), MAR=00(0), IR=0F(15), DR=90(-112/144)
 00 :  6C 81 C0 B4 80 AA 01 31 03 74 90 0F 00 00 00 00
 10 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 20 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 30 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 40 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 50 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 60 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 70 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 80 :  03 04 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 90 :  0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 A0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 B0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 C0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 D0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 E0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 F0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
100 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
110 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
120 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
130 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
140 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
150 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
160 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
170 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
180 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
190 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
1A0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
1B0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
1C0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
1D0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
1E0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
1F0 :  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

> bye
Bye.
$ 
   ```

## How to build
Type `make`. You need a Kotlin compiler to build the tools. Once you build the tools, you can run them on any system with Java Runtime Environment.
